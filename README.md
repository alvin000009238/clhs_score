# 壢中成績 Android — 中大壢中

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/alvin000009238/clhs_score)

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

    %% ===== External Systems =====
    subgraph EXT["External Systems"]
        API["School System API<br/>(shcloud2.k12ea.gov.tw)"]
    end

    %% ===== Data Layer =====
    subgraph DATA["Data Layer (Natural)"]
        CLIENT["SchoolGradeClient<br/>(OkHttp + Jsoup)"]
        REPO["GradeRepository"]
        CACHE["GradeCacheStore<br/>(DataStore)"]

        CLIENT --> REPO
        REPO <--> CACHE
    end

    %% ===== Logic & UI =====
    subgraph UI["Logic & UI (Code Entity Space)"]
        VM["ScoreViewModel"]
        ANALYSIS["GradeAnalysis.kt"]
        SCREEN["Compose UI<br/>(GradesScreen)"]

        VM <--> ANALYSIS
        VM --> SCREEN
    end

    %% ===== Connections =====
    CLIENT <--> |HTTPS/JSON| API
    REPO --> VM
```

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
