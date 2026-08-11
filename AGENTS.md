# AGENTS.md

本檔給 AI agent 在此 repo 工作時使用。請先讀實際程式碼再改動，避免依照通用模板猜測架構。

## 專案結構

- `android/`：獨立 Kotlin / Jetpack Compose / Material 3 Android app，package 為 `com.clhs.score`。
- `demo/`：展示素材與截圖頁面。
- `.github/workflows/`：此 repo 的 Android release、demo deploy 與通用檢查 workflow。

## 工作規則

- 優先保留既有資料流程與檔案分層。UI 改版請先找現有 screen、theme、chart 元件，不要另建平行 app。
- 不要把帳密、token、cookie 或正式環境 secret 寫進程式碼或對話；使用 `.env`、local properties 或本機設定檔。
- 此 repo 的 Markdown 預設會被 `.gitignore` 忽略；新增或更新 agent 文件後要確認 `AGENTS.md` 沒有被 ignore。
- 文件預設使用繁體中文；程式碼註解只在能降低理解成本時加入。
- **Cookie 與 Session 同步安全**：為避免並行 API 呼叫時出現 Cookie 覆蓋或競態條件導致 HTTP 401 錯誤，請勿在各個 API 請求方法中無條件呼叫 `cookieJar.replace(session.cookies, ...)`。應經由以學號過濾與鎖保護的 `prepareSession(session)` 進行按需載入。且 `SchoolCookieJar` 的讀寫方法（`saveFromResponse`, `loadForRequest`, `replace`, `clear`）都必須在 `synchronized` 同步鎖保護下執行，確保執行緒安全。

## Android 校務系統 WebView

- 登入後的校務系統 WebView 只能使用 `ScoreViewModel.getCurrentSession()` 提供的已解鎖記憶體 session；不得自行讀取一般、生物識別或提醒專用 session。
- WebView 只能導向 `https://shcloud2.k12ea.gov.tw`，不得把登入 cookie 送往其他網域。
- 離開 WebView 或登出時必須清除 WebView cookie 與網站資料；`ArchitectureBoundaryTest` 會檢查這些邊界。

## 常用驗證

- Android：在 `android/` 內執行 `.\gradlew.bat test`

在 Windows Codex 環境跑 Android Gradle 時，若 `java` 不在 PATH，使用 Android Studio 內建 JBR，並將 `GRADLE_USER_HOME`、`ANDROID_USER_HOME` 指到 workspace 內的暫存目錄。若測試一開始就出現 `could not open ...\jbr\lib\jvm.cfg`，通常是設定的 Android Studio JBR 路徑不存在或不完整；先用 `Test-Path` 或列出 `C:\Program Files\Android\Android Studio*` 確認實際 JBR 位置。本機曾遇到 `C:\Program Files\Android\Android Studio\jbr` 不可用，而 `C:\Program Files\Android\Android Studio1\jbr` 可用。

## Android release

- 推送 `v*` tag 會觸發 `.github/workflows/android-release.yml`，建立 signed `arm64-v8a` release APK 並發布 GitHub Release。
- Release notes 來自 `CHANGELOG.md` 內與 tag 對應的 `## [x.y.z]` 區塊；新增版本時要先補 changelog。
- `CHANGELOG.md` 的新版本內容必須和上一個版本比較，並比照既有 `v1.1.0` 的分類寫法（Features、Bug Fixes、Performance Improvements），不要混入更早版本已經發布的內容。
- **重要規則**：將更新發布推送至 GitHub 前，必須先將 `CHANGELOG.md` 寫完並請使用者檢查和修改，確認無誤後才能推送。
- **重要規則**：`CHANGELOG.md` 僅用於記錄與 Android app 有關的更新。若僅修改展示素材、文件或 workflow，請勿新增版本號。
- GitHub Secrets 需設定 `ANDROID_RELEASE_KEYSTORE_BASE64`、`ANDROID_RELEASE_KEYSTORE_PASSWORD`、`ANDROID_RELEASE_KEY_ALIAS`、`ANDROID_RELEASE_KEY_PASSWORD`。不要提交 keystore 或密碼。
- App 內更新只能下載 APK 後呼叫系統安裝器；Android 不允許靜默安裝。下載的 APK 透過 `FileProvider` 暫存在 app cache 的 `updates/`，若系統要求，使用者需在安裝流程中允許此 App 安裝未知應用。

