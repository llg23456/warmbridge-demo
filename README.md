# 暖桥 WarmBridge · Demo

面向「家长—子女」代际沟通的演示应用：**子女分享资讯、家长用大字号界面浏览**，并可 **图片识梗（OCR + 通俗解读）**、**视频链接快解析**、一键 **AI 讲给长辈听**，以及 **通俗视频生成**（D3 片头 + AI 轮播 + 口播 + 硬字幕）。服务端聚合 vivo **蓝心 Chat / OCR §8 / TTS §14 / 文生图 §5 / 图生视频 §6** 等能力。含 **Android 客户端** 与 **FastAPI BFF**。

> **新对话接手**：先读 [暖桥-开发现状与速查.md](暖桥-开发现状与速查.md)（30 秒速览 + API + 环境变量）；改通俗视频读 [通俗视频生成-实现备忘.md](通俗视频生成-实现备忘.md)。

---

## 功能一览

| 角色 / 入口 | 能力 |
| --- | --- |
| **家长** | 兴趣标签浏览热点；**详情与解读**（通俗摘要、背景小知识、词语小抄、免责声明）；**随口追问** Chip；**听这段摘要**（TTS）；浏览器打开原文；「孩子推荐」；**温情提醒**；首页 **图片识梗** |
| **孩子** | **分享链接**（含备注 / 粘贴口令）写入后端，家长端可见；首页 **图片识梗** |
| **图片识梗** | 相册选图 → `POST /api/image/explain`（`multipart` 字段 **`file`**）→ OCR 文本再走蓝心解读；**详情页顶部展示本机缓存的原图**（不展示大段 OCR 原文，便于长辈对照画面） |
| **视频快解析** | 粘贴分享文案或链接 → 服务端抽取信息生成会话条目，详情页解读流程与资讯一致 |
| **通俗视频生成** | D3 片头 + 文生图轮播 + **按句 TTS/字幕对齐** + **连贯口播**（禁止 1.2.3. 分点）；封面 **B 站/RAWG/Bing**（动漫类仍易占位）；Job **持久化+完成通知**；报告 `reports/{job_id}.md` §0 |
| **年轻人话题** | 单独频道列表（与标签热点数据源一致，演示多入口） |

**离线 / 降级约定**

- 未配置或未接通蓝心时，`POST /api/explain` 仍返回 **HTTP 200** 与占位文案；是否真走模型以 **`from_llm: true`** 为准。
- `POST /api/tts` 固定返回 **JSON**（成功 `ok: true` + WAV **`audio_base64`**；上游失败时 `ok: false` 与提示语，**HTTP 仍为 200**）。

---

## 技术栈

- **Android**：Kotlin、Jetpack Compose、Material 3（暖色、大字号）、Retrofit、WorkManager（提醒）；HTTP 明文仅用于 Demo 局域网调试
- **后端**：Python 3.10+、FastAPI、httpx（Chat / OCR）、`websockets`（TTS）、APScheduler（可选定时任务）
- **数据**：热点 Mock；分享/识图等待会话条目 **内存**（重启清空）；通俗视频 **Job 持久化** `data/popular_videos/jobs/*.json`

---

## 仓库结构

```
warmbridge-demo/
├── android/                      # Android Studio 工程根（含 settings.gradle.kts）
│   ├── app/src/main/...          # 界面、主题、网络、Session 原图缓存等
│   └── local.properties          # 本地生成：SDK、API 根地址（勿提交 Git）
├── server/
│   ├── .env                      # 密钥（勿提交 Git）
│   ├── .env.example
│   ├── data/popular_videos/      # 成片 mp4 + reports/*.md（通俗视频报告）
│   ├── app/services/             # 含 popular_video_*、web_lookup、vivo_* …
│   └── requirements.txt
├── 暖桥-开发现状与速查.md         # **新对话必读**（30 秒速览）
├── 通俗视频生成-实现备忘.md       # 通俗视频流水线细节
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

### 2. 局域网 IP 配置（真机联调必看）

App 所有接口（今日热点、解读、识图等）都指向 **`BuildConfig.API_BASE_URL`**。该地址在编译时从 **`android/local.properties`** 读入；**改 IP 后必须 Sync Gradle + 重新安装 App**，只改文件不编译不会生效。

#### 2.1 先判断用哪种地址

| 运行方式 | 是否要写 `local.properties` | 默认 / 应填地址 |
| --- | --- | --- |
| **Android 官方模拟器**，后端在本机 | **可不写** | `http://10.0.2.2:8000/`（模拟器访问宿主机的固定别名） |
| **真机**（USB 或 Wi‑Fi 调试） | **必须写** | `http://<电脑局域网 IPv4>:8000/` |
| 后端在**另一台电脑** | **必须写** | 填那台电脑的局域网 IP |

> **不要**把 `127.0.0.1` 或 `localhost` 写进真机配置——那是手机本机，不是开发机。

