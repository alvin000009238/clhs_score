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

## Android UI fake data

- UI 展示資料集中在 `android/app/src/main/java/com/clhs/score/data/FakeData.kt`；新增畫面預覽或假資料情境時優先擴充這裡，不要在 Composable 內臨時硬編資料。
- Android app 可用 `-PuseFakeData=true` 切到 `FakeGradeRepository`，讓登入後成績列表、平均、班排、各科分析、圖表與模擬器都不依賴 API。
- Compose Preview 入口在 `android/app/src/main/java/com/clhs/score/ui/ScorePreviews.kt`，應直接使用 `FakeData` 組 `GradesUiState`。

## Android Material Symbols subset

- Material Symbols rounded icon 由 `android/app/src/main/res/font/material_symbols_rounded_*_subset.ttf` 提供，不要重新加入 `dev.vicart:compose-material-symbols` 整包依賴。
- 新增 icon ligature 時，先更新 `android/scripts/generate_material_symbol_subset.py` 的 `ICONS` 清單，再執行 `python android/scripts/generate_material_symbol_subset.py` 重新產生 outline / filled subset font。

## Android R8 與安裝包大小

- Release build 已啟用 `isMinifyEnabled` 與 `isShrinkResources`；新增 library 或功能時不要用 `-keep class androidx.**`、`-keep class org.jsoup.**` 這類 broad keep 擋住 R8。優先依賴 library 自帶的 consumer rules，只針對 app 端需要反射或跨版本保留名稱的入口加最小規則，例如 WorkManager worker class name，並用 `:app:assembleRelease` 比對 APK 大小。

## Android FCM notifications

- Android app 使用 Firebase Cloud Messaging 接收手動推播；目前發送端預設是 Firebase Console，不需要把 FCM server key、service account 或其他私鑰放進 app。
- `android/app/google-services.json` 是 Firebase app 設定檔，需保留在 app module 根目錄並允許進版控；不要提交 Firebase service account JSON。
- 使用者在設定頁開啟通知後，app 會訂閱 `general` 與 `app_updates` topics；關閉通知時會取消訂閱。
- `POST_NOTIFICATIONS` 權限不要用 Compose `ActivityResultContracts.RequestPermission()` 直接請求；此 app 使用 `FragmentActivity`，實機曾因 requestCode 超過 lower 16 bits 閃退。改開 App 通知設定頁並在返回 App 時檢查權限。
- 系統通知權限與 App 內通知開關的同步由 `ScoreApp` 根層處理；不要只放在設定頁，否則使用者從系統設定封鎖通知後，其他入口回 App 時狀態會不一致。
- 發送 app 更新通知時使用 `app_updates` topic，一般公告使用 `general` topic。可在 FCM data payload 帶 `url`，使用者點通知時會開啟該網址。

## Android Firebase Analytics

- Firebase Analytics 事件經由 `com.clhs.score.analytics.AnalyticsLogger` 與 `FirebaseAnalyticsLogger` 集中記錄；不要在 UI、ViewModel 或 service 內直接呼叫 Firebase `logEvent`，避免事件名稱與隱私規則分散。
- Analytics 採嚴格匿名策略：不得呼叫 `setUserId`，不得送學號、姓名、班級、座號、成績、排名、科目名稱、考試名稱、URL、cookie、token、rawResult 或錯誤原文。事件參數只允許 enum 字串、布林、計數與 bucket。
- 新增事件或參數時，先更新 `AnalyticsEvents.kt` / `AnalyticsParameterSanitizer.kt` 的常數與白名單，並補 `AnalyticsParameterSanitizerTest` 或 `ArchitectureBoundaryTest`，確保敏感欄位不會被送出。

## Android 段考資訊變更提醒

