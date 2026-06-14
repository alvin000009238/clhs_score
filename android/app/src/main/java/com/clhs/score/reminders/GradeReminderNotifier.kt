package com.clhs.score.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.clhs.score.MainActivity
import com.clhs.score.R
import com.clhs.score.data.GradeChangeSet
import com.clhs.score.data.GradeReminderText
import com.clhs.score.notifications.NotificationChannels
import java.util.concurrent.atomic.AtomicInteger

class GradeReminderNotifier(private val context: Context) {
    private val appContext = context.applicationContext

    fun showChangedNotification(changeSet: GradeChangeSet) {
        if (!canPostNotifications()) return
        NotificationChannels.ensureCreated(appContext)

        val notification = Notification.Builder(appContext, NotificationChannels.GRADE_REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(GradeReminderText.notificationTitle(changeSet))
            .setContentText(GradeReminderText.notificationBody(changeSet))
            .setStyle(Notification.BigTextStyle().bigText(GradeReminderText.notificationBody(changeSet)))
            .setContentIntent(openReminderIntent(changeSet.yearValue, changeSet.examValue))
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        appContext.getSystemService(NotificationManager::class.java)
            .notify(nextNotificationId(), notification)
    }

    fun showStoppedNotification(reason: String) {
        if (!canPostNotifications()) return
        NotificationChannels.ensureCreated(appContext)

        val notification = Notification.Builder(appContext, NotificationChannels.GRADE_REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("段考提醒已停止")
            .setContentText(reason)
            .setStyle(Notification.BigTextStyle().bigText(reason))
            .setContentIntent(openAppIntent())
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        appContext.getSystemService(NotificationManager::class.java)
            .notify(nextNotificationId(), notification)
    }

    private fun openReminderIntent(yearValue: String, examValue: String): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_GRADE_REMINDER, true)
            putExtra(EXTRA_YEAR_VALUE, yearValue)
            putExtra(EXTRA_EXAM_VALUE, examValue)
        }
        return PendingIntent.getActivity(
            appContext,
            nextRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(): PendingIntent {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(appContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            appContext,
            nextRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_OPEN_GRADE_REMINDER = "open_grade_reminder"
        const val EXTRA_YEAR_VALUE = "grade_reminder_year_value"
        const val EXTRA_EXAM_VALUE = "grade_reminder_exam_value"
        private val idCounter = AtomicInteger(3000)

        private fun nextNotificationId(): Int = idCounter.getAndIncrement()

        private fun nextRequestCode(): Int = idCounter.getAndIncrement()
    }
}