#### 2.2 查电脑当前 IPv4（记为 `YOUR_IP`）

在**正在跑 uvicorn 的那台电脑**上执行：

**Windows（PowerShell / CMD）**

```bat
ipconfig
```

在输出中找到 **当前上网用的网卡**（通常是 **无线局域网适配器 WLAN** 或 **以太网**），取其 **IPv4 地址**，例如 `10.129.179.33`。

- 若有多条 `10.x.x.x` / `192.168.x.x`，优先选 **WLAN / 以太网**，不要选 `VMware`、`VirtualBox`、`Hyper-V` 等虚拟网卡地址。
- 校园网 / 公司网换 Wi‑Fi、插拔网线、重启路由器后，IP **常会变化**（DHCP），旧 IP 会直接导致 App「连接超时」。

**macOS / Linux**

```bash
# macOS 常用
ipconfig getifaddr en0

# 或查看全部
ifconfig | grep "inet "
```

#### 2.3 用手机浏览器验证（配置 App 前的唯一标准）

手机与电脑须 **同一 Wi‑Fi / 同一局域网**。在手机浏览器打开（把 `YOUR_IP` 换成上一步查到的地址）：

1. `http://YOUR_IP:8000/health` → 应看到 `{"status":"ok"}`
2. `http://YOUR_IP:8000/api/tags` → 应看到 JSON

**以手机浏览器能打开的 IP 为准**。浏览器打不通时，先修后端 / 防火墙 / 网段，不要先改 App。

若本机浏览器能开 `http://127.0.0.1:8000/health`，但手机打不开 `http://YOUR_IP:8000/health`，常见原因：

- uvicorn 未加 `--host 0.0.0.0`（见上文 §1 启动命令）
- Windows 防火墙未放行 **Python** 或 **8000 端口** 入站
- 手机与电脑不在同一网段（例如手机用 5G、电脑用有线）

#### 2.4 写入 Android 配置

用 Android Studio 打开 **`warmbridge-demo/android`**（含 `settings.gradle.kts` 的目录）。

编辑 **`android/local.properties`**（与 `app` 同级；无则新建）。在文件**末尾**增加或修改一行（**末尾斜杠 `/` 必填**）：

```properties
sdk.dir=...   # Android Studio 自动生成，保留即可

# 真机联调：与手机浏览器 /health 成功的地址完全一致
warmbridge.api.baseUrl=http://YOUR_IP:8000/
```

**示例**（仅示意，请换成你本机当前 IP）：

```properties
warmbridge.api.baseUrl=http://10.129.179.33:8000/
```

Gradle 在 **`app/build.gradle.kts`** 中读取该键并写入 `BuildConfig.API_BASE_URL`；未配置时真机会误用模拟器默认地址 `10.0.2.2`，导致真机永远连不上。

#### 2.5 改 IP 后必做三步

1. Android Studio：**Sync Project with Gradle Files**
2. **Build → Rebuild Project**（或 Clean + Rebuild）
3. 真机 **卸载旧 App 后重新 Run**（避免旧 APK 仍带旧 `API_BASE_URL`）

可在 Logcat 过滤 `OkHttp`，确认实际请求为 `http://YOUR_IP:8000/api/...`，且与手机浏览器测试地址一致。

#### 2.6 换网 / 超时后快速自检

| 现象 | 优先检查 |
| --- | --- |
| 「今日热点」**连接超时** | `local.properties` 里是否是**旧 IP**；重新 `ipconfig` 并更新 |
| 浏览器 `/health` 通，App 不通 | 是否 **Sync + Rebuild + 重装**；Logcat 里 URL 是否仍为旧 IP |
| `Failed to connect` / 连接被拒绝 | 后端是否已启动；端口是否为 **8000** |
| `CLEARTEXT ... not permitted` | Clean/Rebuild 重装；详见 [联调问题报告.md](联调问题报告.md) |

更完整的网络联调说明见 [联调问题报告.md](联调问题报告.md)。

### 3. 验证密钥是否加载

在已 **activate** 的 `server` 虚拟环境中：

```bash
python -c "from app.config import settings; print('VIVO_APP_KEY 已加载:', bool(settings.vivo_app_key.strip()))"
```

### 4. 运行 App

选角色 → 家长：标签 / 列表 / 详情 / **AI 讲给长辈听**；孩子：**分享链接**；双端均可试 **图片识梗**（成功后详情顶栏为 **原图** + 「来源：识图」）。

---

