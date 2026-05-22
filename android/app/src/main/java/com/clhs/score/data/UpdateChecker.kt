package com.clhs.score.data

import kotlinx.coroutines.Dispatchers
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

class UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateResult.Error("HTTP ${response.code}")
            }
            val body = response.body.string()
            val json = SchoolJson.parseToJsonElement(body).jsonObject
            val tagName = json["tag_name"]?.jsonPrimitive?.content.orEmpty()
            val remoteVersion = tagName.removePrefix("v")
            val htmlUrl = json["html_url"]?.jsonPrimitive?.content.orEmpty()
            val releaseBody = json["body"]?.jsonPrimitive?.content.orEmpty()
            val apkUrl = json["assets"]?.jsonArray
                ?.firstOrNull { asset ->
                    asset.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true
                }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content

            if (isNewer(remoteVersion, currentVersionName)) {
                UpdateResult.NewVersion(
                    versionName = remoteVersion,
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = apkUrl,
                    releaseNotes = releaseBody,
                )
            } else {
                UpdateResult.UpToDate
            }
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

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/alvin000009238/clhs_score/releases/latest"
    }
}
