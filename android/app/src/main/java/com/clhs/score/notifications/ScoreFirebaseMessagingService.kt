package com.clhs.score.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.clhs.score.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

class ScoreFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        showNotification(message)
    }

    override fun onNewToken(token: String) {
        // Topic subscriptions are restored by Firebase and SettingsViewModel on app launch.
    }

    private fun showNotification(message: RemoteMessage) {
        if (!canPostNotifications()) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        val url = message.data["url"].orEmpty()
        val isUpdateTopic = message.from == "/topics/${NotificationTopicManager.APP_UPDATES_TOPIC}" ||
            message.data["check_update"] == "true" ||
            message.data["action"] == "check_update"

        NotificationChannels.ensureCreated(this)

        val notification = Notification.Builder(this, NotificationChannels.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(buildContentIntent(url, isUpdateTopic))
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationIdCounter.getAndIncrement(), notification)
    }

    private fun buildContentIntent(url: String, isUpdateTopic: Boolean): PendingIntent {
        val safeUri = if (url.isNotBlank() && !isUpdateTopic) {
            Uri.parse(url).takeIf { it.scheme in listOf("http", "https") }
        } else {
            null
        }

        val intent = if (safeUri != null) {
            Intent(Intent.ACTION_VIEW, safeUri)
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (isUpdateTopic) {
            intent.putExtra(EXTRA_CHECK_UPDATE, true)
        }

        return PendingIntent.getActivity(
            this,
            notificationIdCounter.get(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val EXTRA_CHECK_UPDATE = "check_update"
        private val notificationIdCounter = AtomicInteger(1000)
    }
}
