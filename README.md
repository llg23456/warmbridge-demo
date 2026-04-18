# 暖桥 WarmBridge · Demo

面向「家长—子女」代际沟通的演示应用：**子女分享资讯、家长用大字号界面浏览**，并可一键 **AI 讲给长辈听**（服务端调用 vivo **蓝心 Chat** 生成通俗解读）。工程用于应用赛道 / MVP 演示，含 **Android 客户端** 与 **FastAPI 后端（BFF）**。

---

## 功能一览

| 角色 | 能力 |
| --- | --- |
| **家长** | 选择兴趣标签浏览热点列表；查看详情；**AI 讲给长辈听**（通俗摘要、背景小知识、词语小抄）；**浏览器打开原文**；查看「孩子推荐」；**温情提醒**（定时通知） |
| **孩子** | **分享链接**（带备注）到后端，家长端「孩子推荐」可见 |
| **年轻人话题** | 单独频道列表（与标签热点数据源一致，演示多入口） |

未配置大模型密钥时，解读接口仍返回 **HTTP 200** 与离线占位文案，便于先调通网络与 UI；是否真正走蓝心以响应字段 **`from_llm: true`** 为准（也可用 `/docs` 调 `POST /api/explain` 查看）。

---

## 技术栈

- **Android**：Kotlin、Jetpack Compose、Material 3（暖色、大字号）、Retrofit；HTTP 明文仅用于 Demo 局域网调试，详见 `android/app/src/main/res/values/dimens.xml` 中的界面基准说明
- **后端**：Python 3、FastAPI、httpx（调用蓝心 Chat Completions）
- **数据**：热点为服务端 **Mock 白名单**（`server/app/services/feed_mock.py`）；分享条目内存存储（重启清空）

---

## 仓库结构

```
warmbridge-demo/
├── android/                 # Android Studio 工程（模块根目录）
│   ├── app/src/main/... # 界面、主题、网络层、Worker 等
│   └── local.properties     # 本地生成：SDK 路径、API 根地址（勿提交 Git）
├── server/
│   ├── .env                 # 本地配置：VIVO_APP_KEY 等（勿提交 Git）
│   ├── .env.example         # 变量模板，可复制为 .env
│   ├── app/
│   │   ├── main.py          # FastAPI 入口
│   │   ├── routers/         # /api/feed、/api/explain、/api/share 等
│   │   ├── services/        # Mock  feed、vivo_llm、内存 store
│   │   └── config.py        # 读取 server/.env
│   └── requirements.txt
├── 联调问题报告.md          # 明文 HTTP、IP、explain 缓存等排障
├── GitHub上传指南.md        # 协作、.gitignore、内网 baseUrl 说明
└── README.md                # 本文件
```
（若赛事方另发了 **蓝心接口速查**等文档，请自行对照模型名与权限，不必放进本仓库。）

---

## 从零开始配置并跑起来

以下默认你在本机 **同时跑后端 + 编译 App**；手机与电脑需 **同一局域网（如同一 Wi‑Fi）**。

### 0. 准备环境

- 安装 **Python 3.10+**、**Git**（若从仓库克隆）
- 安装 **Android Studio**，并装好 **Android SDK**、一台 **真机或模拟器**
- 关闭或放行本机防火墙对 **8000 端口** 的入站（真机访问 Windows 开发机时常见）

### 1. 后端：虚拟依赖与启动

在仓库中的 **`server`** 目录执行：

**Windows（PowerShell / CMD）：**

```bat
cd server
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
```

**macOS / Linux：**

```bash
cd server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

用编辑器打开 **`server/.env`**，先填 **`VIVO_APP_KEY=`**（赛事/官网下发的 AppKey 整行粘贴即可；**末尾 `=` / `==` 不要删**）。可选：`VIVO_CHAT_MODEL` 等与 `.env.example` 说明一致。

启动 API（**必须**监听 `0.0.0.0`，否则真机访问不到）：

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

浏览器打开 [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs) 可调试接口。

### 2. 确认局域网地址

在 **运行后端的电脑** 上查看 IPv4，例如 Windows 执行 `ipconfig`，取当前上网网卡（如 **WLAN**）的地址，记为 `YOUR_IP`（如 `192.168.1.5`）。

用 **手机浏览器** 访问（把 `YOUR_IP` 换成上一步的地址）：

- `http://YOUR_IP:8000/health` → 应返回类似 `{"status":"ok"}`
- `http://YOUR_IP:8000/api/tags` → 应返回 JSON

