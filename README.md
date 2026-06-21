# 暖桥 WarmBridge · Demo

面向「家长—子女」代际沟通的演示应用：子女分享资讯、家长用大字号浏览。含 **Android 客户端** 与 **FastAPI 后端**，对接 vivo 蓝心 Chat / OCR / TTS / 文生图 / 图生视频 等能力。

---

## 项目结构与功能

### 目录结构

```
warmbridge-demo/
├── android/                      # Android 客户端（Kotlin + Compose）
│   ├── app/src/main/...          # 界面、网络、主题
│   └── local.properties          # 本地配置：SDK 路径、后端 API 地址（勿提交 Git）
├── server/                       # FastAPI 后端（BFF）
│   ├── .env                      # 密钥与模型配置（勿提交 Git）
│   ├── .env.example              # 环境变量模板
│   ├── app/
│   │   ├── routers/              # HTTP 接口（解读、识图、分享、TTS、通俗视频…）
│   │   └── services/             # 业务逻辑（LLM、OCR、TTS、ffmpeg 合成…）
│   ├── data/popular_videos/      # 通俗视频成片 mp4、报告 reports/*.md
│   └── requirements.txt          # Python 依赖
├── 暖桥-大模型运用说明.md         # 大模型相关代码说明
└── README.md                     # 本文件
```

### 功能一览

| 角色 / 入口 | 能力 |
| --- | --- |
| **家长** | 按兴趣标签浏览热点；**AI 讲给长辈听**（通俗摘要、背景、词语小抄）；**听这段摘要**（TTS）；随口追问；**图片识梗** |
| **孩子** | **分享链接**（含口令/备注）给家长看；**图片识梗** |
| **图片识梗** | 相册选图 → OCR 识文字 → 蓝心解读 |
| **视频快解析** | 粘贴分享文案/链接 → 解析入库 → 同详情页解读流程 |
| **通俗视频生成** | LLM 写口播稿 → TTS 配音 → 文生图轮播 + 可选片头 → **ffmpeg 合成 mp4**（含硬字幕） |

**技术栈**：Android（Kotlin / Compose / Retrofit）· 后端（Python / FastAPI / httpx）· 通俗视频合成依赖本机 **ffmpeg**（非 pip 包）。

---

## 环境准备

| 工具 | 用途 |
| --- | --- |
| Python 3.10+ | 运行后端 |
| Android Studio | 编译、安装 App |
| **ffmpeg** | 通俗视频：合并音轨、烧录字幕、输出 mp4（**必须单独安装**） |

### 安装 ffmpeg（Windows）

通俗视频最后一步用 ffmpeg 把 TTS 音频、画面、字幕合成 mp4。`requirements.txt` 里**没有** ffmpeg，需要在本机安装并加入系统 PATH。

**方式一：winget（推荐）**

```powershell
winget install --id Gyan.FFmpeg
```

安装完成后**新开一个终端**，执行：

```powershell
ffmpeg -version
```

能显示版本号即表示 PATH 已生效。

**方式二：手动下载**

1. 打开 [https://www.gyan.dev/ffmpeg/builds/](https://www.gyan.dev/ffmpeg/builds/)（或 [ffmpeg.org/download.html](https://ffmpeg.org/download.html) 选 Windows builds）
2. 下载 **ffmpeg-release-essentials.zip**（或 full 版）
3. 解压到固定目录，例如 `C:\ffmpeg`
4. 将 **`bin` 目录**（如 `C:\ffmpeg\bin`）加入系统环境变量 **Path**：
   - 设置 → 系统 → 关于 → 高级系统设置 → 环境变量 → Path → 新建 → 填入 `C:\ffmpeg\bin`
5. **重新打开** PowerShell / CMD，执行 `ffmpeg -version` 验证

**未安装时的表现**：通俗视频任务在「合成」步骤失败，日志提示「未安装 ffmpeg」。

---

## 启动步骤

### 1. 配置 `server/.env`

```bat
cd server
copy .env.example .env
```

用编辑器打开 `server/.env`，填入赛事方提供的 **AppKey** 及 OCR 等参数（字段说明见 `.env.example`）。

**必填**：`VIVO_APP_KEY`  
**识图必填**：`VIVO_OCR_BUSINESSID`、`VIVO_APP_ID`

### 2. 查局域网 IP，写入 `android/local.properties`

在**跑后端的电脑**上执行：

```bat
ipconfig
```

找到当前上网网卡（WLAN / 以太网）的 **IPv4**，例如 `192.168.1.100`。

用 Android Studio 打开 `warmbridge-demo/android`，编辑 `android/local.properties`（无则新建），末尾加一行（**末尾斜杠必填**）：

```properties
warmbridge.api.baseUrl=http://192.168.1.100:8000/
```

> 真机联调必须用局域网 IP，不能写 `127.0.0.1`。模拟器可不写，默认 `10.0.2.2:8000`。

可选：手机浏览器访问 `http://你的IP:8000/health`，应返回 `{"status":"ok"}`。

### 3. 启动后端

在 `server` 目录：

```bat
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

浏览器打开 [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs) 可调试接口。

### 4. 启动 App

1. Android Studio：**Sync Project with Gradle Files**
2. **Build → Rebuild Project**
3. 真机调试时建议卸载旧 App 后重新 Run

改 IP 后必须 **Sync + Rebuild + 重装**，否则 App 仍用旧地址。

---

## 常见问题

| 现象 | 处理 |
| --- | --- |
| App 连接超时 | 重新 `ipconfig` 查 IP → 更新 `local.properties` → Rebuild → 重装 |
| 通俗视频合成失败 | 终端执行 `ffmpeg -version`；无输出则按上文安装 ffmpeg 并重启终端 |
| 解读像离线占位 | 检查 `.env` 中 `VIVO_APP_KEY`；响应看 `from_llm` 字段 |
| 识图 400 | 检查 `VIVO_OCR_BUSINESSID`、`VIVO_APP_ID` |

---

## 安全提示

- `VIVO_APP_KEY` 只放在 `server/.env`，勿写入 Android 工程，勿提交 Git。
- `android/local.properties`、`.env` 应被 `.gitignore` 忽略。


