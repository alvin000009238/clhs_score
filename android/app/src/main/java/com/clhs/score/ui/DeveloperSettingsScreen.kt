package com.clhs.score.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.clhs.score.data.AppSettings
import com.clhs.score.data.DeveloperDiagnostics
import com.clhs.score.data.ErrorDiagnosticContext
import com.clhs.score.data.LocalDataCategory
import com.clhs.score.data.LocalDataCleanupResult
import com.clhs.score.data.StorageEntry
import com.clhs.score.data.StorageDiagnostics
import com.clhs.score.data.defaultClearableLocalDataCategories
import com.clhs.score.data.toReadableSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    settings: AppSettings,
    showRestartDialog: Boolean,
    isLoggedIn: Boolean,
    loginErrorMessage: String?,
    gradesErrorMessage: String?,
    onBack: () -> Unit,
    onSetDemoMode: (Boolean) -> Unit,
    onDismissRestartDialog: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val diagnostics = remember(context) { DeveloperDiagnostics(context.applicationContext) }

    var isBusy by remember { mutableStateOf(false) }
    var storageDiagnostics by remember { mutableStateOf<StorageDiagnostics?>(null) }
    var cleanupResult by remember { mutableStateOf<LocalDataCleanupResult?>(null) }
    var diagnosticReport by remember { mutableStateOf<String?>(null) }
    var selectedCleanupCategories by remember { mutableStateOf(defaultClearableLocalDataCategories()) }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("需要重新啟動") },
            text = { Text("Demo 模式會在重新啟動 App 後完整套用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRestartDialog()
                        val activity = context as? Activity
                        activity?.finishAffinity()
                        kotlin.system.exitProcess(0)
                    },
                ) {
                    Text("關閉 App")
                }
            },
        )
    }

    storageDiagnostics?.let { currentDiagnostics ->
        LocalDataManagementDialog(
            diagnostics = currentDiagnostics,
            cleanupResult = cleanupResult,
            selectedCategories = selectedCleanupCategories,
            isBusy = isBusy,
            onToggleCategory = { category ->
                selectedCleanupCategories = if (category in selectedCleanupCategories) {
                    selectedCleanupCategories - category
                } else {
                    selectedCleanupCategories + category
                }
            },
            onClearSelected = {
                val categories = selectedCleanupCategories
                if (categories.isEmpty()) return@LocalDataManagementDialog
                isBusy = true
                scope.launch {
                    cleanupResult = diagnostics.clearLocalData(categories)
                    storageDiagnostics = cleanupResult?.storageAfterCleanup
                    isBusy = false
                    Toast.makeText(context, "已清除選取資料", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { storageDiagnostics = null },
        )
    }

    diagnosticReport?.let { report ->
        DiagnosticReportDialog(
            report = report,
            onCopy = {
                context.copyText("CLHS Score 診斷包", report)
                Toast.makeText(context, "診斷包已複製", Toast.LENGTH_SHORT).show()
            },
            onShare = { context.shareText(report) },
            onDismiss = { diagnosticReport = null },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 64.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            DeveloperSwitchItem(
                icon = "science",
                title = "Demo 模式",
                subtitle = "使用假資料測試畫面，不依賴學校系統登入。",
                checked = settings.demoMode,
                onCheckedChange = onSetDemoMode,
            )

            DeveloperActionItem(
                icon = "settings",
                title = "本機資料與儲存空間",
                subtitle = "查看各項資料大小，並只清除選取的本機資料。",
                enabled = !isBusy,
                onClick = {
                    isBusy = true
                    scope.launch {
                        cleanupResult = null
                        selectedCleanupCategories = defaultClearableLocalDataCategories()
                        storageDiagnostics = diagnostics.collectStorageDiagnostics()
                        isBusy = false
                    }
                },
            )

            DeveloperActionItem(
                icon = "science",
                title = "錯誤診斷包",
                subtitle = "產生不含帳密、cookie、token、姓名與成績內容的診斷文字。",
                enabled = !isBusy,
                onClick = {
                    isBusy = true
                    scope.launch {
                        diagnosticReport = diagnostics.buildErrorReport(
                            ErrorDiagnosticContext(
                                isLoggedIn = isLoggedIn,
                                loginErrorMessage = loginErrorMessage,
                                gradesErrorMessage = gradesErrorMessage,
                            ),
                        )
                        isBusy = false
                    }
                },
            )

            if (isBusy) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "處理中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            OutlinedRoundedSymbol(
                icon = "arrow_back",
                contentDescription = "返回",
            )
        }
    }
}

@Composable
private fun DeveloperSwitchItem(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedRoundedSymbol(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
                size = 24.dp,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun DeveloperActionItem(
    icon: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.42f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedRoundedSymbol(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                size = 24.dp,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
    }
}

@Composable
private fun LocalDataManagementDialog(
    diagnostics: StorageDiagnostics,
    cleanupResult: LocalDataCleanupResult?,
    selectedCategories: Set<LocalDataCategory>,
    isBusy: Boolean,
    onToggleCategory: (LocalDataCategory) -> Unit,
    onClearSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        title = { Text("本機資料與儲存空間") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cleanupResult?.let {
                    Text(
                        text = "已釋放 ${it.removedBytes.toReadableSize()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                diagnostics.entries.forEach { entry ->
                    StorageEntryRow(
                        entry = entry,
                        checked = entry.category in selectedCategories,
                        enabled = entry.isClearable && !isBusy,
                        onToggle = { onToggleCategory(entry.category) },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "合計",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = diagnostics.totalBytes.toReadableSize(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onClearSelected,
                enabled = !isBusy && selectedCategories.isNotEmpty(),
            ) {
                Text(if (selectedCategories.isEmpty()) "請先選擇" else "清除選取")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("關閉")
            }
        },
    )
}

@Composable
private fun StorageEntryRow(
    entry: StorageEntry,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val contentAlpha = if (entry.isClearable) 1f else 0.62f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.isClearable) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            )
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            if (!entry.isClearable) {
                Text(
                    text = "保留設定、Demo 模式與開發者選項",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = entry.bytes.toReadableSize(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DiagnosticReportDialog(
    report: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("錯誤診斷包") },
        text = {
            Text(
                text = report,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        },
        confirmButton = {
            Button(onClick = onShare) {
                Text("分享")
            }
        },
        dismissButton = {
            Row {
                OutlinedButton(onClick = onCopy) {
                    Text("複製")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("關閉")
                }
            }
        },
    )
}

private fun Context.copyText(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "CLHS Score 診斷包")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, "分享診斷包"))
}