## Android UI fake data

- UI 展示資料集中在 `android/app/src/main/java/com/clhs/score/data/FakeData.kt`；新增畫面預覽或假資料情境時優先擴充這裡，不要在 Composable 內臨時硬編資料。
- Android app 可用 `-PuseFakeData=true` 切到 `FakeGradeRepository`，讓登入後成績列表、平均、班排、各科分析、圖表與模擬器都不依賴 API。
- Compose Preview 入口在 `android/app/src/main/java/com/clhs/score/ui/ScorePreviews.kt`，應直接使用 `FakeData` 組 `GradesUiState`。

## Android 自適應版面與返回手勢

- 成績頁以可用寬度 `600dp` / `840dp` 為斷點，分別使用底部導覽、Navigation Rail 與側欄；寬畫面內容在頁面內分欄，但仍共用既有 pager 與各頁 scroll state。
- 成績模擬器在 `600dp` 以上將摘要固定於左側安全區中央、操作內容置於右側捲動；system bar inset 必須讀取目前值，不要跨旋轉快取。
- 成績頁 pager 只能由導覽控制，不開放手勢切頁；切換時即使 page 相同但仍有 offset，也要完成回正。
- Predictive Back 由 Navigation Compose 與 Material drawer 的 `drawerState` 管理；不要在登入頁或成績頁另加攔截相同流程的 `BackHandler`。
- 獨立 Activity 必須呼叫 `enableEdgeToEdge()`；未由 Scaffold 消化 insets 的全畫面內容需自行處理 `safeDrawing`，有文字輸入的畫面也需處理 IME inset。

## Android Material Symbols subset

- Material Symbols rounded icon 由 `android/app/src/main/res/font/material_symbols_rounded_*_subset.ttf` 提供，不要重新加入 `dev.vicart:compose-material-symbols` 整包依賴。
- 新增 icon ligature 時，先更新 `android/scripts/generate_material_symbol_subset.py` 的 `ICONS` 清單，再執行 `python android/scripts/generate_material_symbol_subset.py` 重新產生 outline / filled subset font。產生器找不到舊 Gradle AAR 時會下載 Google 官方原始字型；離線環境可用 `--source-font` 指定本機字型。

## Android Material 3 Expressive

- App 根主題使用 `MaterialExpressiveTheme` 與 `MotionScheme.expressive()`；高頻工具型清單、設定與安全流程改用 `MotionScheme.standard()` 的 effects／spatial spec。
- 一般容器優先使用 `MaterialTheme.shapes` 與 surface container roles；固定小圓角只保留在圖表、課表格與 Widget 等資料幾何。
- Button、IconButton 與 toggle control 優先使用 Material 3 Expressive 的 `shapes()`／`toggleableShapes()`，並維持至少 48dp 觸控區。
- Glance Widget 不能直接套用 `MaterialExpressiveTheme`；只同步 App 的色彩、字級與資訊層級，並保留每個條件分支明確設定背景與圓角。

## Android 品牌字型與開源授權

- App 內可見的 `CLHS Pocket` 品牌字樣共用 `ScoreTheme.kt` 的 `OutfitFontFamily`；`outfit_bold_subset.ttf` 只保留目前品牌名稱需要的 Outfit Bold 700 字形。品牌文字或字重變更時，需從 Google Fonts 官方 Outfit 字型重新產生 subset，不能直接顯示 subset 未包含的字元。
- `OpenSourceLicensesScreen` 透過 `buildThirdPartyLicenses(...)` 逐項顯示實際開源元件與各自授權；不要把非開源 SDK Terms 混進「開放原始碼授權」。新增或移除 runtime 開源元件時，同步更新 `OpenSourceLicensesTest` 的數量與唯一性檢查。

