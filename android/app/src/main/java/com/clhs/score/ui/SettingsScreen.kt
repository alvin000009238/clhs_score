package com.clhs.score.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clhs.score.BuildConfig
import com.clhs.score.data.AppSettings
import com.clhs.score.data.ThemeMode
import com.clhs.score.data.UpdateResult
import com.clhs.score.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdateResult: () -> Unit,
    onVersionTap: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.showDeveloperUnlockedToast) {
        if (uiState.showDeveloperUnlockedToast) {
            Toast.makeText(context, "已開啟開發者選項", Toast.LENGTH_SHORT).show()
            onDismissDeveloperToast()
        }
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

    UpdateResultDialog(
        result = uiState.updateResult,
        onDismiss = onDismissUpdateResult,
    )

    SubpageLayout(onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(56.dp))

            SectionHeader("一般")
            AppearanceCard(
                settings = settings,
                onSetThemeMode = onSetThemeMode,
                onSetDynamicColor = onSetDynamicColor,
                onSetAmoledBlack = onSetAmoledBlack,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ClickableSettingsItem(
                icon = "system_update",
                title = "檢查更新",
                subtitle = if (uiState.isCheckingUpdate) "檢查中…" else "從 GitHub 取得最新版本",
                onClick = onCheckUpdate,
                trailing = {
                    if (uiState.isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )

            ClickableSettingsItem(
                icon = "info",
                title = "版本",
                subtitle = BuildConfig.VERSION_NAME,
                onClick = onVersionTap,
                trailing = {
                    val remaining = 10 - uiState.versionTapCount
                    if (remaining in 1..6 && !settings.developerEnabled) {
                        Text(
                            text = "再 $remaining 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )

            AnimatedVisibility(
                visible = settings.developerEnabled,
                enter = fadeIn() + expandVertically(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("開發者選項")
                    ClickableSettingsItem(
                        icon = "science",
                        title = "開發者選項",
                        subtitle = "演示模式與其他進階設定",
                        onClick = onOpenDeveloperSettings,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = "登出",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(
    settings: AppSettings,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
) {
    val isDarkActive = settings.themeMode == ThemeMode.DARK ||
        (settings.themeMode == ThemeMode.SYSTEM /* assume could be dark */)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedRoundedSymbol(
                    icon = "brightness_medium",
                    tint = MaterialTheme.colorScheme.primary,
                    size = 22.dp,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "外觀",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val options = listOf("系統" to ThemeMode.SYSTEM, "淺色" to ThemeMode.LIGHT, "深色" to ThemeMode.DARK)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (label, mode) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                        ),
                    ) {
                        Text(label)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchSettingsRow(
                    icon = "palette",
                    title = "動態色彩",
                    subtitle = "依照桌布色彩調整",
                    checked = settings.dynamicColor,
                    onCheckedChange = onSetDynamicColor,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchSettingsRow(
                icon = "dark_mode",
                title = "純黑背景",
                subtitle = "在深色模式使用純黑背景（AMOLED）",
                checked = settings.amoledBlack,
                onCheckedChange = onSetAmoledBlack,
                enabled = isDarkActive,
            )
        }
    }
}

@Composable
private fun SwitchSettingsRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.42f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedRoundedSymbol(
            icon = icon,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            size = 22.dp,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ClickableSettingsItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedRoundedSymbol(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
                size = 22.dp,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun UpdateResultDialog(
    result: UpdateResult?,
    onDismiss: () -> Unit,
) {
    if (result == null) return
    val context = LocalContext.current
    when (result) {
        is UpdateResult.UpToDate -> {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
        is UpdateResult.Error -> {
            LaunchedEffect(result) {
                Toast.makeText(context, "檢查更新失敗：${result.message}", Toast.LENGTH_LONG).show()
                onDismiss()
            }
        }
        is UpdateResult.NewVersion -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("有新版本") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("v${result.versionName} 已可更新")
                        if (result.releaseNotes.isNotBlank()) {
                            Text(
                                text = result.releaseNotes.take(300),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val url = result.apkDownloadUrl ?: result.htmlUrl
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        context.startActivity(intent)
                        onDismiss()
                    }) {
                        Text(if (result.apkDownloadUrl != null) "下載 APK" else "前往 GitHub")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("稍後") }
                },
            )
        }
    }
}
