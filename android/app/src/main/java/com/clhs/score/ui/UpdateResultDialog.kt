package com.clhs.score.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.clhs.score.data.ApkAsset
import com.clhs.score.data.UpdateResult
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateResultDialog(
    result: UpdateResult?,
    onDismiss: () -> Unit,
) {
    if (result == null) return
    val context = LocalContext.current
    when (result) {
        is UpdateResult.UpToDate -> {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
        is UpdateResult.Error -> {
            LaunchedEffect(result) {
                Toast.makeText(context, "檢查更新失敗：${result.message}", Toast.LENGTH_LONG).show()
                onDismiss()
            }
        }
        is UpdateResult.NewVersion -> {
            var isInstalling by remember(result.apkAsset) { mutableStateOf(false) }
            var downloadProgress by remember(result.apkAsset) { mutableStateOf<Float?>(null) }
            val scope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = { if (!isInstalling) onDismiss() },
                title = { Text("有新版本") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("v${result.versionName} 已可更新")
                        if (isInstalling) {
                            val progress = downloadProgress
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (progress == null) {
                                    CircularWavyProgressIndicator(modifier = Modifier.size(28.dp))
                                } else {
                                    CircularWavyProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Text(
                                    text = progress?.let { "下載中 ${(it * 100).toInt()}%" } ?: "下載中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        if (result.releaseNotes.isNotBlank()) {
                            Markdown(
                                content = result.releaseNotes,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isInstalling,
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            val apkAsset = result.apkAsset
                            if (apkAsset == null) {
                                openUrl(context, result.htmlUrl)
                                onDismiss()
                                return@TextButton
                            }
                            scope.launch {
                                isInstalling = true
                                downloadProgress = 0f
                                try {
                                    val apk = downloadUpdateApk(context.applicationContext, apkAsset) {
                                        downloadProgress = it
                                    }
                                    openApkInstaller(context, apk)
                                    onDismiss()
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: UpdateApkTooLargeException) {
                                    Toast.makeText(
                                        context,
                                        "更新檔過大，請前往 GitHub 下載",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } catch (_: ChecksumMismatchException) {
                                    Toast.makeText(
                                        context,
                                        "更新檔驗證失敗，請重新下載",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "下載或安裝失敗", Toast.LENGTH_LONG).show()
                                } finally {
                                    isInstalling = false
                                    downloadProgress = null
                                }
                            }
                        },
                    ) {
                        Text(
                            when {
                                isInstalling -> "下載中..."
                                result.apkAsset != null -> "安裝 APK"
                                else -> "前往 GitHub"
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isInstalling,
                        shapes = ButtonDefaults.shapes(),
                        onClick = onDismiss,
                    ) { Text("稍後") }
                },
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val uri = url.toUri()
        if (uri.scheme !in listOf("http", "https")) error("unsupported update URL")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        Toast.makeText(context, "無法開啟連結", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun downloadUpdateApk(
    context: Context,
    asset: ApkAsset,
    onProgress: suspend (Float?) -> Unit,
): File =
    withContext(Dispatchers.IO) {
        suspend fun reportProgress(value: Float?) {
            withContext(Dispatchers.Main.immediate) {
                onProgress(value)
            }
        }

        val dir = File(context.cacheDir, "updates")
        dir.mkdirs()
        val apk = File(dir, "clhs-score-update.apk")
        apk.delete()

        val request = Request.Builder().url(asset.downloadUrl).get().build()
        try {
            updateDownloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val totalBytes = response.body.contentLength()
                if (totalBytes > MAX_UPDATE_APK_BYTES) throw UpdateApkTooLargeException()
                if (totalBytes <= 0) reportProgress(null)
                var lastPercent = -1
                response.body.byteStream().use { input ->
                    writeVerifiedApk(input, apk, asset.sha256) { copiedBytes ->
                        if (totalBytes > 0) {
                            val percent = ((copiedBytes * 100) / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                reportProgress(percent.coerceIn(0, 100) / 100f)
                            }
                        }
                    }
                }
                if (totalBytes > 0) reportProgress(1f)
            }
        } catch (error: Exception) {
            apk.delete()
            throw error
        }
        apk
    }

internal suspend fun writeVerifiedApk(
    input: InputStream,
    destination: File,
    expectedSha256: String,
    maxBytes: Long = MAX_UPDATE_APK_BYTES,
    onBytesCopied: suspend (Long) -> Unit = {},
): File {
    require(maxBytes > 0) { "maxBytes must be positive" }
    val digest = MessageDigest.getInstance("SHA-256")
    try {
        destination.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copiedBytes = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read.toLong() > maxBytes - copiedBytes) throw UpdateApkTooLargeException()
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                copiedBytes += read
                onBytesCopied(copiedBytes)
            }
        }
        val actualSha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        if (actualSha256 != expectedSha256) throw ChecksumMismatchException()
        return destination
    } catch (error: Exception) {
        destination.delete()
        throw error
    }
}

internal class ChecksumMismatchException : Exception()
internal class UpdateApkTooLargeException : Exception()

internal const val MAX_UPDATE_APK_BYTES = 256L * 1024 * 1024

private fun openApkInstaller(context: Context, apk: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apk,
    )
    context.startActivity(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )
}

private val updateDownloadClient = OkHttpClient()
