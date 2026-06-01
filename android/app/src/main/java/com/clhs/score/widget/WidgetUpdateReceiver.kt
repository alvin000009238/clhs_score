package com.clhs.score.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.clhs.score.data.PERIOD_TIMES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == ACTION_UPDATE_WIDGET) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ScheduleWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("WidgetUpdateReceiver", "Failed to update widget", e)
                } finally {
                    pendingResult.finish()
                }
            }
            scheduleNextUpdate(context)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.clhs.score.ACTION_UPDATE_WIDGET"
        
        fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTotalMinutes = currentHour * 60 + currentMinute
            
            var nextTriggerMillis: Long = -1L
            
            // Find next class boundary today
            for (period in PERIOD_TIMES) {
                val startH = period.start.substringBefore(":").toIntOrNull()
                val startM = period.start.substringAfter(":").toIntOrNull()
                val endH = period.end.substringBefore(":").toIntOrNull()
                val endM = period.end.substringAfter(":").toIntOrNull()
                
                if (startH != null && startM != null && endH != null && endM != null) {
                    val startMin = startH * 60 + startM
                    val endMin = endH * 60 + endM
                    
                    if (startMin > currentTotalMinutes) {
                        calendar.set(Calendar.HOUR_OF_DAY, startH)
                        calendar.set(Calendar.MINUTE, startM)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        nextTriggerMillis = calendar.timeInMillis
                        break
                    } else if (endMin > currentTotalMinutes) {
                        calendar.set(Calendar.HOUR_OF_DAY, endH)
                        calendar.set(Calendar.MINUTE, endM)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        nextTriggerMillis = calendar.timeInMillis
                        break
                    }
                }
            }
            
            // If no more class boundaries today, schedule for midnight tomorrow
            if (nextTriggerMillis == -1L) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                nextTriggerMillis = calendar.timeInMillis
            }
            
            // Adding a tiny buffer (e.g., 5 seconds) to avoid immediate re-triggering
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMillis + 5000,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e("WidgetUpdateReceiver", "Failed to schedule alarm", e)
            }
        }
        
        fun cancelUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}
