package com.clhs.score.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clhs.score.data.AppSettings

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationPromptDialog(
    settings: AppSettings,
    onEnableNotifications: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEnableNotifications by rememberUpdatedState(onEnableNotifications)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val needsPermission = !context.arePostNotificationsGranted()

    if (!needsPermission || settings.notificationPromptDismissed) {
        return
    }

    var showDialog by rememberSaveable { mutableStateOf(true) }
    if (!showDialog) return
    var awaitingNotificationSettings by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingNotificationSettings) {
                awaitingNotificationSettings = false
                if (context.arePostNotificationsGranted()) {
                    currentOnEnableNotifications(true)
                    showDialog = false
                    Toast.makeText(context, "已開啟推播通知", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未取得通知權限，可在設定中手動開啟", Toast.LENGTH_SHORT).show()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("開啟推播通知") },
        text = {
            Column {
                Text("開啟通知以接收 app 更新與重要公告，不錯過最新消息。")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "不再提醒",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        showDialog = false
                        currentOnDismiss()
                        Toast.makeText(context, "日後可在設定中隨時開啟通知", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    awaitingNotificationSettings = true
                    if (!context.openAppNotificationSettings()) {
                        awaitingNotificationSettings = false
                        Toast.makeText(context, "無法開啟通知設定，請手動到系統設定開啟", Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text("開啟")
            }
        },
    )
}
