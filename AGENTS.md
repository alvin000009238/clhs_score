# AGENTS.md

本檔給 AI agent 在此 repo 工作時使用。請先讀實際程式碼再改動，避免依照通用模板猜測架構。

## 專案結構

- `web/`：Web 端工作目錄，包含 Flask 後端、Vite 前端、web 測試、Dockerfile 與 web 專用 scripts。
- `web/app/` 與 `web/server.py`：Flask 後端。`app.create_app()` 是主要 app factory，`server.py` 只負責本機啟動。
- `web/frontend/`：Vite 前端原始碼，進入點是 `web/frontend/main.js`，樣式拆在 `web/frontend/styles/`。
- `web/public/`：Flask 服務的靜態入口與 Vite build 輸出位置。
- `android/`：獨立 Kotlin / Jetpack Compose / Material 3 Android app，package 為 `com.clhs.score`。
- `web/tests/`：後端 pytest 與前端 Node 測試。

## 工作規則

- 優先保留既有資料流程與檔案分層。UI 改版請先找現有 screen、theme、chart 元件，不要另建平行 app。
- 不要把帳密、token、cookie 或正式環境 secret 寫進程式碼或對話；使用 `.env`、local properties 或本機設定檔。
- 此 repo 的 Markdown 預設會被 `.gitignore` 忽略；新增或更新 agent 文件後要確認 `AGENTS.md` 沒有被 ignore。
- 文件預設使用繁體中文；程式碼註解只在能降低理解成本時加入。

## 常用驗證

- Web 後端：在 `web/` 內執行 `pytest tests/backend/`
- Web Python 語法：在 `web/` 內執行 `python -m compileall app fetcher.py server.py`
- Web 前端：在 `web/` 內執行 `npm run test`、`npm run build`
- Android：在 `android/` 內執行 `.\gradlew.bat test`

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