## Android R8 與安裝包大小

- Release build 已啟用 `isMinifyEnabled` 與 `isShrinkResources`；新增 library 或功能時不要用 `-keep class androidx.**`、`-keep class org.jsoup.**` 這類 broad keep 擋住 R8。優先依賴 library 自帶的 consumer rules，只針對 app 端需要反射或跨版本保留名稱的入口加最小規則，例如 WorkManager worker class name，並用 `:app:assembleRelease` 比對 APK 大小。

## Android FCM notifications

- Android app 使用 Firebase Cloud Messaging 接收手動推播；目前發送端預設是 Firebase Console，不需要把 FCM server key、service account 或其他私鑰放進 app。
- `android/app/google-services.json` 是 Firebase app 設定檔，需保留在 app module 根目錄並允許進版控；不要提交 Firebase service account JSON。
- 使用者在設定頁開啟通知後，app 會訂閱 `general` 與 `app_updates` topics；關閉通知時會取消訂閱。
- Android 13 以上的 `POST_NOTIFICATIONS` 權限使用 Compose `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`，並只在使用者主動開啟推播或段考提醒時請求；Android 12 以下不啟動 launcher。使用者反覆拒絕或系統封鎖通知時，改開 App 通知設定頁並在返回 App 時重新檢查 `checkSelfPermission()` 與 `NotificationManagerCompat.areNotificationsEnabled()`。
- 系統通知權限與 App 內通知開關的同步由 `ScoreApp` 根層處理；不要只放在設定頁，否則使用者從系統設定封鎖通知後，其他入口回 App 時狀態會不一致。
- 發送 app 更新通知時使用 `app_updates` topic，一般公告使用 `general` topic。可在 FCM data payload 帶 `url`，使用者點通知時會開啟該網址。

## Android Firebase Analytics

- Firebase Analytics 事件經由 `com.clhs.score.analytics.AnalyticsLogger` 與 `FirebaseAnalyticsLogger` 集中記錄；不要在 UI、ViewModel 或 service 內直接呼叫 Firebase `logEvent`，避免事件名稱與隱私規則分散。
- Analytics 採嚴格匿名策略：不得呼叫 `setUserId`，不得送學號、姓名、班級、座號、成績、排名、科目名稱、考試名稱、URL、cookie、token、rawResult 或錯誤原文。事件參數只允許 enum 字串、布林、計數與 bucket。
- 新增事件或參數時，先更新 `AnalyticsEvents.kt` / `AnalyticsParameterSanitizer.kt` 的常數與白名單，並補 `AnalyticsParameterSanitizerTest` 或 `ArchitectureBoundaryTest`，確保敏感欄位不會被送出。

## Android Firebase Remote Config

- 關於頁的回饋表單使用 Remote Config 參數 `feedback_form_url`；只接受 `https://forms.gle` 或 `https://docs.google.com/forms/` 網址。
- Remote Config 的參數可被 App 使用者讀取，只能保存公開設定，不得放入 token、密碼或其他秘密資訊。

## Android 段考資訊變更提醒

