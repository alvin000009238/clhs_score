package com.clhs.score.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clhs.score.BuildConfig
import com.clhs.score.R
import com.clhs.score.analytics.UsageMetric
import com.clhs.score.analytics.UsageStatistics
import com.clhs.score.data.AppSettings
import com.clhs.score.data.BiometricHelper
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.ThemeMode
import com.clhs.score.data.YearTermOption
import com.clhs.score.notifications.canPostNotifications
import com.clhs.score.notifications.hasPostNotificationsPermission
import com.clhs.score.notifications.openAppNotificationSettings
import com.clhs.score.notifications.shouldShowPostNotificationsRationale
import com.clhs.score.ui.components.PinSetupDialog
import com.clhs.score.ui.theme.OutfitFontFamily
import com.clhs.score.viewmodel.SettingsUiState
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SourceCodeUrl = "https://github.com/alvin000009238/clhs_score"
private const val FeedbackFormUrlKey = "feedback_form_url"

private val MIT_LICENSE_TEXT = """
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
""".trimIndent()

private val APACHE_LICENSE_TEXT = """
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity. For the purposes of this definition, "control" means (i) the power, direct or indirect, to cause the direction or management of such entity, whether by contract or otherwise, or (ii) ownership of fifty percent (50%) or more of the outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.

      "Object" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship. For the purposes of this License, Derivative Works shall not include works that remain separable from, or merely link (or bind by name) to the interfaces of, the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to the Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner. For the purposes of this definition, "submitted" means any form of electronic, verbal, or written communication sent to the Licensor or its representatives, including but not limited to communication on electronic mailing lists, source code control systems, and issue tracking systems that are managed by, or on behalf of, the Licensor for the purpose of discussing and improving the Work, but excluding communication that is conspicuously marked or otherwise designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by the Licensor and subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this section) patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work, where such license applies only to those patent claims licensable by such Contributor that are necessarily infringed by their Contribution(s) alone or by combination of their Contribution(s) with the Work to which such Contribution(s) was submitted. If You institute patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Work or a Contribution incorporated within the Work constitutes direct or contributory patent infringement, then any patent licenses granted to You under this License for that Work shall terminate as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions:

      (a) You must give any other recipients of the Work or Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work, excluding those notices that do not pertain to any part of the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its distribution, then any Derivative Works that You distribute must include a readable copy of the attribution notices contained within such NOTICE file, excluding any notices that do not pertain to any part of the Derivative Works, in at least one of the following places: within a NOTICE text file distributed as part of the Derivative Works; within the Source form or documentation, if provided along with the Derivative Works; or, within a display generated by the Derivative Works, if and wherever such third-party notices normally appear. The contents of the NOTICE file are for informational purposes only and do not modify the License. You may add Your own attribution notices within Derivative Works that You distribute, alongside or as an addendum to the NOTICE text from the Work, provided that such additional attribution notices cannot be construed as modifying the License.

      You may add Your own copyright statement to Your modifications and may provide additional or different license terms and conditions for use, reproduction, or distribution of Your modifications, or for any such Derivative Works as a whole, provided Your use, reproduction, and distribution of the Work otherwise complies with the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise, any Contribution intentionally submitted for inclusion in the Work by You to the Licensor shall be under the terms and conditions of this License, without any additional terms or conditions. Notwithstanding the above, nothing herein shall supersede or modify the terms of any separate license agreement you may have executed with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade names, trademarks, service marks, or product names of the Licensor, except as required for reasonable and customary use in describing the origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory, whether in tort (including negligence), contract, or otherwise, unless required by applicable law (such as deliberate and grossly negligent acts) or agreed to in writing, shall any Contributor be liable to You for damages, including any direct, indirect, special, incidental, or consequential damages of any character arising as a result of this License or out of the use or inability to use the Work (including but not limited to damages for loss of goodwill, work stoppage, computer failure or malfunction, or any and all other commercial damages or losses), even if such Contributor has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing the Work or Derivative Works thereof, You may choose to offer, and charge a fee for, acceptance of support, warranty, indemnity, or other liability obligations and/or rights consistent with this License. However, in accepting such obligations, You may act only on Your own behalf and on Your sole responsibility, not on behalf of any other Contributor, and only if You agree to indemnify, defend, and hold each Contributor harmless for any liability incurred by, or claims asserted against, such Contributor by reason of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS
""".trimIndent()