若手机打不开，先检查 **同一 Wi‑Fi**、**防火墙**、**IP 是否选对**，再往下配 App。

### 3. Android：配置 API 根地址

用 **Android Studio** 打开 **`warmbridge-demo/android`**（打开到含 `settings.gradle.kts` 的那一层）。

编辑 **`android/local.properties`**（与 `app` 文件夹同级；若不存在可新建），增加一行，**末尾斜杠必填**：

```properties
warmbridge.api.baseUrl=http://YOUR_IP:8000/
```

将 `YOUR_IP` 换成第 2 步里手机浏览器已验证能访问的 IP。

然后：**File → Sync Project with Gradle Files**，**Build → Rebuild Project**。真机建议 **卸载旧 App 再安装**，避免仍使用旧的 `BuildConfig.API_BASE_URL`。

**官方模拟器、且后端在本机**：可不写 `warmbridge.api.baseUrl`，工程默认使用 **`http://10.0.2.2:8000/`**（模拟器访问宿主机）。

### 4. 验证密钥是否被后端加载

在已 **activate** 的 `server` 虚拟环境下执行：

```bash
python -c "from app.config import settings; print('VIVO_APP_KEY 已加载:', bool(settings.vivo_app_key.strip()))"
```

- `True`：密钥非空；若 App 里仍像离线，多为 **蓝心接口报错**，请看详情页「背景小知识」或 `/docs` 里 `POST /api/explain` 返回的 `background`、`from_llm`。
- `False`：检查 `.env` 是否在 **`server/`** 下、键名是否为 **`VIVO_APP_KEY`**、保存后是否 **重启过 uvicorn**。

### 5. 运行 App

在 Android Studio 中选择设备，运行 **`app`**。建议体验路径：选角色 → 家长：选标签 → 进列表 → 详情 → **AI 讲给长辈听**；孩子端：**分享链接**后回到家长端「孩子推荐」查看。

---

## 环境变量说明（服务端）

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `VIVO_APP_KEY` | 使用真模型时必填 | `Authorization: Bearer <AppKey>` |
| `VIVO_CHAT_URL` | 否 | 默认官方 Chat Completions 地址 |
| `VIVO_CHAT_MODEL` | 否 | 须与账号开通的模型一致 |
| `VIVO_APP_ID` | 否 | 预留 |

详情见 **`server/.env.example`**；模型名与权限以赛事 / 官方接口文档为准。

---

## 常见问题（精简）

| 现象 | 处理 |
| --- | --- |
| App 连不上、Logcat 含 `CLEARTEXT ... not permitted` | 本 Demo 已放行 HTTP；仍报错请 **Clean/Rebuild** 并重装 App |
| 浏览器能开 `/health`，App 不行 | **`local.properties` 的 IP 与浏览器不一致** 或 **未 Sync/重装** |
| 配了 Key 仍像离线 | 看 **`from_llm`** 与「背景小知识」；参阅 [联调问题报告.md](联调问题报告.md)（含 explain 缓存说明） |
| 别人 clone 后怎么填 IP | 每人填 **自己电脑** 的局域网 IP，见 [GitHub上传指南.md](GitHub上传指南.md) |

---

## 安全与协作

- **`VIVO_APP_KEY` 仅放在 `server/.env`**，不要写入 Android 工程，不要提交到 Git。
- **`android/local.properties`** 已在 `.gitignore`，每人本地配置。

---

## 更多文档

- [联调问题报告.md](联调问题报告.md) — 网络、明文策略、`/api/explain` 与缓存等  
- [GitHub上传指南.md](GitHub上传指南.md) — 推送仓库、协作者内网与密钥  
- `server/README.md` 为指向本仓库说明的简短入口（若存在）  