- 段考提醒是本機背景功能，不使用 FCM topic，也不要把 session、cookie 或成績送到伺服器。
- 背景檢查由 WorkManager unique periodic work `grade_reminder_poll` 執行，週期為 Android 允許的最短 15 分鐘；即使請使用者忽略電池最佳化，仍不能保證即時執行。
- 開始前必須取得通知權限，並透過 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 請使用者允許忽略電池最佳化；若系統不支援或拋出例外，退到 `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`。未允許時不啟用。
- `GradeReportDiffer` 只比對使用者可見資訊：成績、排名、平均、五標、標準差、級距與缺考/作弊等狀態；不要把 `rawResult`、HTTP 格式差異或 `StudentInfo.updatedAt` 納入通知觸發條件。
- 五標/分布或整體成績區塊的新增、移除也屬於可見資訊變更，需產生 diff；不要只比較兩邊都存在的欄位。
- 啟用段考提醒成功後，`SessionStore.saveReminderSession(...)` 會保存一份提醒專用臨時 session，供生物識別解鎖清除一般 session 後的背景 worker 使用；它必須只跟隨提醒狀態存在，並在 48 小時到期、關閉提醒、登出、學生切換或連續失敗停止時清除。
- 停止或過期段考提醒時，除了取消 WorkManager 與清除 reminder session，也要清掉 reminder snapshot 與 latest change set，避免本機殘留舊成績或舊通知明細。
- App 啟動後若 DataStore 仍有未過期的提醒狀態，`ScoreViewModel` 會補排 `grade_reminder_poll` 作為自我修復；避免在每次 worker 更新 state 時重複 reschedule。
- 段考提醒 channel `grade_reminders` 預設使用 `IMPORTANCE_HIGH`，讓新資訊通知有機會 heads-up 彈出；已安裝 App 的既有 channel 可能仍需使用者到系統設定手動調整。
- 開發者選項內的「段考提醒測試通知」只應在 debug build 顯示，用來測試正式通知 channel 與通知文案；不要把它當成正式背景檢查或 release 使用者功能。
- `src/debug` 的 `GradeReminderDebugReceiver` 只供 ADB 測完整 worker 鏈：它會把目前提醒 state 的上一版 snapshot 改舊，再 enqueue 真正的 `GradeReminderWorker`；不得移到 main/release，也不得改成會外送成績資料。

## Android Widget

- 桌面課表小工具 (`ScheduleWidget`) 使用 Jetpack Glance 實作。
- 自動更新依賴 `AlarmManager.setAndAllowWhileIdle` (`WidgetUpdateReceiver`)，在每日午夜與每節課下課時觸發更新，避開了需申請 `SCHEDULE_EXACT_ALARM` 權限的限制。
- 測試 Widget UI 時，注意 Glance 的 RemoteViews 資源回收問題：所有動態修飾 (`GlanceModifier`)，包括 `background` 或 `cornerRadius`，在條件分支 (`if-else`) 中都必須明確設置（例如重設為 `Color.Transparent` 與 `0.dp`），否則滑動列表時樣式會錯誤殘留。
- 從 Widget 或 `scoreapp://schedule` deep link 進入 app 時，不得繞過生物識別鎖；若存在 biometric session，`MainActivity` 必須先顯示 `BiometricLockScreen`。課表頁網路 repository 要優先使用已解鎖的 in-memory active session；存在 biometric session 時不得 fallback 到一般 `SessionStore`，避免繞過鎖或誤顯示未登入。
- Widget 本體不得讀取一般 session、biometric session、cookie 或 token；只能讀 `GradeCacheStore` 的 widget 專用課表快照。課表查詢成功或從舊的學生課表快取載入成功時，要同步寫入 widget 快照；登出、學生快取清除或生物識別資料失效時要清掉該快照並刷新 widget。
- `ArchitectureBoundaryTest` 會防止 Widget 重新依賴登入狀態，並檢查 PIN 解鎖必須先 activate in-memory session 再解除鎖定；修改 widget、課表或生物識別流程時要保留這些邊界。

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