- 段考提醒是本機背景功能，不使用 FCM topic，也不要把 session、cookie 或成績送到伺服器。
- 背景檢查由 WorkManager unique periodic work `grade_reminder_poll` 執行，週期為 Android 允許的最短 15 分鐘；即使請使用者忽略電池最佳化，仍不能保證即時執行。
- 開始前必須取得通知權限，並透過 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 請使用者允許忽略電池最佳化；若系統不支援或拋出例外，退到 `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`。未允許時不啟用。
- `GradeReportDiffer` 只比對使用者可見資訊：成績、排名、平均、五標、標準差、級距與缺考/作弊等狀態；不要把 `rawResult`、HTTP 格式差異或 `StudentInfo.updatedAt` 納入通知觸發條件。
- 五標/分布或整體成績區塊的新增、移除也屬於可見資訊變更，需產生 diff；不要只比較兩邊都存在的欄位。
- 啟用段考提醒成功後，`SessionStore.saveReminderSession(...)` 會保存一份提醒專用臨時 session，供生物識別解鎖清除一般 session 後的背景 worker 使用；它必須只跟隨提醒狀態存在，並在 48 小時到期、關閉提醒、登出、學生切換或連續失敗停止時清除。
- `GradeReminderWorker` 只能使用提醒專用 session，不得 fallback 到一般 session。登入失效時停止提醒；連線逾時、HTTP 408/429/5xx 等暫時性錯誤交給 WorkManager retry；其他錯誤才累計連續失敗次數。
- 段考提醒狀態與通知不得直接顯示 exception message、URL、response body、學號、cookie 或 token；使用固定且不含敏感資訊的使用者文案。
- 停止或過期段考提醒時，除了取消 WorkManager 與清除 reminder session，也要清掉 reminder snapshot 與 latest change set，避免本機殘留舊成績或舊通知明細。
- App 啟動後若 DataStore 仍有未過期的提醒狀態，`ScoreViewModel` 會補排 `grade_reminder_poll` 作為自我修復；避免在每次 worker 更新 state 時重複 reschedule。
- 段考提醒 channel `grade_reminders` 預設使用 `IMPORTANCE_HIGH`，讓新資訊通知有機會 heads-up 彈出；已安裝 App 的既有 channel 可能仍需使用者到系統設定手動調整。
- 開發者選項內的「段考提醒測試通知」只應在 debug build 顯示，用來測試正式通知 channel 與通知文案；不要把它當成正式背景檢查或 release 使用者功能。
- `src/debug` 的 `GradeReminderDebugReceiver` 只供 ADB 測完整 worker 鏈：它會把目前提醒 state 的上一版 snapshot 改舊，再 enqueue 真正的 `GradeReminderWorker`；不得移到 main/release，也不得改成會外送成績資料。

## Android 課表

- 課表 grid 預設顯示既有 1–8 節；若 API 實際回傳其他正節次，需以稀疏節次清單追加顯示，避免靜默漏課或為異常大節次建立巨大連續範圍。

## Android 學校最新消息

- 學校最新消息是公開資料，列表與詳情只能使用 `NetworkSchoolAnnouncementsRepository` 的無 Cookie client；不得讀取或回退到校務系統、一般登入、生物識別或提醒專用 session。
- 公告列表使用 Material 3 Expressive 下拉重新整理，不顯示來源摘要卡、獨立重新整理按鈕或更新時間。
- 列表直接查詢校網首頁消息，表單參數 `flock` 必須保持空字串；回應陣列第一筆是分頁 metadata，其餘才是消息，第一頁會寫入 App cache 供離線備援。
- 詳情先從 `show.php?nid=...` 擷取 `g_news_unique_id`，再查詢 content endpoint。HTML 必須先清除主動內容與危險 URL scheme；只有 HTTPS 的 `www.clhs.tyc.edu.tw` 圖片可自動載入，其他連結需由使用者主動開啟。
- `SchoolAnnouncementsTest` 固定公開 API 的列表、詳情與附件契約；CI 不直接依賴校網即時回應。

## Android Widget

