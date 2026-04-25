# 暖桥 WarmBridge · Demo

面向「家长—子女」代际沟通的演示应用：**子女分享资讯、家长用大字号界面浏览**，并可 **图片识梗（OCR + 通俗解读）**、**视频链接快解析**、一键 **AI 讲给长辈听**（服务端聚合 vivo **蓝心 Chat**、**通用 OCR §8**、**流式 TTS §14** 等能力）。工程用于应用赛道 / MVP 演示，含 **Android 客户端** 与 **FastAPI 后端（BFF）**。

---

## 功能一览

| 角色 / 入口 | 能力 |
| --- | --- |
| **家长** | 兴趣标签浏览热点；**详情与解读**（通俗摘要、背景小知识、词语小抄、免责声明）；**随口追问** Chip；**听这段摘要**（TTS）；浏览器打开原文；「孩子推荐」；**温情提醒**；首页 **图片识梗** |
| **孩子** | **分享链接**（含备注 / 粘贴口令）写入后端，家长端可见；首页 **图片识梗** |
| **图片识梗** | 相册选图 → `POST /api/image/explain`（`multipart` 字段 **`file`**）→ OCR 文本再走蓝心解读；**详情页顶部展示本机缓存的原图**（不展示大段 OCR 原文，便于长辈对照画面） |
| **视频快解析** | 粘贴分享文案或链接 → 服务端抽取信息生成会话条目，详情页解读流程与资讯一致 |
| **年轻人话题** | 单独频道列表（与标签热点数据源一致，演示多入口） |

**离线 / 降级约定**

- 未配置或未接通蓝心时，`POST /api/explain` 仍返回 **HTTP 200** 与占位文案；是否真走模型以 **`from_llm: true`** 为准。
- `POST /api/tts` 固定返回 **JSON**（成功 `ok: true` + WAV **`audio_base64`**；上游失败时 `ok: false` 与提示语，**HTTP 仍为 200**）。

---

## 技术栈

- **Android**：Kotlin、Jetpack Compose、Material 3（暖色、大字号）、Retrofit、WorkManager（提醒）；HTTP 明文仅用于 Demo 局域网调试
- **后端**：Python 3.10+、FastAPI、httpx（Chat / OCR）、`websockets`（TTS）、APScheduler（可选定时任务）
- **数据**：热点为服务端 **Mock**（`server/app/services/feed_mock.py`）；分享、识图、快解析等会话条目为 **内存存储**（**重启服务端即清空**）

---

## 仓库结构

```
warmbridge-demo/
├── android/                      # Android Studio 工程根（含 settings.gradle.kts）
│   ├── app/src/main/...          # 界面、主题、网络、Session 原图缓存等
│   └── local.properties          # 本地生成：SDK、API 根地址（勿提交 Git）
├── server/
│   ├── .env                      # VIVO_APP_KEY、OCR/TTS 等（勿提交 Git）
│   ├── .env.example              # 变量模板，可复制为 .env
│   ├── app/
│   │   ├── main.py               # FastAPI 入口
│   │   ├── routers/              # feed、explain、share、image、video_quick、tts …
│   │   ├── services/             # vivo_llm、vivo_ocr、vivo_tts、store、feed_mock …
│   │   └── config.py
│   └── requirements.txt
├── 联调-图片OCR与TTS问题报告.md   # OCR 解析、TTS 握手等与官方文档对齐说明
├── 问题排查清单-OCR-TTS与追问.md
├── 联调问题报告.md               # 明文 HTTP、explain 缓存等通用联调
├── GitHub上传指南.md
├── 暖桥-全量功能与流程说明.md    # 产品级流程（可选阅读）
└── README.md                     # 本文件
```

赛事方提供的 **蓝心接口速查 / PDF** 请自行对照模型名与权限，**不必**放进本仓库。

---

## 从零开始配置并跑起来

默认 **本机同时跑后端 + 编译 App**；手机与电脑需 **同一局域网**。

### 0. 准备环境

- **Python 3.10+**、**Git**
- **Android Studio**（Android SDK、真机或模拟器）
- 放行本机 **8000** 端口入站（真机访问 Windows 开发机时常见）

### 1. 后端：依赖与启动

在 **`server`** 目录：

**Windows**

```bat
cd server
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
```

**macOS / Linux**