## 环境变量说明（服务端）

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `VIVO_APP_KEY` | 云端能力必填 | `Authorization: Bearer <AppKey>`（Chat / OCR / TTS） |
| `VIVO_APP_ID` | 识图建议填 | 与文档 `aigc`+AppId、`businessid` 回退一致 |
| `VIVO_CHAT_URL` / `VIVO_CHAT_MODEL` | 否 | 蓝心 Chat Completions |
| `VIVO_EXPLAIN_MODEL` / `_FALLBACK` / `_USE_TOOLS` | 否 | 解读追问：**pro** + tools 联网；见 `.env.example` |
| `VIVO_OCR_URL` / `VIVO_OCR_BUSINESSID` / `VIVO_OCR_POS` | 识图必填 businessid | 见 `.env.example` §8 |
| `VIVO_TTS_ENGINEID` / `VIVO_TTS_VCN` / `VIVO_TTS_VAID` | 否 | §14 流式 TTS；签名头等已在代码中按文档处理 |
| `WEB_SEARCH_ENABLED` | 否 | `true`：分享 enrich + **通俗视频 analyze 前检索** |
| `RAWG_API_KEY` | 否 | 游戏类封面（RAWG）；无则降级 Bing |
| `POPULAR_VIDEO_USE_VIVO_MEDIA` | 否 | 文生图轮播；`false` 仅封面 Ken Burns |
| `POPULAR_VIDEO_USE_VIVO_INTRO` | 否 | D3 片头；`false` 仅 D2 轮播 |
| `VIVO_VIDEO_MODEL` | 否 | 默认 `Doubao-Seedance-1.0-pro` |
| `VIVO_IMAGE_SIZE` | 否 | 默认 **`2K`**（勿 1280x720） |
| `VIVO_IMAGE_URL` | 否 | vivo 网关；勿填火山 `ark.volces.com` |

完整注释见 **`server/.env.example`**。

---

## 常见问题（精简）

| 现象 | 处理 |
| --- | --- |
| 今日热点 / 列表 **连接超时** | 重新 `ipconfig` 查 **当前** IPv4 → 更新 `android/local.properties` 的 `warmbridge.api.baseUrl` → Sync → Rebuild → **卸载重装**；见上文 **§2 局域网 IP 配置** |
| App 明文 HTTP 报错 | Clean/Rebuild 并重装；本 Demo 已配置 cleartext |
| 浏览器能开 `/health`，App 不行 | IP 是否与浏览器一致；是否 Sync + Rebuild + 重装；Logcat 看 `OkHttp` 实际 URL |
| `POST /api/image/explain` 400 | 检查 **`file` 是否为真实图片字节**、`.env` 中 OCR **`businessid`** / **`VIVO_OCR_POS`**；详见 [联调-图片OCR与TTS问题报告.md](联调-图片OCR与TTS问题报告.md) |
| TTS 日志 `WebSocket … HTTP 400` | 核对 **AppKey**、控制台 TTS 权限、`engineid`/`vcn` 匹配；参阅 [问题排查清单-OCR-TTS与追问.md](问题排查清单-OCR-TTS与追问.md) |
| 配了 Key 解读仍像离线 | 看响应 **`from_llm`**；[联调问题报告.md](联调问题报告.md)（含 explain 缓存） |
| 识图详情没有原图 | 原图仅 **本机缓存**；清缓存或换机后需重新上传 |
| 通俗视频 og/正文为空 | 抖音口令 **正常**；看 `reports/{job_id}.md` §0 |
| 封面橙色占位 | 看报告 §0：游戏类查 `RAWG_API_KEY`；**动漫类**（剑风传奇）Bing 常 score=0，口播仍正常 |
| 玩梗口播偏题 | **重新分享** 完整口令（含全部 `#话题#`）；旧条目 `share_keywords` 不全 |
| 追问答案跑顶部 | 已改：**追问记录**在按钮下方；响应字段 `follow_up_answer` |
| 字幕拆词（最/新） | 已改：一句一条，28 字内，仅逗号切分 |
| 解读写「从标题和关键词看」 | 已改：`vivo_llm` **只给结论**、禁推理腔；旧缓存需 **重新解读** |
| 词语小抄显示 `['…']` | 已改：`normalize_glossary()` → **1. 2. 3.** 分行 |
| 口播念 1.2.3. / 粘 Bing 原文 | 已改：连贯口播 + `normalize_narration_prose()`；本地兜底不再 dump 检索 |
| 测 Chat tools | `cd server` → `python test_tools.py` |
| 封面像文物/王室 | 「皇室战争」Bing 歧义；已消歧 + RAWG |
| 文生图 code=1003 | vivo 限流 → Ken Burns 仍 done；关 `POPULAR_VIDEO_USE_VIVO_MEDIA` 演示 |
| 通俗视频任务重启丢失 | **Job 已持久化**；`running` 变 `interrupted`，需重新生成；分享列表仍内存 |
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
- [暖桥-开发现状与速查.md](暖桥-开发现状与速查.md) — **新对话必读**（30 秒速览 / API / 环境变量 / 已知问题）
- [通俗视频生成-实现备忘.md](通俗视频生成-实现备忘.md) — 流水线、报告 §0、代码索引、配额
- [联调-图片OCR与TTS问题报告.md](联调-图片OCR与TTS问题报告.md) — OCR / TTS 联调
