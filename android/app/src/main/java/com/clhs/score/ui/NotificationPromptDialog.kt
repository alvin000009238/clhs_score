package com.clhs.score.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clhs.score.data.AppSettings
import com.clhs.score.notifications.canPostNotifications
import com.clhs.score.notifications.hasPostNotificationsPermission
import com.clhs.score.notifications.openAppNotificationSettings
import com.clhs.score.notifications.shouldShowPostNotificationsRationale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationPromptDialog(
    settings: AppSettings,
    onEnableNotifications: (Boolean) -> Unit,
    onOpenSettings: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEnableNotifications by rememberUpdatedState(onEnableNotifications)
    val currentOnOpenSettings by rememberUpdatedState(onOpenSettings)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    var showDialog by rememberSaveable { mutableStateOf(true) }
    var awaitingNotificationSettings by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            currentOnEnableNotifications(true)
            showDialog = false
            Toast.makeText(context, "已開啟推播通知", Toast.LENGTH_SHORT).show()
        } else {
            permissionDenied = true
            Toast.makeText(context, "未取得通知權限，可再次嘗試開啟", Toast.LENGTH_SHORT).show()
        }
    }

    if (context.canPostNotifications() || settings.notificationPromptDismissed || !showDialog) {
        return
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingNotificationSettings) {
                awaitingNotificationSettings = false
                if (context.canPostNotifications()) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (
                        !context.hasPostNotificationsPermission() &&
                        (!permissionDenied || context.shouldShowPostNotificationsRationale())
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        currentOnOpenSettings()
                        awaitingNotificationSettings = true
                        if (!context.openAppNotificationSettings()) {
                            awaitingNotificationSettings = false
                            Toast.makeText(context, "無法開啟通知設定，請手動到系統設定開啟", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("開啟")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    showDialog = false
                    currentOnDismiss()
                    Toast.makeText(context, "日後可在設定中隨時開啟通知", Toast.LENGTH_SHORT).show()
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("不再提醒")
            }
        },
    )
}
