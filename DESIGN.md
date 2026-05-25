---
version: alpha
name: CLHS Score
description: Design system for the CLHS Score web app, Android app, and demo site.
colors:
  primary: "#1565C0"
  primaryDark: "#82B1FF"
  primaryWeb: "#2D5DAA"
  primaryDemo: "#4A9EFF"
  onPrimary: "#FFFFFF"
  primaryContainer: "#BBDEFB"
  onPrimaryContainer: "#002171"
  secondary: "#4F6354"
  secondaryDark: "#B6CCBB"
  secondaryContainer: "#D2E8D7"
  tertiary: "#33618D"
  success: "#126738"
  successDark: "#81C995"
  warning: "#946C00"
  warningDark: "#FDE293"
  danger: "#BA1A1A"
  dangerDark: "#FFB4AB"
  backgroundLight: "#FBFCFF"
  backgroundDark: "#121212"
  backgroundWebDark: "#111318"
  demoBackground: "#0A0A0F"
  surfaceLight: "#FBFCFF"
  surfaceDark: "#121212"
  surfaceWebDark: "#111318"
  surfaceElevatedLight: "#FFFFFF"
  surfaceElevatedDark: "#1E2025"
  surfaceContainerLight: "#EEF2F6"
  surfaceContainerDark: "#1E2225"
  surfaceVariantLight: "#DDE3EA"
  surfaceVariantDark: "#43474E"
  textLight: "#1A1C1E"
  textDark: "#E2E2E6"
  textWebDark: "#E1E2E8"
  textSecondaryLight: "#43474E"
  textSecondaryDark: "#C3C7CF"
  outlineLight: "#73777F"
  outlineDark: "#8D9199"
  outlineVariantLight: "#C3C7CF"
  outlineVariantDark: "#43474E"
typography:
  display:
    fontFamily: Roboto
    fontSize: 3rem
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: 0
  title-lg:
    fontFamily: Roboto
    fontSize: 1.375rem
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: 0
  title-md:
    fontFamily: Roboto
    fontSize: 1.125rem
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: 0
  body:
    fontFamily: Roboto
    fontSize: 1rem
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0
  body-zh:
    fontFamily: Noto Sans TC
    fontSize: 1rem
    fontWeight: 400
    lineHeight: 1.65
    letterSpacing: 0
  label:
    fontFamily: Roboto
    fontSize: 0.8125rem
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: 0
  numeric:
    fontFamily: Roboto
    fontSize: 1rem
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: 0
    fontFeature: tnum
rounded:
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  pill: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  section: 64px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: 14px
  button-primary-hover:
    backgroundColor: "{colors.primaryWeb}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.md}"
  card:
    backgroundColor: "{colors.surfaceContainerLight}"
    textColor: "{colors.textLight}"
    rounded: "{rounded.md}"
    padding: 16px
  card-dark:
    backgroundColor: "{colors.surfaceContainerDark}"
    textColor: "{colors.textDark}"
    rounded: "{rounded.md}"
    padding: 16px
  hero-card:
    backgroundColor: "{colors.primaryContainer}"
    textColor: "{colors.onPrimaryContainer}"
    rounded: "{rounded.lg}"
    padding: 24px
  chip:
    backgroundColor: "{colors.surfaceVariantLight}"
    textColor: "{colors.textSecondaryLight}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: 8px
  chart-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.onPrimary}"
    rounded: "{rounded.sm}"
    size: 12px
---

## Overview

壢中成績的介面是「安靜、可信、可重複使用的成績工具」，不是行銷式儀表板。設計重點是讓學生快速確認平均、排名、科目落點與趨勢，因此資訊層級、數字可讀性和狀態辨識要優先於裝飾。

Web 與 Android app 採 Material 3 語彙：藍色作為主要行動與資料焦點，綠色處理成長/優勢，黃橘色處理提醒，紅色只用於錯誤或低分風險。Demo site 可以更有展示感，但仍要延續深色、藍色焦點與清楚的 app icon 訊號。

## Colors

