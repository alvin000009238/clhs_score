package com.clhs.score.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class UpdateResult {
    data object UpToDate : UpdateResult()
    data class NewVersion(
        val versionName: String,
        val htmlUrl: String,
        val apkDownloadUrl: String?,
        val releaseNotes: String,
    ) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class UpdateChecker(
    private val client: OkHttpClient = defaultClient,
    private val latestReleaseUrl: String = LATEST_RELEASE_URL,
) {

    suspend fun check(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(latestReleaseUrl)
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateResult.Error("HTTP ${response.code}")
                }
                val body = response.body.string()
                val json = SchoolJson.parseToJsonElement(body).jsonObject
                val tagName = json["tag_name"]?.jsonPrimitive?.content.orEmpty()
                val remoteVersion = tagName.removePrefix("v")
                val htmlUrl = json["html_url"]?.jsonPrimitive?.content.orEmpty().takeIfValidHttpUrl()
                val releaseBody = json["body"]?.jsonPrimitive?.content.orEmpty()
                val apkUrl = json["assets"]?.jsonArray
                    ?.firstOrNull { asset ->
                        asset.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true
                    }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content
                    ?.takeIfValidHttpUrl()

                if (!isNewer(remoteVersion, currentVersionName)) {
                    return@withContext UpdateResult.UpToDate
                }
                if (htmlUrl == null && apkUrl == null) {
                    return@withContext UpdateResult.Error("更新連結格式不正確")
                }
                UpdateResult.NewVersion(
                    versionName = remoteVersion,
                    htmlUrl = htmlUrl.orEmpty(),
                    apkDownloadUrl = apkUrl,
                    releaseNotes = releaseBody,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "未知錯誤")
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun String.takeIfValidHttpUrl(): String? =
        takeIf { value ->
            value.startsWith("https://", ignoreCase = true) ||
                value.startsWith("http://", ignoreCase = true)
        }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/alvin000009238/clhs_score/releases/latest"

        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