- 桌面課表小工具 (`ScheduleWidget`) 使用 Jetpack Glance 實作。
- 自動更新依賴 `AlarmManager.setAndAllowWhileIdle` (`WidgetUpdateReceiver`)，在每日午夜與每節課下課時觸發更新，避開了需申請 `SCHEDULE_EXACT_ALARM` 權限的限制。
- 有 Widget 實例時，系統會在開機流程重新觸發 `ScheduleWidgetReceiver.onEnabled()` 排程更新；不要另註冊 `BOOT_COMPLETED`，避免沒有 Widget 的使用者也喚醒 App。
- 測試 Widget UI 時，注意 Glance 的 RemoteViews 資源回收問題：所有動態修飾 (`GlanceModifier`)，包括 `background` 或 `cornerRadius`，在條件分支 (`if-else`) 中都必須明確設置（例如重設為 `Color.Transparent` 與 `0.dp`），否則滑動列表時樣式會錯誤殘留。
- 從 Widget 或 `scoreapp://schedule` deep link 進入 app 時，不得繞過生物識別鎖；若存在 biometric session，`MainActivity` 必須先顯示 `BiometricLockScreen`。課表頁網路 repository 要優先使用已解鎖的 in-memory active session；存在 biometric session 時不得 fallback 到一般 `SessionStore`，避免繞過鎖或誤顯示未登入。
- Widget 本體不得讀取一般 session、biometric session、cookie 或 token；只能讀 `GradeCacheStore` 的 widget 專用課表快照。課表查詢成功或從舊的學生課表快取載入成功時，要同步寫入 widget 快照；登出、學生快取清除或生物識別資料失效時要清掉該快照並刷新 widget。
- `ArchitectureBoundaryTest` 會防止 Widget 重新依賴登入狀態，並檢查 PIN 解鎖必須先 activate in-memory session 再解除鎖定；修改 widget、課表或生物識別流程時要保留這些邊界。
- Widget 的設定由 Android 原生的 Widget 配置活動（Configuration Activity）即 `WidgetConfigurationActivity` 進行。它支援在新增 Widget 時跳出設定，且在 `schedule_widget_info.xml` 宣告為 `widgetFeatures="reconfigurable"`，使得使用者長按 Widget 時可以重新設定。教師、地點、時間與下課後切換偏好直接保存在個別 `GlanceId` 的 state；`syncAllScheduleWidgets(...)` 只能同步共用課表快照與 theme state，不得覆蓋個別 Widget 偏好。
- Widget 預設在午夜隨日期切換課表；使用者也可改為最後一節下課後切換到下一個上課日。顯示目標日期仍需通過本週課表有效範圍檢查。
- Widget 的「上課中」判定使用含開始、不含結束的時間區間，才能和每節下課後延遲 5 秒觸發的更新 Alarm 一致；不要改回包含結束分鐘，否則會在下課後持續顯示上課中直到下一次 Alarm。
- Widget 新增時仍以 4×3 為預設尺寸，但 `schedule_widget_info.xml` 透過 `minResizeWidth` / `minResizeHeight` 允許縮小至約 3×2。
- Widget 頂層版面使用 Glance 官方 `Scaffold`，由系統提供背景圓角；Widget 選擇器在 Android 15 以上使用 generated preview，舊版使用同步維護的 `previewImage`。
- `schedule_widget_preview.png` 不得手工或用生成式圖片工具修改；連接一台 Android 裝置後執行 `python android/scripts/generate_schedule_widget_preview.py`，由 `ScheduleWidget.providePreview` 的實際 Glance `RemoteViews` 產生 552×406 PNG。
- Widget 以 `LocalSize.current` 響應尺寸：高度低於 `160dp` 時只顯示上課中或下一堂課，寬度低於 `220dp` 時隱藏教師、地點與週次；高度達 `260dp` 才在底部加入低對比的已下課區塊。無法判定時間的節次必須保留在接下來清單。
- Widget 設定頁預覽使用相同的課表快照、時間分類、狀態文案與該 Widget 的實際尺寸；調整 Widget 版面時需同步更新預覽。
- 舊版儲存在 `GradeCacheStore` 的全域 Widget 顯示偏好只用來補上尚未有個別 Glance state 的既有 Widget；同步時不得覆蓋已存在的個別 Widget 偏好。
- 課表的「週課表」模式會先呼叫 `GetWeekNoList`，依手機本地日期落入的 `StartDateDisplay` / `EndDateDisplay` 範圍選出唯一 `WeekNo`，不得依賴校方可能延遲切換的 `Selected` / `Item.IsSelected`。本週實際最後一堂課下課後自動刷新時，目標日期使用 `weekStartDate + 1 週`；找不到唯一週次或後續查詢失敗時退回學期課表並提示使用者。
- App 進入課表頁時，本週課表快取只有在整週實際最後一堂課下課後才自動重新查詢，不得把通常落在星期六的 `weekEndDate` 當成刷新門檻；最後上課日有無法辨識的節次時以 16:55 為備援。門檻前直接使用快取，不得每次進頁都重複呼叫課表 API。
- 手動查詢若先取得已達刷新門檻的本週課表，畫面仍可顯示該結果，但後續重新整理必須改用 `weekStartDate + 1 週` 作為目標日期，不得持續重查同一個已過期週次。
- 週課表成功後會再查一次學期課表，以星期與節次為鍵，比對科目、教師及教室；`ScheduleItem`、課表快取與差異資料都不得保存未使用的 API `rawData`。比較失敗時仍顯示週課表並提示使用者。
- Widget 只同步 App 已查詢的本週課表快照；跨出該週日期範圍後必須顯示過期提示並引導開啟 App 更新，不得繼續顯示上週課表，也不得自行讀取登入狀態向學校 API 查詢。
- App 在前一週最後一堂課下課後可能已同步下一週快照；Widget 與設定預覽選擇顯示日期時不得早於該快照的 `weekStartDate`，避免把下週同星期的課誤當成今天並顯示過期。
- Widget 不執行週課表差異比較，也不顯示調課摘要；調課提醒只在使用者開啟 App 查詢週課表時產生。