色彩以 Material 3 的 light/dark scheme 為核心。Android 的 `ScoreTheme.kt` 是 app 端主要來源；Web 的 `web/frontend/styles/tokens.css` 是網頁端主要來源。

- **Primary** 用於主要 CTA、選取狀態、目前資料點、進度與重點數字。
- **Surface** 用於卡片、底部導覽、彈窗與表格容器；不要把大面積內容做成過亮或過飽和。
- **Success / warning / danger** 只表達成績語意或系統狀態，不應當作裝飾色。
- **Demo background** 可以使用接近黑色的深色背景與白色粒子，但粒子不可遮擋文字或 icon。

## Typography

Android 使用 Material 3 預設 Roboto，Web 使用 `Roboto` + `Noto Sans TC`，Demo 使用 `Outfit` + `Noto Sans TC`。新介面若未明確屬於 demo，預設使用 Roboto / Noto Sans TC。

數字欄位、平均、排名、百分比、分數差距必須使用 tabular numbers 或等寬數字特性，避免資料更新時水平跳動。中文說明文字使用較寬鬆 line-height，標題保持短句，不做過度行銷化文案。

## Layout

畫面應以「總覽 → 排名/平均 → 科目細節 → 圖表/趨勢 → 操作」的閱讀順序組織。手機 app 優先單欄、卡片堆疊；Web 可使用兩欄或卡片 grid，但不要讓同一層級資訊密度過高。

常用間距以 8px 系列建立：8px 用於小元素內距，16px 用於卡片內容，24px 用於卡片群組，32px 以上用於區塊分隔。主要內容容器在 Web 上維持可掃讀寬度，避免滿版拉長成績表格。

## Elevation & Depth

深色模式偏向低陰影、靠 surface 色階與細邊框建立層級。Light mode 可以使用柔和陰影，但卡片不應浮得太重。Demo 首屏可使用 glow、粒子與 icon shadow，但產品內頁要避免大面積裝飾光暈。

互動狀態以 subtle background、outline、primary tint 或 1-2px 位移呈現；不要用大幅縮放造成資料版面跳動。

## Shapes

標準卡片使用 16px 圓角，重要摘要卡可用 24px。小標籤、分布條、chip 使用 pill 或 9999px。App icon 保持圓角方形，不要把產品 UI 裡的資料卡做成過度有機或不規則形狀。

圖表中的 bar、legend marker、distribution segment 可用 4-12px 小圓角；表格列與資料行保持穩定高度，避免 hover 改變布局。

## Components

Primary button 用於登入、同步、下載、重新載入與主要確認動作；同一畫面只保留一個最強 primary CTA。次要動作使用 tonal、outline 或 text button。

成績卡應包含科目名稱、分數、班平均/標準差等必要上下文；高低分顏色要搭配文字標籤，不可只靠顏色傳達。排名與百分比要同時顯示目前值與總數或 PR 說明。

圖表卡要優先讓資料可讀：清楚 legend、足夠 label 空間、淡色 grid、強調我的成績與班級平均。Radar、bar、distribution chart 應共享同一套 primary/outline/semantic colors。

Modal 用於登入、分享、免責與確認流程。Modal 文字要短，表單錯誤用 inline feedback，避免清空使用者已輸入內容。

## Do's and Don'ts

Do:
- 保持工具感：資訊密度可以高，但層級要清楚。
- 使用 Material 3 color roles 和既有 tokens，不臨時創造近似藍色。
- 讓平均、排名、百分比等數字對齊且容易比較。
- 在 demo 或行銷頁可加入動態背景，但互動效果必須在內容層之後。

Don't:
- 不要把學生資料、帳密、token 或正式環境 secret 寫進 UI、文件範例或截圖。
- 不要用單一顏色家族鋪滿整個 app；藍色是焦點，不是背景全部。
- 不要在產品內頁使用大 hero、過多裝飾卡片或阻礙掃讀的動畫。
- 不要只用顏色表示成績好壞；必須保留文字或數字上下文。