```bash
cd server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

编辑 **`server/.env`**：

- **必填（走真云能力）**：`VIVO_APP_KEY`
- **图片识梗**：`VIVO_OCR_BUSINESSID`（文档 §8 固定串或 `aigc`+AppId）；建议同时填 **`VIVO_APP_ID`**，便于服务端在无字时自动回退 `aigc{AppId}`；可选 `VIVO_OCR_POS=2`
- **TTS**：一般可用默认 `VIVO_TTS_ENGINEID` / `VIVO_TTS_VCN`；握手异常见下方「常见问题」与 `.env.example`

启动（**必须** `0.0.0.0`，否则真机访问不到）：

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

浏览器打开 [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs) 可调试接口。

### 2. 确认局域网地址

在运行后端的电脑上查看 IPv4（如 Windows `ipconfig` 取 **WLAN** 地址），记为 `YOUR_IP`。

手机浏览器访问：

- `http://YOUR_IP:8000/health` → `{"status":"ok"}` 等
- `http://YOUR_IP:8000/api/tags` → JSON

### 3. Android：API 根地址

用 Android Studio 打开 **`warmbridge-demo/android`**（打开到含 `settings.gradle.kts` 的目录）。

编辑 **`android/local.properties`**（与 `app` 同级，可新建），**末尾斜杠必填**：

```properties
warmbridge.api.baseUrl=http://YOUR_IP:8000/
```

**官方模拟器、后端在本机**：可不写该项，默认 **`http://10.0.2.2:8000/`**。

**Sync Project with Gradle Files**，真机建议 **卸载旧 App 再安装**。

### 4. 验证密钥是否加载

在已 **activate** 的 `server` 虚拟环境中：

```bash
python -c "from app.config import settings; print('VIVO_APP_KEY 已加载:', bool(settings.vivo_app_key.strip()))"
```

### 5. 运行 App

选角色 → 家长：标签 / 列表 / 详情 / **AI 讲给长辈听**；孩子：**分享链接**；双端均可试 **图片识梗**（成功后详情顶栏为 **原图** + 「来源：识图」）。

---

## 环境变量说明（服务端）

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `VIVO_APP_KEY` | 云端能力必填 | `Authorization: Bearer <AppKey>`（Chat / OCR / TTS） |
| `VIVO_APP_ID` | 识图建议填 | 与文档 `aigc`+AppId、`businessid` 回退一致 |
| `VIVO_CHAT_URL` / `VIVO_CHAT_MODEL` | 否 | 蓝心 Chat Completions |
| `VIVO_OCR_URL` / `VIVO_OCR_BUSINESSID` / `VIVO_OCR_POS` | 识图必填 businessid | 见 `.env.example` §8 |
| `VIVO_TTS_ENGINEID` / `VIVO_TTS_VCN` / `VIVO_TTS_VAID` | 否 | §14 流式 TTS；签名头等已在代码中按文档处理 |
| `WEB_SEARCH_ENABLED` | 否 | 分享解读辅助联网（内网可关） |

完整注释见 **`server/.env.example`**。

---

## 常见问题（精简）

| 现象 | 处理 |
| --- | --- |
| App 明文 HTTP 报错 | Clean/Rebuild 并重装；本 Demo 已配置 cleartext |
| 浏览器能开 `/health`，App 不行 | `local.properties` 的 IP、Sync、重装 |
| `POST /api/image/explain` 400 | 检查 **`file` 是否为真实图片字节**、`.env` 中 OCR **`businessid`** / **`VIVO_OCR_POS`**；详见 [联调-图片OCR与TTS问题报告.md](联调-图片OCR与TTS问题报告.md) |
| TTS 日志 `WebSocket … HTTP 400` | 核对 **AppKey**、控制台 TTS 权限、`engineid`/`vcn` 匹配；参阅 [问题排查清单-OCR-TTS与追问.md](问题排查清单-OCR-TTS与追问.md) |
| 配了 Key 解读仍像离线 | 看响应 **`from_llm`**；[联调问题报告.md](联调问题报告.md)（含 explain 缓存） |
| 识图详情没有原图 | 原图仅 **本机缓存**；清缓存或换机后需重新上传；详见上文「图片识梗」 |
| 协作 / 推送 GitHub | [GitHub上传指南.md](GitHub上传指南.md) |

---

## 安全与协作

- **`VIVO_APP_KEY` 仅放在 `server/.env`**，勿写入 Android 工程，**勿提交 Git**。
- **`android/local.properties`**、**`server/.env`** 应被 `.gitignore` 忽略；每人本地配置自己的局域网 IP 与密钥。

---

## 更多文档

- [联调-图片OCR与TTS问题报告.md](联调-图片OCR与TTS问题报告.md) — OCR 响应结构、TTS 握手与官方示例对齐  
- [问题排查清单-OCR-TTS与追问.md](问题排查清单-OCR-TTS与追问.md) — 接口与客户端字段核对  
- [联调问题报告.md](联调问题报告.md) — 通用网络与 explain 缓存  
- [GitHub上传指南.md](GitHub上传指南.md) — 推送仓库、协作者配置  
- [家长端底部导航-产品方案.md](家长端底部导航-产品方案.md) — 双端 Tab 与产品路线  
- [界面实现规范与资源清单.md](界面实现规范与资源清单.md) — UI 约定与资源  