## Android 成績匯出

- 設定頁的「匯出成績」使用 `GradeExporter`（`data/GradeExporter.kt`）產生 BOM+UTF-8 CSV，透過 `MediaStore` API 存到 Downloads。
- 匯出流程由 `ScoreViewModel.exportGrades()` 驅動，支援跨學期多考試批次匯出；未快取的考試會自動從網路拉取。
- 考試勾選 UI 在 `ui/ExportDialog.kt`，依學期分組並預設全選。

## Android 生物識別防護鎖 (Biometric Lock)

- 生物識別解鎖採用**雙層分層加密 (Key Wrapping) 模式**與硬體密鑰綁定（`setUserAuthenticationRequired(true)` 且 `setInvalidatedByBiometricEnrollment(true)`）。
- **資料層加密 (Session)**：使用使用者設定的 4~6 位數備用 PIN 碼，透過 PBKDF2 衍生出對稱金鑰來加密 Session 資料。
- **密碼層加密 (PIN)**：將使用者的 PIN 碼，使用 Android Keystore 中與生物識別綁定的硬體金鑰進行加密儲存。
- **解鎖流程**：指紋驗證成功 -> 硬體金鑰解密出 PIN 碼 -> 使用 PIN 碼衍生金鑰解密出 Session -> 解鎖成功。若使用者選擇密碼解鎖，則直接使用輸入的 PIN 碼解密 Session。
- **密鑰失效與重新註冊**：若使用者在系統增刪指紋，解密金鑰會失效並拋出 `KeyPermanentlyInvalidatedException`，App 將會捕獲該異常，提示使用者生物特徵已變更，並自動切換為備用 PIN 碼解鎖流程。驗證 PIN 碼成功解密 Session 後，會自動重新呼叫 `BiometricPrompt` 進行新硬體金鑰的綁定與 PIN 碼重新加密。
- **冷啟動與背景鎖定**：利用 `DefaultLifecycleObserver` 監聽 App 生命週期。當 App 冷啟動或從背景喚醒時，若開啟了生物識別，會將 App 鎖定（`isAppLocked = true`）並顯示 `BiometricLockScreen` 覆蓋層以防洩漏隱私。解鎖後的 Session 絕不寫回硬碟的普通明文儲存，以維持最高安全性。
- `BiometricPrompt` 顯示期間可能造成 Activity lifecycle 變化；不得把 prompt 覆蓋造成的 `onStop/onStart` 當作真正背景回來。`MainActivity` 必須用 single-flight 狀態避免重複呼叫 `authenticate(...)`，並在 prompt 顯示期間暫停背景鎖定判斷，避免從 Widget/deep link 進入時連續要求兩次解鎖。
- **多工頁面防護**：只要生物識別已啟用、存在生物識別 session，或 App 正在鎖定狀態，`MainActivity` 會套用 `WindowManager.LayoutParams.FLAG_SECURE`，讓系統多工縮圖與截圖/錄影無法顯示成績畫面；關閉生物識別後才移除此 flag。

