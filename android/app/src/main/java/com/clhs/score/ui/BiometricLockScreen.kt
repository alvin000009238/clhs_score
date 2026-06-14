package com.clhs.score.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import com.clhs.score.ui.components.PinInputDialog

@Composable
fun BiometricLockScreen(
    isBiometricInvalidated: Boolean,
    onTriggerBiometric: () -> Unit,
    onUnlockWithPin: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showInvalidatedAlert by remember { mutableStateOf(isBiometricInvalidated) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isBiometricInvalidated) {
        if (isBiometricInvalidated) {
            showInvalidatedAlert = true
        }
    }

    if (showInvalidatedAlert) {
        AlertDialog(
            onDismissRequest = { /* Force action */ },
            title = { Text("安全防護提示") },
            text = { Text("系統生物特徵已變更，請輸入備用密碼重新註冊。") },
            confirmButton = {
                TextButton(onClick = {
                    showInvalidatedAlert = false
                    showPinDialog = true
                }) {
                    Text("確定")
                }
            }
        )
    }

    if (showPinDialog) {
        PinInputDialog(
            title = "備用密碼解鎖",
            subtitle = "請輸入您的 4~6 位數備用密碼：",
            onConfirm = { pin ->
                showPinDialog = false
                onUnlockWithPin(pin)
            },
            onDismiss = {
                showPinDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("登出") },
            text = { Text("確定要登出嗎？登出後需要重新登入。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("登出") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedRoundedSymbol(
            icon = "lock",
            tint = MaterialTheme.colorScheme.primary,
            size = 80.dp,
            contentDescription = "安全鎖定"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "已鎖定",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "請進行生物辨識來解鎖，或輸入備用密碼解鎖。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onTriggerBiometric,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(
                text = "生物識別解鎖",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { showPinDialog = true },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "使用備用密碼解鎖",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "登出",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
