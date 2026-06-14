package com.clhs.score.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.clhs.score.data.UpdateResult

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
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("有新版本") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("v${result.versionName} 已可更新")
                        if (result.releaseNotes.isNotBlank()) {
                            Text(
                                text = result.releaseNotes.take(300),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val url = result.apkDownloadUrl ?: result.htmlUrl
                        try {
                            val uri = url.toUri()
                            if (uri.scheme !in listOf("http", "https")) {
                                error("unsupported update URL")
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {
                            Toast.makeText(context, "無法開啟連結", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }) {
                        Text(if (result.apkDownloadUrl != null) "下載 APK" else "前往 GitHub")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("稍後") }
                },
            )
        }
    }
}
