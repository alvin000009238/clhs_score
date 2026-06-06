package com.clhs.score.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.clhs.score.data.AppSettings

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationPromptDialog(
    settings: AppSettings,
    onEnableNotifications: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val needsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED

    if (!needsPermission || settings.notificationPromptDismissed) {
        return
    }

    var showDialog by rememberSaveable { mutableStateOf(true) }
    if (!showDialog) return

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onEnableNotifications(true)
            Toast.makeText(context, "已開啟推播通知", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "未取得通知權限，可在設定中手動開啟", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("開啟推播通知") },
        text = {
            Column {
                Text("開啟通知以接收 app 更新與重要公告，不錯過最新消息。")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "不再提醒（拜託開啟通知> <）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        onDismiss()
                        Toast.makeText(context, "日後可在設定中隨時開啟通知", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) {
                Text("開啟")
            }
        },
    )
}
