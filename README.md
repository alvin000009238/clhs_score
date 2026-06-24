# 壢中成績 Android — 中大壢中

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> [!IMPORTANT]
> 本專案為非官方開發之第三方服務，我們與壢中及欣河智慧校園平台無任何直接關聯。

此 repo 以 Android 原生 app 為核心，使用 Kotlin、Jetpack Compose 與 Material 3 實作成績查詢、分析、趨勢比較與課表小工具。

## 功能亮點

- **Android 原生版**：Kotlin + Jetpack Compose + Material 3，手機端直接連線學校系統。
- **成績視覺化**：雷達圖、長條圖、五標落點、分數分布，一眼掌握表現。
- **深色模式與動態色彩**：支援淺色、深色、AMOLED 純黑與 Material You 動態色彩。
- **成績模擬器**：調整各科分數與採計科目，快速試算調整後的平均。
- **歷次趨勢比較**：自動對比前次考試，追蹤進退步軌跡。

## 架構總覽

```mermaid
flowchart TB
    subgraph Repo["Repo"]
        Android["android/\nKotlin / Jetpack Compose / Material 3"]
        Demo["demo/\n展示網站與截圖素材"]
        Workflows[".github/workflows/\nrelease、demo deploy、CodeQL"]
    end

    subgraph AndroidApp["Android app: com.clhs.score"]
        Main["MainActivity\n入口、deep link、FLAG_SECURE、生物識別鎖"]
        AppShell["ScoreApp\nCompose NavHost 與全域狀態同步"]

        subgraph UI["ui/"]
            Login["WebViewLoginScreen\n學校登入與 cookie 回收"]
            Grades["GradesScreen / Subject / Charts\n成績總覽、分析、趨勢、模擬器"]
            ScheduleUi["schedule/ScheduleScreen\n課表查詢與 Widget 設定"]
            SettingsUi["SettingsScreen\n主題、通知、更新、匯出、生物識別"]
            LockUi["BiometricLockScreen\n指紋 / PIN 解鎖覆蓋層"]
        end

        subgraph ViewModels["viewmodel/"]
            ScoreVm["ScoreViewModel\n登入、成績、比較、趨勢、匯出、段考提醒"]
            ScheduleVm["ScheduleViewModel\n課表查詢與 Widget 偏好"]
            SettingsVm["SettingsViewModel\n設定、通知 topic、更新檢查"]
        end

        subgraph Data["data/"]
            GradeRepo["GradeRepository\nsession restore、cache-first 成績讀取"]
            ScheduleRepo["ScheduleRepository\n課表年份、班級、課表內容"]
            SchoolClient["SchoolGradeClient\nOkHttp + Jsoup + JSON parsing"]
            CookieJar["SchoolCookieJar\n同步鎖保護 cookie 讀寫"]
            Stores["SessionStore / GradeCacheStore / SettingsRepository\nDataStore、Security Crypto、本機快取"]
            Analysis["GradeAnalysis / GradeReportDiffer\n分析、趨勢、可見資訊 diff"]
            Exporter["GradeExporter\nMediaStore CSV 匯出"]
            Biometric["BiometricHelper\nKeystore key wrapping"]
            FakeData["FakeData\n-PuseFakeData / demo mode"]
        end

        subgraph Background["背景與系統整合"]
            Reminder["GradeReminderWorker + Scheduler + Notifier\nWorkManager 15 分鐘週期檢查"]
            Widget["ScheduleWidget + Receivers\nGlance widget 與 AlarmManager 更新"]
            Fcm["ScoreFirebaseMessagingService + TopicManager\nFCM 通知與 topic 訂閱"]
            Channels["NotificationChannels\nscore_updates / grade_reminders"]
            FileProvider["FileProvider\nAPK 更新安裝與匯出分享 URI"]
        end

        subgraph Analytics["analytics/"]
            Logger["AnalyticsLogger / FirebaseAnalyticsLogger\n集中匿名事件記錄"]
            Sanitizer["AnalyticsParameterSanitizer\n白名單與敏感欄位防護"]
        end
    end

    subgraph External["外部服務 / Android 系統"]
        School["欣河智慧校園 / 學校系統\nhttps://shcloud2.k12ea.gov.tw/CLHSTYC"]
        Firebase["Firebase\nAnalytics + Cloud Messaging"]
        GitHub["GitHub Releases\n版本檢查與 APK 發布"]
        AndroidSystem["Android System\n通知權限、Keystore、MediaStore、AppWidget、WorkManager"]
    end

    subgraph Tests["驗證"]
        UnitTests["src/test\nRepository、ViewModel、Analytics、Reminder、Boundary tests"]
        UiTests["src/androidTest\nCompose UI test"]
        DebugOnly["src/debug\nGradeReminderDebugReceiver"]
    end

    Android --> Main
    Main --> AppShell
    Main --> LockUi
    AppShell --> UI
    UI --> ViewModels
    Login --> ScoreVm
    ScoreVm --> GradeRepo
    ScoreVm --> Analysis
    ScoreVm --> Exporter
    ScoreVm --> Reminder
    ScheduleVm --> ScheduleRepo
    SettingsVm --> Stores
    SettingsVm --> Fcm
    SettingsVm --> GitHub
    GradeRepo --> SchoolClient
    GradeRepo --> Stores
    ScheduleRepo --> SchoolClient
    ScheduleRepo --> Stores
    SchoolClient --> CookieJar
    SchoolClient --> School
    Reminder --> GradeRepo
    Reminder --> Stores
    Widget --> Stores
    Fcm --> Firebase
    Logger --> Firebase
    Sanitizer --> Logger
    FileProvider --> AndroidSystem
    Biometric --> AndroidSystem
    Stores --> AndroidSystem
    Reminder --> AndroidSystem
    Widget --> AndroidSystem
    Channels --> AndroidSystem
    Workflows --> GitHub
    Workflows --> Android
    Demo --> Workflows
    UnitTests --> ScoreVm
    UnitTests --> GradeRepo
    UiTests --> AppShell
    DebugOnly --> Reminder
```

核心資料流是 `Compose UI -> ViewModel -> Repository -> SchoolGradeClient -> 學校系統`。Repository 會先讀取本機快取與 session，必要時才透過 `SchoolGradeClient` 連線；背景提醒、桌面 Widget、通知、更新檢查與匯出則透過各自的 Android 系統服務保持在 app 邊界內。

## 專案結構

| 目錄 | 說明 | 文件 |
|------|------|------|
| [`android/`](android/) | Kotlin / Jetpack Compose 原生 App | [android/README.md](android/README.md) |
| [`demo/`](demo/) | 展示素材與截圖頁面 | |
| [`.github/workflows/`](.github/workflows/) | Android release、demo deploy 與程式碼掃描 workflow | |

## 快速開始

參見 [`android/README.md`](android/README.md)，使用 Gradle 建置與測試 Android app。

## 貢獻者

[@alvin000009238](https://github.com/alvin000009238)

## License

[MIT](LICENSE) © 2026 alvin000009238