private val APACHE_COMPONENTS = listOf(
    "AndroidX Activity Compose",
    "AndroidX Biometric",
    "AndroidX Compose Foundation",
    "AndroidX Compose Material 3",
    "AndroidX Compose Runtime",
    "AndroidX Compose UI",
    "AndroidX Compose UI Graphics",
    "AndroidX Compose UI Tooling Preview",
    "AndroidX Core Splashscreen",
    "AndroidX DataStore Preferences",
    "AndroidX Glance AppWidget",
    "AndroidX Glance Material 3",
    "AndroidX Lifecycle Runtime Compose",
    "AndroidX Lifecycle ViewModel Compose",
    "AndroidX Navigation Compose",
    "AndroidX Security Crypto",
    "AndroidX WorkManager Runtime KTX",
    "Firebase Cloud Messaging",
    "Firebase Remote Config",
    "Kotlin Standard Library",
    "kotlinx.coroutines Android",
    "kotlinx.serialization JSON",
    "Material Symbols Rounded",
    "Multiplatform Markdown Renderer",
    "OkHttp",
)

internal data class LicenseEntry(
    val componentName: String,
    val licenseName: String,
    val licenseText: String,
)

internal fun buildThirdPartyLicenses(outfitLicenseText: String): List<LicenseEntry> =
    (APACHE_COMPONENTS.map {
        LicenseEntry(it, "Apache License 2.0", APACHE_LICENSE_TEXT)
    } + listOf(
        LicenseEntry("jsoup", "MIT License", MIT_LICENSE_TEXT),
        LicenseEntry("Outfit", "SIL Open Font License 1.1", outfitLicenseText),
    )).sortedBy { it.componentName.lowercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    uiState: SettingsUiState,
    structure: List<YearTermOption>,
    isExporting: Boolean,
    exportResult: String?,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdateResult: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onExportGrades: (List<ExamSelection>) -> Unit,
    onDismissExportResult: () -> Unit,
    onLogout: () -> Unit,
    onSetBiometricEnabled: (Boolean, String?) -> Unit,
) {
    SubpageLayout(onBack = onBack) {
        SettingsContent(
            settings = settings,
            uiState = uiState,
            structure = structure,
            isExporting = isExporting,
            exportResult = exportResult,
            onSetThemeMode = onSetThemeMode,
            onSetDynamicColor = onSetDynamicColor,
            onSetAmoledBlack = onSetAmoledBlack,
            onSetNotificationsEnabled = onSetNotificationsEnabled,
            onCheckUpdate = onCheckUpdate,
            onOpenAbout = onOpenAbout,
            onDismissDeveloperToast = onDismissDeveloperToast,
            onOpenDeveloperSettings = onOpenDeveloperSettings,
            onExportGrades = onExportGrades,
            onDismissExportResult = onDismissExportResult,
            onLogout = onLogout,
            onSetBiometricEnabled = onSetBiometricEnabled,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            includeTopSpacer = true,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    settings: AppSettings,
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onVersionTap: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onOpenUsageStatistics: () -> Unit,
    onOpenSourceLicenses: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnDismissDeveloperToast by rememberUpdatedState(onDismissDeveloperToast)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val remoteConfig = remember {
        FirebaseRemoteConfig.getInstance().also { config ->
            config.setDefaultsAsync(mapOf(FeedbackFormUrlKey to ""))
        }
    }
    var isFetchingFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.showDeveloperUnlockedToast) {
        if (uiState.showDeveloperUnlockedToast) {
            Toast.makeText(context, "已開啟開發者選項", Toast.LENGTH_SHORT).show()
            currentOnDismissDeveloperToast()
        }
    }

    SubpageLayout(
        onBack = onBack,
        title = "關於",
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryColor,
                                    tertiaryColor,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.clhs_pocket_foreground),
                        contentDescription = "CLHS Pocket",
                        modifier = Modifier.size(80.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(androidx.compose.ui.graphics.Color.White),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App 名稱
                Text(
                    text = "CLHS Pocket",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "壢中校園口袋工具",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    AboutInfoRow(
                        icon = "info",
                        title = "版本",
                        value = BuildConfig.VERSION_NAME,
                        onClick = onVersionTap,
                        index = 0,
                        count = 5,
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
                    AboutInfoRow(
                        icon = "license",
                        title = "授權",
                        value = "MIT License",
                        onClick = onOpenSourceLicenses,
                        index = 1,
                        count = 5,
                        trailing = { ForwardIndicator() },
                    )
                    AboutInfoRow(
                        icon = "newsstand",
                        title = "使用統計",
                        value = "查看",
                        onClick = onOpenUsageStatistics,
                        index = 2,
                        count = 5,
                        trailing = { ForwardIndicator() },
                    )
                    AboutInfoRow(
                        icon = "thumbs_up_down",
                        title = "回饋意見",
                        value = if (isFetchingFeedback) "載入中" else "填寫",
                        enabled = !isFetchingFeedback,
                        onClick = {
                            isFetchingFeedback = true
                            remoteConfig.fetchAndActivate().addOnCompleteListener {
                                isFetchingFeedback = false
                                val url = remoteConfig.getString(FeedbackFormUrlKey)
                                if (isAllowedFeedbackFormUrl(url)) {
                                    openExternalUrl(context, url)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "暫時無法取得回饋表單，請稍後再試",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        index = 3,
                        count = 5,
                        trailing = {
                            if (isFetchingFeedback) {
                                LoadingIndicator(modifier = Modifier.size(32.dp))
                            } else {
                                ForwardIndicator()
                            }
                        },
                    )
                    AboutInfoRow(
                        icon = "code",
                        title = "原始碼",
                        value = "GitHub",
                        onClick = { openExternalUrl(context, SourceCodeUrl) },
                        index = 4,
                        count = 5,
                        trailing = { ForwardIndicator() },
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(32.dp))

                // Footer
                Text(
                    text = "© 2026 alvin000009238",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Made with ❤\uFE0F",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UsageStatisticsScreen(
    statistics: UsageStatistics,
    onBack: () -> Unit,
) {
    val trackingPeriod = statistics.startedAtMillis?.let { millis ->
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        "自 $date 起"
    } ?: "尚未開始記錄"

    SubpageLayout(
        onBack = onBack,
        title = "使用統計",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "你的使用概況",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$trackingPeriod，以下資料只儲存在這台裝置。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                usageStatisticRows.forEachIndexed { index, (metric, label) ->
                    UsageStatisticRow(
                        label = label,
                        count = statistics.count(metric),
                        index = index,
                        itemCount = usageStatisticRows.size,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "統計不包含學號、姓名、成績、排名或登入資訊。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private val usageStatisticRows = listOf(
    UsageMetric.APP_OPEN to "App 開啟",
    UsageMetric.GRADE_QUERY to "讀取成績成功",
    UsageMetric.SCHEDULE_OPEN to "課表開啟",
    UsageMetric.SUBJECT_TREND_OPEN to "科目趨勢開啟",
    UsageMetric.SCORE_SIMULATOR_OPEN to "成績模擬器開啟",
    UsageMetric.GRADE_EXPORT to "成績匯出成功",
    UsageMetric.GRADE_REMINDER_START to "段考提醒啟用成功",
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UsageStatisticRow(
    label: String,
    count: Long,
    index: Int,
    itemCount: Int,
) {
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index = index, count = itemCount),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        trailingContent = {
            Text(
                text = "$count 次",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutInfoRow(
    icon: String,
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    index: Int,
    count: Int,
    trailing: @Composable (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = {
            OutlinedRoundedSymbol(
                icon = icon,
                size = 24.dp,
                contentDescription = null,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                )
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    trailing()
                }
            }
        },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            leadingContentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = title)
    }
}

internal fun isAllowedFeedbackFormUrl(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        uri.userInfo != null ||
        (uri.port != -1 && uri.port != 443)
    ) {
        return false
    }
    return when (uri.host?.lowercase()) {
        "forms.gle" -> !uri.path.isNullOrBlank() && uri.path != "/"
        "docs.google.com" -> uri.path?.startsWith("/forms/") == true
        else -> false
    }
}

@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val outfitLicenseText = remember {
        context.resources.openRawResource(R.raw.outfit_ofl)
            .bufferedReader()
            .use { it.readText() }
    }
    val thirdPartyLicenses = remember(outfitLicenseText) {
        buildThirdPartyLicenses(outfitLicenseText)
    }

    SubpageLayout(
        onBack = onBack,
        title = "開放原始碼授權",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader("專案授權")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            OutlinedRoundedSymbol(
                                icon = "license",
                                tint = MaterialTheme.colorScheme.primary,
                                size = 20.dp,
                                contentDescription = null,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MIT License",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Copyright (c) 2026 alvin000009238",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    Text(
                        text = MIT_LICENSE_TEXT,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }
            }

            SectionHeader("第三方元件（${thirdPartyLicenses.size}）")

            thirdPartyLicenses.forEach { license ->
                ThirdPartyLicenseCard(
                    componentName = license.componentName,
                    licenseName = license.licenseName,
                    licenseText = license.licenseText,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThirdPartyLicenseCard(
    componentName: String,
    licenseName: String,
    licenseText: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = componentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = licenseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedRoundedSymbol(
                    icon = if (expanded) "keyboard_arrow_up" else "keyboard_arrow_down",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 22.dp,
                    contentDescription = if (expanded) "收合" else "展開",
                )
            }
            if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = licenseText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsContent(
    settings: AppSettings,
    uiState: SettingsUiState,
    structure: List<YearTermOption>,
    isExporting: Boolean,
    exportResult: String?,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onExportGrades: (List<ExamSelection>) -> Unit,
    onDismissExportResult: () -> Unit,
    onLogout: () -> Unit,
    onSetBiometricEnabled: (Boolean, String?) -> Unit,
    modifier: Modifier = Modifier,
    includeTopSpacer: Boolean = false,
    showLogout: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var awaitingNotificationSettings by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionDenied by rememberSaveable { mutableStateOf(false) }
    val currentOnSetNotificationsEnabled by rememberUpdatedState(onSetNotificationsEnabled)
    val currentOnDismissDeveloperToast by rememberUpdatedState(onDismissDeveloperToast)
    val currentOnDismissExportResult by rememberUpdatedState(onDismissExportResult)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        currentOnSetNotificationsEnabled(granted)
        if (!granted) {
            notificationPermissionDenied = true
        }
        Toast.makeText(
            context,
            if (granted) "已開啟推播通知" else "未取得通知權限，可再次嘗試開啟",
            Toast.LENGTH_SHORT,
        ).show()
    }

    DisposableEffect(lifecycleOwner, settings.notificationsEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) {
                return@LifecycleEventObserver
            }
            if (awaitingNotificationSettings) {
                awaitingNotificationSettings = false
                if (context.canPostNotifications()) {
                    currentOnSetNotificationsEnabled(true)
                    Toast.makeText(context, "已開啟推播通知", Toast.LENGTH_SHORT).show()
                } else {
                    currentOnSetNotificationsEnabled(false)
                    Toast.makeText(context, "未取得通知權限，暫不接收推播通知", Toast.LENGTH_SHORT).show()
                }
            } else if (settings.notificationsEnabled && !context.canPostNotifications()) {
                currentOnSetNotificationsEnabled(false)
                Toast.makeText(context, "系統通知權限已關閉，已同步關閉通知", Toast.LENGTH_SHORT).show()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onNotificationToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            onSetNotificationsEnabled(false)
        } else if (context.canPostNotifications()) {
            onSetNotificationsEnabled(true)
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasPostNotificationsPermission() &&
            (
                !notificationPermissionDenied ||
                    context.shouldShowPostNotificationsRationale()
            )
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            awaitingNotificationSettings = true
            if (context.openAppNotificationSettings()) {
                Toast.makeText(context, "請在系統設定中開啟通知", Toast.LENGTH_SHORT).show()
            } else {
                awaitingNotificationSettings = false
                Toast.makeText(context, "無法開啟通知設定，請手動到系統設定開啟", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.showDeveloperUnlockedToast) {
        if (uiState.showDeveloperUnlockedToast) {
            Toast.makeText(context, "已開啟開發者選項", Toast.LENGTH_SHORT).show()
            currentOnDismissDeveloperToast()
        }
    }

    LaunchedEffect(exportResult) {
        if (exportResult != null) {
            Toast.makeText(context, exportResult, Toast.LENGTH_LONG).show()
            currentOnDismissExportResult()
        }
    }

    if (showExportDialog) {
        ExportDialog(
            structure = structure,
            onConfirm = { selections ->
                showExportDialog = false
                onExportGrades(selections)
            },
            onDismiss = { showExportDialog = false },
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onConfirm = { pin ->
                showPinSetupDialog = false
                onSetBiometricEnabled(true, pin)
            },
            onDismiss = { showPinSetupDialog = false }
        )
    }

    if (showLogout && showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
        )
    }

    val utilityMotion = remember { MotionScheme.standard() }
    MaterialTheme(motionScheme = utilityMotion) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (includeTopSpacer) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(56.dp))
            }

            SectionHeader("外觀")
            GeneralSettingsCard(
                settings = settings,
                onSetThemeMode = onSetThemeMode,
                onSetDynamicColor = onSetDynamicColor,
                onSetAmoledBlack = onSetAmoledBlack,
            )

            SectionHeader("通知與提醒")
            SettingsSwitchListItem(
                icon = "notifications",
                title = "通知",
                subtitle = "接收 app 更新與公告推播",
                checked = settings.notificationsEnabled,
                onCheckedChange = onNotificationToggle,
            )

            SectionHeader("資料與隱私")
            val biometricAvailable = BiometricHelper.canAuthenticate(context)
            val dataPrivacyItemCount = if (biometricAvailable) 2 else 1
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (biometricAvailable) {
                    val onBiometricToggle: (Boolean) -> Unit = { enabled ->
                        if (enabled) {
                            showPinSetupDialog = true
                        } else {
                            onSetBiometricEnabled(false, null)
                        }
                    }
                    SettingsSwitchListItem(
                        icon = "fingerprint",
                        title = "生物識別解鎖",
                        subtitle = "開啟後，每次啟動 App 均需進行驗證",
                        checked = settings.biometricEnabled,
                        onCheckedChange = onBiometricToggle,
                        index = 0,
                        count = dataPrivacyItemCount,
                    )
                }
                ClickableSettingsItem(
                    icon = "download",
                    title = "匯出成績",
                    subtitle = if (isExporting) "匯出中…" else "將成績資料匯出成 CSV",
                    onClick = { if (!isExporting) showExportDialog = true },
                    index = dataPrivacyItemCount - 1,
                    count = dataPrivacyItemCount,
                    trailing = {
                        if (isExporting) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                        }
                    },
                )
            }

            SectionHeader("關於")
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickableSettingsItem(
                    icon = "system_update",
                    title = "檢查更新",
                    subtitle = if (uiState.isCheckingUpdate) "檢查中…" else "從 GitHub 取得最新版本",
                    onClick = onCheckUpdate,
                    index = 0,
                    count = 2,
                    trailing = {
                        if (uiState.isCheckingUpdate) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                        }
                    },
                )
                ClickableSettingsItem(
                    icon = "info",
                    title = "關於",
                    subtitle = "版本、授權與原始碼",
                    onClick = onOpenAbout,
                    index = 1,
                    count = 2,
                )
            }

            AnimatedVisibility(
                visible = settings.developerEnabled,
                enter = fadeIn(utilityMotion.defaultEffectsSpec()) +
                    expandVertically(animationSpec = utilityMotion.defaultSpatialSpec()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader("開發者選項")
                    ClickableSettingsItem(
                        icon = "science",
                        title = "開發者選項",
                        subtitle = "其他進階設定",
                        onClick = onOpenDeveloperSettings,
                    )
                }
            }

            if (showLogout) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLogoutDialog = true },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = "登出",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登出") },
        text = { Text("確定要登出嗎？登出後成績資料將被刪除。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("登出") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) { Text("取消") }
        },
    )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneralSettingsCard(
    settings: AppSettings,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
) {
    val isDarkActive = settings.themeMode == ThemeMode.DARK ||
        (settings.themeMode == ThemeMode.SYSTEM /* assume could be dark */)

    val appearanceItemCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 2 else 1
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
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
                        text = "主題",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }

                val options = listOf("系統" to ThemeMode.SYSTEM, "淺色" to ThemeMode.LIGHT, "深色" to ThemeMode.DARK)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (label, mode) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            icon = {
                                if (settings.themeMode == mode && mode != ThemeMode.SYSTEM) {
                                    OutlinedRoundedSymbol(
                                        icon = if (mode == ThemeMode.LIGHT) "light_mode" else "dark_mode",
                                        size = 18.dp,
                                        contentDescription = null,
                                    )
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsSwitchListItem(
                icon = "palette",
                title = "動態色彩",
                subtitle = "依照桌布色彩調整",
                checked = settings.dynamicColor,
                onCheckedChange = onSetDynamicColor,
                index = 0,
                count = appearanceItemCount,
            )
        }
        SettingsSwitchListItem(
            icon = "dark_mode",
            title = "純黑背景",
            subtitle = "在深色模式使用純黑背景（AMOLED）",
            checked = settings.amoledBlack,
            onCheckedChange = onSetAmoledBlack,
            enabled = isDarkActive,
            index = appearanceItemCount - 1,
            count = appearanceItemCount,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsSwitchListItem(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    index: Int = 0,
    count: Int = 1,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = count,
            defaultShapes = if (count == 1) {
                ListItemDefaults.shapes(shape = MaterialTheme.shapes.large)
            } else {
                ListItemDefaults.shapes()
            },
        ),
        supportingContent = {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        leadingContent = {
            OutlinedRoundedSymbol(
                icon = icon,
                size = 24.dp,
                contentDescription = null,
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            leadingContentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Text(text = title)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClickableSettingsItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    index: Int = 0,
    count: Int = 1,
    trailing: @Composable (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = count,
            defaultShapes = if (count == 1) {
                ListItemDefaults.shapes(shape = MaterialTheme.shapes.large)
            } else {
                ListItemDefaults.shapes()
            },
        ),
        supportingContent = {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        leadingContent = {
            OutlinedRoundedSymbol(
                icon = icon,
                size = 24.dp,
                contentDescription = null,
            )
        },
        trailingContent = {
            if (trailing != null) {
                trailing()
            } else {
                ForwardIndicator()
            }
        },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            leadingContentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Text(text = title)
    }
}

@Composable
private fun ForwardIndicator() {
    OutlinedRoundedSymbol(
        icon = "keyboard_arrow_right",
        size = 24.dp,
        contentDescription = null,
    )
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: Exception) {
        Toast.makeText(context, "無法開啟連結", Toast.LENGTH_SHORT).show()
    }
}
