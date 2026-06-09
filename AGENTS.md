# AGENTS.md

本檔給 AI agent 在此 repo 工作時使用。請先讀實際程式碼再改動，避免依照通用模板猜測架構。

## 專案結構

- `android/`：獨立 Kotlin / Jetpack Compose / Material 3 Android app，package 為 `com.clhs.score`。
- `demo/`：展示或 demo 相關內容。
- `docs/`：Android 與整體專案文件。
- `.github/workflows/`：此 repo 的 Android release、demo deploy 與通用檢查 workflow。

## 已搬出的同層專案

- `..\clhs-score-worker\web`：原 `web/`，Flask 後端、Vite 前端、web 測試、Dockerfile、Docker Compose 與 web 專用 scripts。
- `..\clhs-score-worker`：原 `workers/clhs-score-worker/`，獨立 Cloudflare Workers fullstack web backend nested git repo。

## 工作規則

- 優先保留既有資料流程與檔案分層。UI 改版請先找現有 screen、theme、chart 元件，不要另建平行 app。
- 不要把帳密、token、cookie 或正式環境 secret 寫進程式碼或對話；使用 `.env`、local properties 或本機設定檔。
- 此 repo 的 Markdown 預設會被 `.gitignore` 忽略；新增或更新 agent 文件後要確認 `AGENTS.md` 沒有被 ignore。
- 文件預設使用繁體中文；程式碼註解只在能降低理解成本時加入。

## 常用驗證

- Android：在 `android/` 內執行 `.\gradlew.bat test`
- Web：切到同層 `..\clhs-score-worker\web` 後執行該專案的 `AGENTS.md` 驗證
- Cloudflare Worker：切到同層 `..\clhs-score-worker` 後執行該專案的 `AGENTS.md` 驗證

在 Windows Codex 環境跑 Android Gradle 時，若 `java` 不在 PATH，使用 Android Studio 內建 JBR，並將 `GRADLE_USER_HOME`、`ANDROID_USER_HOME` 指到 workspace 內的暫存目錄。若測試一開始就出現 `could not open ...\jbr\lib\jvm.cfg`，通常是設定的 Android Studio JBR 路徑不存在或不完整；先用 `Test-Path` 或列出 `C:\Program Files\Android\Android Studio*` 確認實際 JBR 位置。本機曾遇到 `C:\Program Files\Android\Android Studio\jbr` 不可用，而 `C:\Program Files\Android\Android Studio1\jbr` 可用。

## Android release

- 推送 `v*` tag 會觸發 `.github/workflows/android-release.yml`，建立 signed `arm64-v8a` release APK 並發布 GitHub Release。
- Release notes 來自 `CHANGELOG.md` 內與 tag 對應的 `## [x.y.z]` 區塊；新增版本時要先補 changelog。
- `CHANGELOG.md` 的新版本內容必須和上一個版本比較，並比照既有 `v1.1.0` 的分類寫法（Features、Bug Fixes、Performance Improvements），不要混入更早版本已經發布的內容。
- **重要規則**：將更新發布推送至 GitHub 前，必須先將 `CHANGELOG.md` 寫完並請使用者檢查和修改，確認無誤後才能推送。
- **重要規則**：`CHANGELOG.md` 僅用於記錄與 Android app 有關的更新。若僅修改 Web Demo 等非 Android app 相關的程式碼，請勿更動 `CHANGELOG.md`，也不要新增版本號。
- GitHub Secrets 需設定 `ANDROID_RELEASE_KEYSTORE_BASE64`、`ANDROID_RELEASE_KEYSTORE_PASSWORD`、`ANDROID_RELEASE_KEY_ALIAS`、`ANDROID_RELEASE_KEY_PASSWORD`。不要提交 keystore 或密碼。

## Android UI fake data

- UI 展示資料集中在 `android/app/src/main/java/com/clhs/score/data/FakeData.kt`；新增畫面預覽或假資料情境時優先擴充這裡，不要在 Composable 內臨時硬編資料。
- Android app 可用 `-PuseFakeData=true` 切到 `FakeGradeRepository`，讓登入後成績列表、平均、班排、各科分析、圖表與模擬器都不依賴 API。
- Compose Preview 入口在 `android/app/src/main/java/com/clhs/score/ui/ScorePreviews.kt`，應直接使用 `FakeData` 組 `GradesUiState`。

## Android Material Symbols subset

- Material Symbols rounded icon 由 `android/app/src/main/res/font/material_symbols_rounded_*_subset.ttf` 提供，不要重新加入 `dev.vicart:compose-material-symbols` 整包依賴。
- 新增 icon ligature 時，先更新 `android/scripts/generate_material_symbol_subset.py` 的 `ICONS` 清單，再執行 `python android/scripts/generate_material_symbol_subset.py` 重新產生 outline / filled subset font。

## Android FCM notifications

- Android app 使用 Firebase Cloud Messaging 接收手動推播；目前發送端預設是 Firebase Console，不需要把 FCM server key、service account 或其他私鑰放進 app。
- `android/app/google-services.json` 是 Firebase app 設定檔，需保留在 app module 根目錄並允許進版控；不要提交 Firebase service account JSON。
- 使用者在設定頁開啟通知後，app 會訂閱 `general` 與 `app_updates` topics；關閉通知時會取消訂閱。
- 發送 app 更新通知時使用 `app_updates` topic，一般公告使用 `general` topic。可在 FCM data payload 帶 `url`，使用者點通知時會開啟該網址。

## Android Widget

- 桌面課表小工具 (`ScheduleWidget`) 使用 Jetpack Glance 實作。
- 自動更新依賴 `AlarmManager.setAndAllowWhileIdle` (`WidgetUpdateReceiver`)，在每日午夜與每節課下課時觸發更新，避開了需申請 `SCHEDULE_EXACT_ALARM` 權限的限制。
- 測試 Widget UI 時，注意 Glance 的 RemoteViews 資源回收問題：所有動態修飾 (`GlanceModifier`)，包括 `background` 或 `cornerRadius`，在條件分支 (`if-else`) 中都必須明確設置（例如重設為 `Color.Transparent` 與 `0.dp`），否則滑動列表時樣式會錯誤殘留。

## Android 成績匯出

- 設定頁的「匯出成績」使用 `GradeExporter`（`data/GradeExporter.kt`）產生 BOM+UTF-8 CSV，透過 `MediaStore` API 存到 Downloads。
- 匯出流程由 `ScoreViewModel.exportGrades()` 驅動，支援跨學期多考試批次匯出；未快取的考試會自動從網路拉取。
- 考試勾選 UI 在 `ui/ExportDialog.kt`，依學期分組並預設全選。

## 搬出專案提醒

- `web/` 已搬到同層 `..\clhs-score-worker\web`，此 repo 不再放 Flask/Vite web 程式碼。
- `workers/clhs-score-worker/` 已搬到同層 `..\clhs-score-worker`，Worker 相關提交應在該獨立 repo 內處理。