# 暖桥 WarmBridge · Demo 工程



Android（Kotlin + Jetpack Compose）+ Python（FastAPI）+ vivo 蓝心 Chat，对齐策划书 MVP。



## UI 设计基准



预览 / 模拟器可参考 **1260 × 2800 px、560 dpi**（逻辑宽约 **360 dp**）。客户端使用 **大字号、暖色 Material3** 主题，详见 `android/app/src/main/res/values/dimens.xml` 说明。



---



## 一、手机 App 如何连上你的电脑后端



### 1. 启动后端（必须先做）



在 **`warmbridge-demo/server`** 目录下。

**首次**在本机需要虚拟环境与依赖：



```bash

cd server

python -m venv .venv

.venv\Scripts\activate

# Windows 用上一行；Linux / macOS：source .venv/bin/activate

pip install -r requirements.txt

copy .env.example .env

# Windows；Linux / macOS：cp .env.example .env

# 再编辑 .env，填写 VIVO_APP_KEY（见下文「二」）

```



**之后每次**启动（若已建 `.venv`，先 `activate`）：



```bash

cd server

uvicorn app.main:app --host 0.0.0.0 --port 8000

```



必须用 **`--host 0.0.0.0`**，否则真机访问不到。本地可调 Swagger：**<http://127.0.0.1:8000/docs>**。



未配置 `VIVO_APP_KEY` 时，`/api/explain` 仍会返回 **HTTP 200** 与离线占位 JSON，便于先调通 App；是否真走蓝心请看响应里的 **`from_llm`**（`true` 为成功）或「背景小知识」文案。



### 2. 确认「唯一正确」的地址



用手机浏览器打开（把 `x.x.x.x` 换成你电脑当前局域网 IP）：



- `http://x.x.x.x:8000/health` → 应看到 `{"status":"ok"}` 或类似 JSON  

- `http://x.x.x.x:8000/api/tags` → 应看到含 `tags` 的 JSON  



**App 里配置的地址必须与浏览器能通的 `x.x.x.x` 完全一致**（换 Wi‑Fi / DHCP 后 IP 会变，需同步改）。



### 3. 在 Android 工程里写死这个地址（真机必做）



1. 用 Android Studio 打开 **`warmbridge-demo/android`**。  

2. 编辑 **`android/local.properties`**（与 `app` 文件夹同级），增加或修改一行（注意末尾 **`/`**）：



```properties

warmbridge.api.baseUrl=http://x.x.x.x:8000/

```



把 `x.x.x.x` 换成上一步浏览器里能打开 `/health` 的 IP（例如你环境中的 `10.70.90.210`）。



3. **File → Sync Project with Gradle Files**，再 **Build → Rebuild Project**。  

4. **卸载手机上的旧 App 再安装**，确保 `BuildConfig.API_BASE_URL` 已更新。



### 4. 官方 Android 模拟器（后端跑在本机时）



可不写 `local.properties`，默认使用 **`http://10.0.2.2:8000/`**（模拟器访问开发机专用地址）。



### 5. 常见现象



| 现象 | 常见原因 |

| --- | --- |

| 浏览器 `10.70.90.210` 能开，App 连 `10.70.50.248` 失败 | `local.properties` 里还是旧 IP，与浏览器不一致 |

| `CLEARTEXT ... not permitted` | 已在本 Demo 的 `network_security_config` 放行 HTTP；若仍出现请 Clean/Rebuild 并重装 App |

| `/health` 通、`/api/...` 不通 | 多为 baseUrl 写错或未 Sync/重装 |



更细的排障步骤见 **[联调问题报告.md](联调问题报告.md)**。



---



## 二、蓝心大模型 API Key（仅服务端，与「能否连上后端」无关）



密钥**只放在 Python 后端**，不要写进 Android 工程。



### 1. 文件位置（必须对）



- 配置文件：**`warmbridge-demo/server/.env`**（与 **`server/app/`** 同级，不是仓库最外层、也不是 `android/`下）。  

- 可从 **`server/.env.example`** 复制改名得到。



### 2. 写法（必须对）



在 `.env` 里**单独一行**（变量名与文档一致）：



```env

VIVO_APP_KEY=你的官网下发的AppKey整串

```



注意：



- 键名必须是 **`VIVO_APP_KEY`**（全大写、下划线）。  

