package com.clhs.score.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.clhs.score.R

object NotificationChannels {
    const val UPDATES_CHANNEL_ID = "score_updates"

    fun ensureCreated(context: Context) {
        val channel = NotificationChannel(
            UPDATES_CHANNEL_ID,
            context.getString(R.string.notification_channel_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_updates_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
