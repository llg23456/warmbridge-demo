# 暖桥 Demo · GitHub 上传指南

## 1. 仓库应该建在哪一层？

**把整个 `warmbridge-demo` 文件夹作为一个 Git 仓库上传**（Android + Python 后端 + 文档在一起）。

| 做法 | 说明 |
| --- | --- |
| **推荐：只传 `warmbridge-demo`** | 这是完整可运行的工程（客户端 + BFF + 说明）。评委/协作者 clone 一次即可按 README 跑起来。 |
| **不推荐：只传 `android` 子目录** | 会丢掉 FastAPI 后端和联调文档，`/api/explain` 等能力无法在仓库里复现。 |
| **可选：传上层 `应用赛道初赛作品策划模板`** | 仅当你还有**非代码**的策划书、PPT、截图等要**和代码同仓**时；否则单独用网盘/附件提交策划更常见。 |

结论：**在 `warmbridge-demo` 目录里执行 `git init`，对应 GitHub 上一个仓库即可。**

---

## 2. 建议的 GitHub 仓库名字

任选其一（英文便于 URL与命令行）：

- **`warmbridge-demo`**（与本地文件夹一致，最直观）
- **`warmbridge-app-track`**（强调应用赛道）
- **`WarmBridge`**（短名；若已被占用可加后缀 `-demo`）

在 GitHub 上创建仓库时，**不要**勾选「Add README / .gitignore / license」（你本地已有 README 与 `.gitignore`，避免首次 pull 冲突）。

---

## 3. 哪些必须上传？哪些绝对不能传？

### 3.1 应该提交（团队/公开仓库需要看到的）

- 全部**源代码**：`android/app/src/...`、`server/app/...`
- **`README.md`、`联调问题报告.md`**、本指南
- **`server/.env.example`**（不含真实密钥，仅变量名说明）
- **`server/requirements.txt`**、Gradle 配置、`AndroidManifest` 等构建文件
- 资源与素材（体积合理的前提下）：如 `app/src/main/assets`、`res` 等

### 3.2 不要提交（已在 `.gitignore` 中忽略）

| 类型 | 说明 |
| --- | --- |
| **`server/.env`** | 含 `VIVO_APP_KEY` 等真实密钥，**泄露等于密钥作废** |
| **`android/local.properties`** | 常含本机 SDK 路径、`warmbridge.api.baseUrl` 等个人环境 |
| **`.venv` / `__pycache__`** | 虚拟环境与缓存，体积大且可重建 |
| **`android/**/build/`、`.gradle`** | 构建产物，可重建 |
| **`.idea/`、本地 `.iml`** | IDE 个人配置 |
| **`*.keystore`、`*.jks`** | 签名密钥，勿进公开库 |

上传前务必在本机执行：

```bash
cd warmbridge-demo
git status
```

确认列表里**没有** `.env`、`local.properties`、`.venv` 等。

---

## 4. 第一次上传到 GitHub（命令步骤）

以下在 **`warmbridge-demo` 目录**下操作（路径按你本机修改）。

### 4.1 安装并登录

- 安装 [Git for Windows](https://git-scm.com/download/win)。  
- 可选：安装 [GitHub CLI](https://cli.github.com/)；也可只用浏览器创建仓库 + 本机 Git 命令。

### 4.2 初始化并提交

```powershell
cd "C:\Users\你的用户名\Desktop\应用赛道初赛作品策划模板\warmbridge-demo"

git init
git branch -M main
git add .
git status
```

**仔细检查 `git status` 里是否出现 `.env` / `local.properties`。**若出现，说明未被忽略，不要继续提交，先检查 `.gitignore` 是否在仓库根目录且已保存。

确认无误后：

```powershell
git commit -m "Initial commit: WarmBridge Android + FastAPI demo"
```

### 4.3 在 GitHub 上建空仓库

1. 打开 https://github.com/new  
2. Repository name 填例如 `warmbridge-demo`  
3. 选 **Public** 或 **Private**  
4. **不要**初始化 README / .gitignore / license  
5. 创建后，页面会给出远程地址，例如：  
   `https://github.com/你的用户名/warmbridge-demo.git`

### 4.4 关联远程并推送

```powershell
git remote add origin https://github.com/你的用户名/warmbridge-demo.git
git push -u origin main
```

首次 `push` 时浏览器或 Git 会提示登录 GitHub（Personal Access Token 或 GitHub Desktop）。

---

## 5. 之后改代码怎么更新？

```powershell
cd warmbridge-demo
git add .
git status
git commit -m "简述本次修改"
git push
```

---

## 6. 别人 clone 下来要做什么？

### 6.1 通用步骤

1. 按根目录 **`README.md`** 安装依赖：Python 虚拟环境 + `server/requirements.txt`，Android Studio 打开 **`android/`** 工程。  
2. **`server/.env`**：从 **`server/.env.example`** 复制为 `.env`，填写**他自己账号**的 **`VIVO_APP_KEY`**（不要用你的密钥；密钥不进 Git）。  
3. 启动后端时必须 **`uvicorn ... --host 0.0.0.0 --port 8000`**，否则手机访问不到。

### 6.2 内网地址怎么配？（协作者必看）

接口地址**不在 Git里写死**。谁在电脑上跑后端，App 就要连 **那台电脑在当前局域网里的 IP**；手机与电脑需在同一 Wi‑Fi（或能互通的内网）。

| 场景 | `warmbridge.api.baseUrl` |
| --- | --- |
| **真机，后端在自己电脑** | `http://<本机 IPv4>:8000/` 例如 `http://192.168.1.5:8000/` |
| **官方模拟器，后端在本机** | 可不配：默认 **`http://10.0.2.2:8000/`** |
| **后端在同事电脑，你手机测** | `http://<同事电脑 IPv4>:8000/`（对方需放行防火墙8000 端口） |

**查本机局域网 IP（Windows）**：`ipconfig` → 当前上网网卡（如 WLAN）的 **IPv4 地址**。不要用仓库里截图或文档中举例的某个固定 `10.70.x.x`，每人环境不同，必须自己填。

**写在哪**：编辑 **`android/local.properties`**（与 `android/app` 同级），例如：

```properties
warmbridge.api.baseUrl=http://192.168.1.5:8000/
```

行末 **`/` 必须保留**。改完 **Sync Gradle → Rebuild**；真机建议**卸载旧 App 再安装**，避免仍用旧的 `BuildConfig.API_BASE_URL`。

**自检**：手机浏览器先打开 `http://<同一IP>:8000/health`，能通再跑 App。`local.properties` 已在 `.gitignore`，每人本地一份，**不要提交**。

### 6.3 更多排障

明文 HTTP、换 Wi‑Fi 后 IP 变了等，见 **`README.md`** 与 **`联调问题报告.md`**。

---

## 7. 若密钥曾经误提交到 Git

1. 在 vivo/赛事平台**轮换或作废**该 AppKey。  
2. 从 Git 历史中清除敏感文件较繁琐，公开库建议**删库重建**或查阅 GitHub 文档「Removing sensitive data」。  
3. 新仓库确保 `.env` 从未进入任何 commit。

---

*文档与 `.gitignore` 已按当前工程整理；若你增加新工具链（如 CI），可再补充对应忽略规则。*