- 一般**不要**加英文双引号；若加，确保成对且中间没有中文引号。  

- **`=` 两侧不要多空格**；密钥后面不要跟注释写在同一行（易解析错）。  

- 保存为 **UTF-8**（Windows 记事本另存为时选 UTF-8）。



可选：



```env

VIVO_CHAT_MODEL=Doubao-Seed-2.0-mini

```



模型名须与账号已开通权限一致，见 `vivo-aigc-接口开发速查.md`。



**环境变量一览（与 `server/.env.example` 一致）：**



| 变量 | 必填 | 说明 |

| --- | --- | --- |

| `VIVO_APP_KEY` | 接真模型时必填 | 官网下发的 AppKey；请求头为 `Authorization: Bearer <AppKey>` |

| `VIVO_CHAT_URL` | 否 | 默认 `https://api-ai.vivo.com.cn/v1/chat/completions` |

| `VIVO_CHAT_MODEL` | 否 | 默认 `Doubao-Seed-2.0-mini`，须与账号权限一致 |

| `VIVO_APP_ID` | 否 | 预留；部分能力（如 OCR）可能需 AppId，见接口文档 |



**AppKey 末尾的 `=` / `==`**：多为 Base64 填充，属正常格式，勿删除。



### 3. 修改后必须重启后端



改 `.env` 后，在运行 `uvicorn` 的终端 **Ctrl+C 停止**，再重新执行：



```bash

uvicorn app.main:app --host 0.0.0.0 --port 8000

```



（本仓库已改为**始终从 `server/.env` 读取**，与你在哪个目录敲命令无关，但 **`server` 目录下启动**仍是推荐习惯。）



### 4. 自检：密钥是否已被进程读到



在 **`server` 目录**执行：



```bash

python -c "from app.config import settings; print('VIVO_APP_KEY 已加载:', bool(settings.vivo_app_key.strip()))"

```



- 输出 **`True`**：进程已读到非空密钥；若 App 仍显示离线，多半是 **调用 vivo 接口失败**（权限/限流/网络），详情见 App 里「背景小知识」是否变为 **「已配置密钥，但调用蓝心 Chat 失败」** 及附带的 HTTP 状态或错误信息。  

- 输出 **`False`**：仍为未加载——检查 `.env` 是否在 **`server/`**、键名是否 **`VIVO_APP_KEY`**、是否保存、是否已重启 `uvicorn`。



### 5. 为什么「我配了 key 仍提示未配置」？常见情况：



1. **`.env` 不在 `server/`**（例如在 `warmbridge-demo` 根目录另建了一个 `.env`，后端不会读）。  

2. **键名写错**（如 `APP_KEY`、`vivo_app_key` 小写在 `.env` 里虽部分环境能映射，但建议严格使用 **`VIVO_APP_KEY`**）。  

3. **改完未重启** `uvicorn`（配置只在启动时加载）。  

4. **其实已读到 key，但 vivo 返回 401/403 等**：以前界面与「完全没 key」共用一句提示；现已区分——**无 key** 与 **调用失败** 在「背景小知识」里文案不同，请根据新版提示继续排查 AppKey/权限/模型名。



---



## 三、体验路径



- 选「家长」→ 兴趣标签 / 孩子推荐 / 年轻人话题 → 列表 → 详情 → **AI 讲给长辈听** → 浏览器打开原文。  

- 选「孩子」→ 分享链接 → 家长端「孩子推荐」可见。  

- 「温情提醒」：定时后系统通知（Android 13+ 需授权通知）。



## 目录结构



```

warmbridge-demo/

├── android/          # Android Studio 工程

├── server/           # FastAPI BFF（.env 放这里）

├── 联调问题报告.md

└── README.md

```



## 安全说明



**勿将 `VIVO_APP_KEY` 写入 Android 工程或提交到公开仓库。** 仅放在 **`server/.env`**，且勿提交 Git。



## 更多文档



- 网络与 IP、`/api/explain` 200 与离线文案、密钥与缓存：[联调问题报告.md](联调问题报告.md)  

- 整理工程、`.gitignore`、推送到 GitHub：[GitHub上传指南.md](GitHub上传指南.md)  

- vivo 接口速查：仓库内 `vivo-aigc-接口开发速查.md`  

- `server/` 目录仅保留简短说明，完整步骤见本文「一」「二」。


