# Web 端 — 成績查詢平台

Flask 後端 + Vite 前端的 Web 版成績查詢與分享服務。

## 技術棧

- **後端**：Python 3.11 / Flask / Gunicorn / Redis（session & 分享資料）
- **前端**：Vanilla ES Modules / Vite / Chart.js
- **安全**：Cloudflare Turnstile 人機驗證、HttpOnly Secure Cookie
- **部署**：Docker 多階段建置 / Docker Compose / Cloudflare Tunnel

## 架構

```text
web/
├── app/                  # Flask 後端
│   ├── __init__.py       # App factory (create_app)
│   ├── routes/           # Blueprint: auth / grades / share / system
│   └── services/         # Service layer: auth / grades / share / turnstile / http_client
├── frontend/             # Vite 前端原始碼
│   ├── main.js           # 進入點
│   └── styles/           # 樣式
├── public/               # 靜態入口與 Vite build 輸出
├── fetcher.py            # 學校系統 API adapter (GradeFetcher)
├── server.py             # 本機啟動入口
├── Dockerfile            # 多階段建置 (Node → Python)
├── tests/                # pytest + Node 測試
└── vite.config.js
```

## 系統需求

- [Docker](https://docs.docker.com/get-docker/) 與 [Docker Compose](https://docs.docker.com/compose/install/)（v2+）
- Node.js 20+（僅本地開發前端時需要）
- Python 3.11+（僅本地開發後端時需要）

## 快速開始（Docker Compose）

> [!NOTE]
> 此方法使用根目錄的 `docker-compose.yml` 搭配 `web/Dockerfile`，最接近正式環境。

1. **準備環境變數**

   ```bash
   cp .env.example .env
   ```

   編輯 `.env`，設定 `SECRET_KEY` 等必要變數。本地開發時 Turnstile 可使用測試金鑰。

2. **建置並啟動**

   ```bash
   docker compose up --build -d
   ```

3. **開啟瀏覽器**

   前往 `http://localhost:5000`

4. **查看 Log / 關閉**

   ```bash
   docker compose logs -f app
   docker compose down
   ```

## 測試

在 `web/` 目錄內執行：

```bash
# 後端 pytest
pytest tests/backend/

# Python 語法檢查
python -m compileall app fetcher.py server.py

# 前端測試與建置
npm run test
npm run build
```

## 相關文件

- [專案架構](../docs/ARCHITECTURE.md)
- [Android 版](../android/README.md)