## Android Session 加密儲存

- 一般與段考提醒 session 先由 `SessionSerializer` 序列化，再由 `SessionCipher` 使用 Android Keystore 的 AES-256-GCM 金鑰加密；Proto DataStore `session_storage.pb` 只能保存 payload/key version、IV 與 ciphertext。
- 一般與提醒 session 共用 application-wide、版本化的 `clhs_session_key_v*`，但必須分別使用穩定且不同的 AAD。解密失敗不得退回明文或其他 session 類型。
- `SessionStore` 的 DataStore API 是 suspend；caller 不得用 `runBlocking` 包裝。登入、登出、提醒與 migration 的寫入須保留原子更新及 generation guard，避免 clear 後舊 coroutine 把 session 寫回。
- `EncryptedSharedPreferencesLegacySessionSource` 是唯一允許依賴 deprecated AndroidX Security Crypto 的位置，而且只讀取舊版資料。新 payload 寫入並重新解密核對成功前不得清除 legacy keys。
- 生物識別仍採 PIN-derived session key + biometric-bound PIN wrapping。`score_biometric_session.xml` 只能保存已加密的 session/PIN ciphertext、IV 與 salt。
- `session_storage.pb`、`score_biometric_session.xml` 與 transition 期間的 `score_session.xml` 都必須排除 cloud backup 與 device transfer；Manifest 仍維持 `allowBackup=false`。

## Android 校務行事曆

- 校務行事曆只讀取 `SchoolCalendar.kt` 內固定的學校公開 Google Calendar ICS，不得使用登入 session、cookie、`SchoolGradeClient` 或校務系統 API；專用 OkHttp client 必須保留 `CookieJar.NO_COOKIES`。
- ICS 使用 `biweekly` 解析，Gradle 必須排除非必要的 Jackson；開源授權頁需保留 biweekly、Vinnie 與其內嵌 Apache 元件的逐項授權。
- ICS 最多下載 5 MiB，快取於 `cacheDir` 六小時；更新失敗時可顯示舊快取，但不得把原始網路錯誤或回應內容顯示給使用者。
- 行事曆議程使用 Material 3 Expressive 下拉重新整理，不顯示來源摘要卡、獨立重新整理按鈕或更新時間；離開清單頂端且停止滑動後，以淡入與垂直位移顯示具無障礙描述的「回到頂端」浮動圖示按鈕。AnimatedVisibility 內需保留 12dp 陰影空間，避免 elevation 陰影在轉場結束時跳動。
- 漢堡選單問候卡下方集中放置校務系統、學校公告與校務行事曆三個圖示入口；公告功能完成前維持 disabled。行事曆圖示疊加手機當日日期，設定頁與「更多」頁不保留重複入口。
- 目前來源沒有週期活動，若日後出現 RRULE，App 先略過並顯示提示；只有來源實際開始使用時，才加入有日期範圍上限的 recurrence 展開。
