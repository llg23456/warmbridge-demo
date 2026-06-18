# vivo 蓝心 AIGC 接口开发速查

> 整理自官方接口说明，便于初赛应用开发时快速查阅。  
> **密钥**：在 AIGC 官网「应用赛道参赛资源」等处获取 **AppKey**，代码与文档示例中的 `your_AppKey` / `$AppKey` 均需替换为真实值；**切勿**将密钥提交到公开仓库或截图外传。

---

## 1. 能力概览（云端 API）

| 能力方向 | 典型场景 | 交付形态 |
| --- | --- | --- |
| 大模型 | 创作、知识问答、推理、代码、信息抽取 | 云端 API |
| 图片生成 | 文生图、图生图、风格迁移、扩图/消除等 | 云端 API |
| 视频生成 | 文生视频、图生视频、风格化、动态照片等 | 云端 API |
| 通用 OCR | 文档数字化、阅卷、截图识别、文本审核等 | 云端 API |
| 语音类（短语音识别、方言、同传、长语音听写/转写等） | 搜索、客服、会议字幕等 | 云端 API |
| 音频生成 / 超拟人音色 / 声音复制 | 播报、导航、客服等 | 云端 API |
| 文本翻译、文本向量、文本相似度、查询改写 | 检索、推荐、去重等 | 云端 API |
| 地理编码（POI 搜索） | 生活、出行 | 云端 API |

**语音 / ASR**：实时短识别、长语音听写（WebSocket）、长文件转写（HTTP 分片）、同声传译、方言自由说等，协议与 `engineid` 各异，**详见下文第 13 节**。

**TTS / 声音复刻 / LBS**：流式语音合成（**§14**）、定制音色与 **`vcn`管理**（**§15**）、**POI 地理编码**（**§16**）。

---

## 2. 鉴权

| 项目 | 说明 |
| --- | --- |
| 请求头 | `Authorization: Bearer <AppKey>` |
| 内容类型（JSON 接口） | `Content-Type: application/json`（部分接口为 `application/x-www-form-urlencoded`，以各接口文档为准） |

### 2.1 HTTP 401 常见响应

| 响应体示例 | 含义 | 处理 |
| --- | --- | --- |
| `missing required app_id in the request header` | 认证串格式或内容无效 | 检查是否正确携带 `Authorization: Bearer ...` |
| `invalid api-key` | app_key 无效 | 核对官网下发的 AppKey |
| `not having this ability, you need to apply for it` | 应用未开通该能力 | 联系管理员 / 按赛事要求开通 |

---

## 3. 大模型对话（OpenAI 兼容 Chat Completions）

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/v1/chat/completions` |
| 协议 | 支持主流 OpenAI 格式、Responses API 及三方自定义格式（以文档为准） |
| URL 参数 | `request_id`（必填）：UUID，与文档中 `requestId` 为同一类追踪 ID |

### 3.1 请求头

- `Content-Type: application/json`
- `Authorization: Bearer <AppKey>`

### 3.2 Body 常用字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `model` | 是 | 见下表 |
| `messages` | 视协议 | `role` + `content`；支持 `system` / `user` 等 |
| `stream` | 否 | `true` 流式，`false` 非流式 |
| `max_tokens` | 否 | 回答最大长度（token），**不含**思考内容；默认约 4096 |
| `max_completion_tokens` | 否 | 输出上限（**含**回答 + 思维链），范围约 `[0, 65536]` |
| `reasoning_effort` | 否 | `minimal` / `low` / `medium` / `high`；`minimal` 为默认，偏关闭深度思考 |
| `temperature` | 否 | `[0, 2]`，默认 `1` |
| `top_p` | 否 | 默认 `0.7` |
| `frequency_penalty` / `presence_penalty` | 否 | 范围约 `[-2.0, 2.0]` |
| `tools` | 否 | 函数调用（OpenAI tools 形态） |

**可选模型（文档列出的名称，调用时以实际权限为准）：**

- `Volc-DeepSeek-V3.2`
- `Doubao-Seed-2.0-mini` / `Doubao-Seed-2.0-lite` / `Doubao-Seed-2.0-pro`
- `qwen3.5-plus`

### 3.3 「深度思考」字段差异（易混点）

| 模型 | 字段 | 取值含义 |
| --- | --- | --- |
| Volc-DeepSeek-V3.2、Doubao-Seed 系列 | `thinking.type`（String） | `enabled`：先思考再答；`disabled`：直接答（DeepSeek 默认 `disabled`，Seed 系列默认多为 `enabled`） |
| qwen3.5-plus | `enable_thinking`（bool） | `true`：思考后回复；`false`：直接回复 |

### 3.4 多模态（图片理解）

- `messages[].content` 可为数组，元素类型如 `image_url` + `text`。
- 图片支持 **公网 URL**；本地图需 `data:image/<格式>;base64,<数据>`。
- Python 可用 `openai` 客户端：`base_url = https://api-ai.vivo.com.cn/v1`，`api_key = AppKey`，并在 `default_query` 或等价位置传入 `request_id`。

### 3.5 `messages` 多轮历史格式（官方说明）

- 历史为 **user / assistant 成对** 交替。
- **最后一条**必须是 **一个** `user`，表示当前最新输入。

示例：

```json
"messages": [
  { "role": "user", "content": "你是谁？" },
  { "role": "assistant", "content": "……" },
  { "role": "user", "content": "你会做什么？" }
]
```

### 3.6 同步响应结构要点

- `choices[0].message.content`：正文。
- 可能存在 `reasoning_content`（思考过程，视模型与开关而定）。
- `usage`：`prompt_tokens` / `completion_tokens` / `total_tokens` 等。

### 3.7 流式响应

- SSE：`data: {json}` 多行，结束为 `data: [DONE]`。
- chunk 中 `delta.content` 为增量文本；部分模型在 `delta.reasoning_content` 中输出思考片段（解析时注意与正文拼接逻辑分离）。

---

## 4. 业务错误码（文档摘录）

| code | 含义（摘要） |
| --- | --- |
| 1001 | 参数异常，如 `requestId` 为空等 |
| 1007 | 命中审核，返回干预文案 |
| 30001 | 无模型权限 / 权限过期；或触发模型 QPS 限流 |
| 2003 | 单日用量上限，需次日再试 |

**限流**：可能返回 `429` 或 inner error、`data` 为 null；可 **间隔退避重试**，避免无限重试。

---

## 5. 图片生成

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/api/v1/image_generation` |
| 请求头 | `Content-Type: application/json`，`Authorization: Bearer <AppKey>` |

### 5.1 URL Query

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `module` | 是 | 固定填 `aigc` |
| `request_id` | 是 | UUID |
| `system_time` | 是 | Unix 时间戳（秒） |

### 5.2 Body

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `model` | 是 | 文档示例：`Doubao-Seedream-4.5` |
| `prompt` | 是 | 文本提示 |
| `image` | 否 | 单张 URL 字符串或多张 URL 列表（图生图）；base64 能力以官方更新为准 |
| `parameters` | 否 | 见下表 |

**`parameters` 常用子字段（2026-06-05 更新）**

| 子字段 | 类型 | 说明 |
| --- | --- | --- |
| `size` | string | `2K`、`2048x2048` 等（Seedream 最小约 1920×1920） |
| `sequential_image_generation` | string | `disabled`（默认）或 `auto`（组图模式，一次请求可返回多张） |
| `sequential_image_generation_options` | object | 组图配置（文档未展开子字段，按官方更新为准） |
| `prompt_extend` | 等 | 其它扩展以官网为准 |

### 5.3 响应

- `code == 0` 为成功。
- 图片 URL：**标准路径** `data.images[]`（元素为 URL 字符串或 `{ "url": "..." }`）；**勿再依赖**已废弃的 `data.image`（若仅有 legacy 字段应迁移到 `data.images[0].url`）。
- `data.usage.image_count`：本次实际生成张数（组图 / auto 模式时用于核对配额）。
- **超时建议**：单张约 10–30 秒，高清或多张更久，**建议请求超时 ≥ 60 秒**。

### 5.4 初赛配额（文档说明，2026-06-05）

- 每天约 **50 次** 图片生成任务提交上限。
- 全程约 **500 次** 总任务上限。  
  请合理规划调用，避免滥用。

### 5.5 图片接口业务错误码（HTTP 200 时 body 内）

| code | 说明 |
| --- | --- |
| 1001 | 参数错误 |
| 1002 | 无权限 |
| 1003 | 限流（响应体可能含 `rate_limit` 明细） |
| 1004 | 内容审核不通过 |
| 3001 | 接口异常 |
| 5001 / 5002 | 服务端错误 |

---

## 6. 视频生成（异步：提交任务 + 查询任务）

**流程**：先 `submit_task` 拿到任务 `id`（即 `task_id`），再轮询 `query_task` 直至 `status` 为终态。

### 6.1 配额与建议（初赛文档，2026-06-05）

- 每天约 **10 次** 视频生成任务提交上限。
- 全程约 **200 个** 视频总上限。  
- 生成耗时与分辨率、时长等相关，查询间隔建议 **数秒级退避**，避免高频空转。

### 6.2 提交任务

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/api/v1/submit_task` |
| 请求头 | `Content-Type: application/json`，`Authorization: Bearer <AppKey>` |

**URL Query（必填）**

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `request_id` | string | UUID |
| `system_time` | int | Unix 时间戳（秒） |
| `module` | string | 固定 `aigc` |

**Body（必填）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `model` | string | 见下表 |
| `content` | object / array | 图文混合内容，见下「请求形态」 |

**可选模型（2026-06-05，以账号权限为准）**

| 模型名 | 说明 |
| --- | --- |
| `Doubao-Seedance-1.0-pro` | 文档默认示例 |
| `Doubao-Seedance-2.0` | 新增 |
| `Doubao-Seedance-2.0-fast` | 新增（偏快） |

**`content` 元素类型**

| `type` | 说明 |
| --- | --- |
| `text` | 字段 `text`：提示词；可在文案中附带参数，如 `--ratio 16:9`、`--ratio adaptive`、`--dur 5` 等（以模型支持为准） |
| `image_url` | 字段 `image_url.url`：图片 URL。图生视频可带 `role`：`first_frame` / `last_frame` 表示首尾帧 |

**请求形态摘要**

1. **文生视频**：`content` 仅含 `type: text` 的条目。  
2. **图生视频（首帧）**：`text` + 一张 `image_url`（无 `role` 或按文档默认）。  
3. **图生视频（首尾帧）**：`text` + `image_url`（`role: first_frame`）+ `image_url`（`role: last_frame`）。

**提交成功响应（示例结构）**

- `code == 0`：`data.id` 为任务 ID，后续查询传入 `task_id`。

### 6.3 查询任务

| 项目 | 值 |
| --- | --- |
| 地址 | `GET https://api-ai.vivo.com.cn/api/v1/query_task` |
| 请求头 | `Content-Type: application/json`，`Authorization: Bearer <AppKey>` |

**URL Query（必填）**

| 参数 | 说明 |
| --- | --- |
| `task_id` | 提交任务返回的 `data.id` |
| `request_id` | UUID |
| `system_time` | Unix 时间戳（秒） |
| `module` | 固定 `aigc` |

**查询成功时 `data` 字段（常用）**

| 字段 | 说明 |
| --- | --- |
| `id` | 任务 ID |
| `model` | 实际模型名（可能与提交时字符串存在大小写/后缀差异） |
| `status` | 如 `succeeded`（成功，以文档枚举为准） |
| `content.video_url` | 成片地址 |
| `content.last_frame_url` | 尾帧图（可能为 `null`） |
| `error` | 失败信息（可能为 `null`） |
| `resolution` / `ratio` / `duration` / `framespersecond` | 成片参数摘要 |
| `usage` | 如 `completion_tokens`、`total_tokens` |
| `created_at` / `updated_at` | Unix 时间戳 |

### 6.4 视频接口业务错误码（HTTP 200 时 body 内）

| code | 说明 |
| --- | --- |
| 1001 | 参数错误 |
| 1002 | 无权限 |
| 1003 | 限流（`data.rate_limit` 可含每日/总额度与已用量） |
| 3001 | 接口异常 |
| 3002 | 查询时任务不存在 |
| 5001 / 5002 | 服务端错误 |

---

## 7. Function Calling（简记）

- 除 OpenAI 原生 `tools` 方式外，文档还提供基于 **system 约束 + 结构化标签** 的用法（如要求模型输出 `<APIs>[...]</APIs>`）。
- `messages` 角色可包含：`system`、`user`、`assistant`、`function` / `tool`（将工具执行结果回传模型）。
- 工具定义建议使用 **JSON Schema 风格**（`name` / `description` / `parameters`），与 OpenAI/Claude 等生态对齐，便于迁移。
- **暖桥实测**（`warmbridge-demo/server/test_tools.py`）：`Doubao-Seed-2.0-pro` + `tools` 可返回 `finish_reason=tool_calls`；解读追问见 `vivo_chat_tools.py`（`web_search` → Bing/DDG/百科）。

---

## 8. 通用 OCR（全文识别 + 位置）

**能力**：识别图片中的文字，并可返回位置信息，便于二次排版或对齐。

| 项目 | 值 |
| --- | --- |
| 地址 | `POST http://api-ai.vivo.com.cn/ocr/general_recognition`（文档给定；若环境支持 HTTPS 以最新文档为准） |
| `Content-Type` | `application/x-www-form-urlencoded` |
| `Authorization` | `Bearer <AppKey>` |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `requestId` | 是 | UUID |

**Body（表单字段）**

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `image` | 是 | 图片 **base64**；支持 **jpg / png / bmp** |
| `pos` | 是 | `0`：只要文字；`1`：文字 + **绝对坐标**；`2`：**文字 + 相对坐标**（文档建议 `pos=2`） |
| `businessid` | 是 | 文档主表为 **`"aigc" + AppId`**（字符串拼接）；同页「补充说明」另给出固定串用于不同识别策略，**以参赛/官网最新说明为准** |
| `sessid` | 否 | 可选 UUID，前端透传 |

**`businessid` 补充说明（文档固定取值，与主表并存时请按官方指引选用）**

| 取值 | 说明 |
| --- | --- |
| `1990173156ceb8a09eee80c293135279` | 支持旋转图、非正向文字 |
| `8bf312e702043779ad0f2760b37a0806` | 仅正向文字，速度相对更快 |

**响应（摘要）**

| 字段 | 说明 |
| --- | --- |
| `error_code` | `0` 成功；`1` OCR 失败；`2` 图像错误 |
| `error_msg` | 如 `succ`、`ocr fail`、`no parameter image` |
| `result` | 随 `pos` 不同，含 `words` 或 `OCR`（含 `location` 四角点）等；可能含 `angle`（0/90/180/270） |
| `version` / `support` | 版本与技术支持标识 |

**`pos` 与 `result` 结构（暖桥联调实测）**

| `pos` | 常见 `result` 形态 |
| --- | --- |
| `0` | `words` 常为字符串，或 `[{"words":"一行"}, …]` |
| `1` / `2` | 除 `words` 外常有 `OCR` 数组（含 `location`）；`pos=2` 为相对坐标 |

**易混：成功但无字（非鉴权失败）**

日志或 payload 类似下面时，说明 **OCR 接口已正常执行**，只是 **图片里没有识别到文字**（或文字过小/过糊/无对比度），**不是** `businessid` 写错导致的 `error_code=1/2`：

```json
{
  "error_code": 0,
  "error_msg": "succ",
  "result": { "words": [], "OCR": [], "angle": 0 }
}
```

暖桥服务端会对 **固定 businessid → aigc+AppId**、**pos 0/2 轮换** 自动重试；若最终仍为空，**不再返回 400**，而是走 **Chat §3.4 多模态看图兜底**（`data:image/...;base64,...` 附在 user 消息），由大模型结合画面生成解读并进入详情页。

**识图无字排查（建议顺序）**

1. 换 **JPG/PNG** 字大图清截图；避免纯梗图/表情包（无字）。
2. `businessid`：**二选一**——固定串 `1990173156ceb8a09eee80c293135279`（旋转/非正向）或 `aigc`+`AppId`（如 `aigc2026436089`，无分隔符）；可再试 `8bf312e702043779ad0f2760b37a0806`（仅正向、较快）。
3. `VIVO_OCR_POS`：`0` 与 `2` 均可；暖桥后端无字时会 **自动轮换 pos**。
4. 客户端必须用 **`multipart` 字段名 `file`** 上传真实二进制（勿传 Uri 字符串）。
5. 日志里 `businessid 前8位=aigc2026…` 表示已走 **aigc+AppId** 路径；若仍 `words:[]`，优先怀疑 **图片内容** 而非密钥。

---

## 9. 文本翻译

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/translation/query/self` |
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer <AppKey>` |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `requestId` | 是 | UUID |

**Body（文档示例为表单字段集合；实现时与官方示例对齐）**

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `from` | 是 | 源语言代码 |
| `to` | 是 | 目标语言代码 |
| `text` | 是 | 待译文本，UTF-8，**长度限制约 1200** |
| `app` | 是 | 应用包名，文档固定填 `test` |
| `requestId` | 是 | 与 Query 中类似，可用 UUID |

**集成提示**：文档 Header 为 `application/json` 时，Body 建议使用 JSON 序列化方式发送（如 `requests.post(..., json=body)`）；若使用 `data=dict` 默认会按表单编码，需与 Header、网关要求一致，**以当前官网示例为准**。

**响应（JSON 示例结构）**

- `code == 0`：`data.translation` 为译文，`data.from` / `data.to` / `data.text` 等回显。

**错误码（文档）**

| code | 含义 |
| --- | --- |
| 10000 | 服务器异常 |
| 20000 | 参数错误 |

**语言代码（摘录）**

| 语言 | 代码 |
| --- | --- |
| 中文 | `zh-CHS` |
| 英文 | `en` |
| 日文 | `ja` |
| 韩文 | `ko` |

- `auto`：可识别 **中、英、日、韩**（文档说明）。

---

## 10. 文本向量（Embedding）

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/embedding-model-api/predict/batch` |
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer <AppKey>` |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `requestId` | 是 | UUID |

**Body**

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `model_name` | 是 | `m3e-base` 或 `bge-base-zh-v1.5` |
| `sentences` | 是 | 字符串数组，与返回 `data` 顺序一一对应 |

**模型选用（文档摘要）**

| `model_name` | 适用 |
| --- | --- |
| `bge-base-zh-v1.5` | 偏 **中文召回**；短 query 建议在句首加 instruction：`为这个句子生成表示以用于检索相关文章：` |
| `m3e-base` | 偏 **中文文本比对** |

**响应**

- `data`：二维数组，每行对应一句的向量（维度与模型有关）。

**限制与语种（FAQ）**

- 语种：**中文、英文**（文档说明暂不支持其他语种向量化）。
- 单条文本长度建议 **≤ 500 字**。

---

## 11. 文本相似度（Rerank）

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/rerank` |
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer <AppKey>` |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `requestId` | 是 | UUID |

**Body**

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `model_name` | 是 | 文档当前支持：`bge-reranker-large` |
| `query` | 是 | 查询句 |
| `sentences` | 是 | 待打分句子数组 |

**响应**

- `data`：数组，长度与 `sentences` 相同；每个值为 **query 与该句的相似度得分**（文档示例为浮点数，数值越大通常越相关，具体比较方式以实现为准）。

**限制与语种（FAQ）**

- 语种：**中文、英文**。
- **`query` + 单条 `sentence`** 长度建议 **≤ 500 字**。

---

## 12. 查询改写（RAG / 搜索链路）

| 项目 | 值 |
| --- | --- |
| 地址 | `POST https://api-ai.vivo.com.cn/query_rewrite_base`（文档亦称外网地址；若示例代码为 `http`，以官网最新说明为准） |
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer <AppKey>` |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `requestId` | 是 | UUID |

**Body**

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `prompts` | 是 | **列表**：每项为一段「历史 + 当前问」的打包结构，见下 |

**`prompts` 结构（文档）**

- 支持最多 **3 轮**历史。
- 每个「对话单元」通常包含两行：
  1. **第一行（长度 6 的列表）**：`q3, a3, q2, a2, q1, a1` —无则传空串 `""`。
  2. **第二行（长度 1 的列表）**：仅含当前问 `q`。

**响应**

- `code == 0`：`result` 为改写后的 query 列表（示例：`{'result': ['《战狼》第一部里有吴京吗'], 'code': 0}`）。

**错误码（文档摘录）**

| code | 含义 |
| --- | --- |
| 0 | 正常 |
| -2 | 请求列表格式错误 |
| -3 | 当前 query 长度大于 50 |
| -4 / -5 | 当前 query 含特定词（A/B 类） |
| -6 | 上轮历史只有 question 或只有 answer |
| -8 | 特定模版，不改写 |
| -9 | 模型判定无需改写 |
| -3002 | 服务异常 |

---

## 13. ASR / 语音能力（WebSocket / HTTP）

> 文档中心 **ASR** 类目下含：实时短语音识别、长语音听写、长语音转写、方言自由说、同声传译等。以下为接入要点速查，**握手 URL、测试域名与引擎参数以官网最新说明及 demo 为准**。

### 13.1 公共约定（WebSocket 类）

| 项目 | 说明 |
| --- | --- |
| 域名 | `api-ai.vivo.com.cn`（部分文档示例出现 `asr-test-v2.vivo.com.cn` 等测试域） |
| 路径 | 文档示例：`ws://.../asr/v2?<公参>`；生产环境是否使用 `wss://` 以网关要求为准 |
| Header | `Authorization: Bearer <AppKey>` |
| URL Query | `key=value` 拼接，**值需 urlencode**；常见键见各子能力表格 |
| 音频（多数实时能力） | **16 kHz / 16 bit / 单声道 PCM**；部分能力另支持 **opus**（见 `asr_info.audio_type`） |
| 二进制分包 | 建议每帧约 **40 ms** 音频；单句时长限制因能力而异（短语音一般 **≤ 60 s**） |
| 结束 / 关闭 | 音频发完后发送 **binary**，payload 为文档约定的 **结束标记**（如 `--end--`，具体字符以官方 demo 为准）；断开前可发 **close** 标记（如 `--close--`） |

**握手返回（示例语义）**

- 成功：`action: started`，`code: 0`。
- 失败：`action: error`，`code` 非 0（如 `1001` timeout等）。

**下行通用字段（摘要）**

| 字段 | 说明 |
| --- | --- |
| `action` | `started` / `result` / `error` |
| `type` | 如 `asr`、`nlu`、`common` |
| `code` | 业务码；**听写/同传** 在 `result` 时常用 `8`（中间 var）、`0`（整句 rec）、`9`（末句，可断开） |
| `data` | 结果体 |
| `sid` | 会话 ID |

---

### 13.2 实时短语音识别

| 项目 | 说明 |
| --- | --- |
| 能力 | 单轮 **60 s 内** 短语音 |
| `engineid` | **`shortasrinput`**（通用短语音） |

**URL Query 常用键（均需 encode）**

| 键 | 必填 | 说明 |
| --- | --- | --- |
| `client_version` / `package` / `sdk_version` / `android_version` | 是 | 未知可填文档默认如 `"unknown"` |
| `user_id` | 是 | 32 位，数字 + 小写字母 |
| `system_time` | 是 | Unix **毫秒** |
| `net_type` | 是 | `0` 蜂窝，`1` Wi-Fi |
| `engineid` | 是 | `shortasrinput` |
| `requestId` | 是 | UUID |
| `model` / `system_version` | 否 | 机型与系统版本 |

**首包 JSON（Text，`type: started`）要点**

| 路径 | 说明 |
| --- | --- |
| `request_id` | 单次请求 UUID |
| `asr_info.end_vad_time` | 后端 VAD 相关，单位 **ms** |
| `asr_info.audio_type` | `pcm` / `opus` |
| `asr_info.chinese2digital` | `0` 关 / `1` 开 |
| `asr_info.punctuation` | `0` 无标点 / `1` 有标点 |
| `business_info` | 可选透传 |

**`result` 时 `data`（短语音）**

| 字段 | 说明 |
| --- | --- |
| `text` | 识别文本 |
| `result_id` | 结果序号 |
| `reformation` | `1` 修正，`0` 追加 |
| `is_last` | 是否本句最后一条 |
| `is_finish` | 是否连接级最后一条 |

**错误码（摘录）**：`10000` 参数；`10002` 引擎；`10003`/`10004` 中间/最终结果失败；`10005`/`10006` 解析/内部；`10007` NLU；`10008` 音频超长。

---

### 13.3 长语音听写（WebSocket，不限制单句时长）

| 项目 | 说明 |
| --- | --- |
| `engineid` | **`longasrlisten`** |
| 音频 | **16 kHz / 16 bit 单声道 PCM**（`pcm`/`opus`） |

**URL Query**：与短语音类似，另可有 `product`（内部机型名）等字段，**以文档为准**。

**首包 `asr_info` 要点**

| 字段 | 说明 |
| --- | --- |
| `audio_type` | `pcm` / `opus` |
| `lang` | 可选 `cn` / `en` |
| `punctuation` | 可选，`1` 开标点 |
| `eng_pgsnum` | 中间结果长度控制，建议 **40** |

**`action=result` 时的 `code` 含义**

| `code` | 含义 |
| --- | --- |
| `8` | 中间 **var**（半句） |
| `0` | 中间 **rec**（整句完整结果） |
| `9` | **最后一句**（发完音频后），**可断开连接** |

**`data` 字段**

| 字段 | 说明 |
| --- | --- |
| `var` | 中间识别片段 |
| `onebest` | 整句或最后一句 |
| `bg` / `ed` | 起止时间 **ms** |
| `speaker` | 角色分离：`0` 当前说话人，**非 0** 为角色 ID |

**错误码（摘录）**：`10000` 参数；`10001` 签名；`10002`–`10006` 引擎/解析类；`50001` **使用超量**。

---

### 13.4 长语音转写（HTTP，录音文件）

| 项目 | 说明 |
| --- | --- |
| 能力 | 单次文件 **≤ 5 小时** 且 **&lt; 500 MB** |
| 格式 | `wav`、`pcm`、`m4a`、`mp3`、`aac`、`ogg`、`ogg_opus` 等（以文档枚举为准） |
| 域名 | `api-ai.vivo.com.cn`；路径前缀 **`/lasr/`** |
| `engineid` | 如 **`fileasrrecorder`** |

**五阶段流程**

1. **创建音频** `POST /lasr/create` — JSON：`audio_type`、`x-sessionId`（UUID）、`slice_num`（`ceil(file_size / 5MB)`，**≤100**）。  
2. **分片上传** `POST /lasr/upload` — `multipart/form-data`，Query：`audio_id`、`slice_index`（从 0起）、`x-sessionId`；单分片 **5 MB**；小于5 MB 可 **`slice_num=1`**。  
3. **开始转写** `POST /lasr/run` — JSON：`audio_id`、`x-sessionId` → 返回 **`task_id`**。  
4. **查进度** `POST /lasr/progress` — JSON：`task_id`、`x-sessionId` → `data.progress` **0–100**。  
5. **取结果** `POST /lasr/result` — JSON：`task_id`、`x-sessionId` → `data.result[]`（`onebest`、`bg`、`ed`、`speaker` 等）。

**各阶段 Header**

- `create` / `run` / `progress` / `result`：`Content-Type: application/json; charset=UTF-8`
- `upload`：`multipart/form-data`

**全程携带**：文档要求 **Header 鉴权 + URL公参**（`client_version`、`package`、`user_id`、`system_time`、`engineid`、`requestId` 等）在 **1–5 阶段均需带上**。

**业务错误码分段（文档）**

| 区间 | 阶段 |
| --- | --- |
| `10000`–`10003` | 创建音频 |
| `10100`–`10106` | 分片上传 |
| `10200`–`10203` | 开始转写 |
| `10300`–`10302` | 查询进度 |
| `10400`–`10402` | 查询结果 |

---

### 13.5 同声传译（WebSocket）

| 项目 | 说明 |
| --- | --- |
| `engineid` | **`longasrsubtitle`** |
| 路径示例 | `ws://api-ai.vivo.com.cn/asr/v2?...`（与短语音同形态） |

**首包 `asr_info` 扩展要点**

| 字段 | 说明 |
| --- | --- |
| `lang` | `cn`（中英自识别）、`cn`/`en`/`ja`/`ko` 等 |
| `target_lang` | 翻译目标；英译中 `en_cn`，日译中 `ja_cn`，韩译中 `ko_cn`；不翻译可传 `""`（文档称默认空串） |
| `punctuation` / `eng_pgsnum` | 同听写 |
| `scene` | 如会议 `meet`（文档：会议场景关语气词等） |
| `audio_source` | `1` 系统音 / `2` 麦克 |
| `roletype` | `0` 不分角色 / `1` 分角色（默认 **1**） |
| `tc` | **同声传译开关**：`1` 开 / `0` 关 |
| `end_vad_time` | 静音检测 **ms**，默认约 **1440** |

**`tts_info`（合成侧，可选）**

| 字段 | 说明 |
| --- | --- |
| `selftts` | 同声纹复刻：`1`/`0` |
| `speed` | `[0,100]`，默认 `50` |
| `speaker` | 音色；复刻时使用复刻音色 |
| `audio_code` | `raw` / `speex` / `speex-wb` 等 |
| `volume` | `[1,100]`，默认 `50` |
| `engineid` | 如 `tts_replica` / `short_audio_synthesis_customization` |

**热词（Text 包，`type: hotword`）**

- `hotword_info.business.hotWord`：英文逗号分隔，总长度 ≤ **10000 字节**；单次会话热词数上限约 **3000**。

**`data` 补充字段（翻译 / 同传）**

| 字段 | 说明 |
| --- | --- |
| `src` | 开启翻译时源语文本 |
| `audio` | 开启同声传译时的 **TTS 音频**（编码见配置） |
| `segId` / `isseg` | 字幕分段；日韩转写文档称 **不支持分段** |
| `speaker` | 分角色时角色 ID（**从 1 起**）；`-1` 表示无效 |

**发音人（文档摘录）**

| 语种 | 发音人示例 |
| --- | --- |
| 中文 `cn` | `xiaopei`,`xiaoyan`,`yiyi`,`xiaofang`,`chaoge`,`yifei` |
| 英文 `en` | `Lindsay`,`Catherine` |
| 法语/俄语/日语/韩语等 | `Mariane`,`Allabent`,`xiaolin`,`zhimin` 等 |
| 粤语 `cn_cantonese` | `xiaomei` |
| 中英混合 `cnen` | `xiaopei` |

（完整列表以官网「发音人列表」为准。）

**错误码（摘录）**：与短语音类似，含 `10000`–`10008`等。

---

### 13.6 方言自由说（WebSocket）

| 项目 | 说明 |
| --- | --- |
| 支持方言（文档） | **济南话、河南话、四川话、武汉话** |
| `engineid` | **`shortasrinput`** |
| `user_info` | **必填**：`0` 关 / `1` 开（用户体验开关） |
| `asr_info.lang` | 方言模式：**`dialect`**（文档说明默认中文时可配此项） |

**首包 `asr_info` 其它字段**：`end_vad_time`、`mini_speech_time`（最小说话时长 **ms**，默认约 300）、`audio_type`、`chinese2digital`、`punctuation` 等与短语音类似。

**下行结果**：与 **§13.2 实时短语音** 同类结构（`text`、`reformation`、`is_last` 等）。

---

## 14. 音频生成 / TTS（WebSocket 流式）

> 文档归类于 **TTS → 音频生成**。单句文本转播报；下行约 **每 100 ms** 一帧 PCM（流式）。

### 14.1 连接与鉴权

| 项目 | 说明 |
| --- | --- |
| 协议 | **`wss://`** |
| 地址 | **`wss://api-ai.vivo.com.cn/tts`** |
| 请求行 | `GET /tts HTTP/1.1` |
| 编码 | UTF-8；下行 JSON |
| Header | `Authorization: Bearer <AppKey>`（官方 demo 可能另有自定义头如 `vaid`，**以可运行示例为准**） |
| 音频 | **24 kHz / 16 bit / 单声道**；格式 **PCM**（或按 `aue` 选 opus） |

**URL Query（均需 urlencode）**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `engineid` | 是 | 能力选择，见下表 |
| `system_time` | 是 | 当前时间戳，文档为 **秒级** Unix 时间 |
| `user_id` | 是 | 32 位：数字 + 小写字母 |
| `model` / `product` / `package` / `client_version` / `system_version` / `sdk_version` / `android_version` | 是 | 未知可填文档默认如 `"unknown"` |
| `requestId` | 是 | UUID |

**`engineid` 取值（文档）**

|值 | 场景 |
| --- | --- |
| `short_audio_synthesis_jovi` | **短音频** / 对话合成（助手类） |
| `long_audio_synthesis_screen` | **长音频** / 朗读、屏幕朗读 |
| `tts_humanoid_lam` | **超拟人**大模型音色 |

**握手后首条下行（JSON）**

| `error_code` | 含义 |
| --- | --- |
| `0` | 成功，可开始发合成文本 |
| `10000` | 缺参或签名错误（HTTP **400**） |
| `10001` | WebSocket 升级失败（HTTP **400**） |

### 14.2 合成请求（WebSocket Text帧，JSON 字符串）

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `aue` | 是 | `0`：PCM；`1`：opus |
| `auf` | 是 | 如 **`audio/L16;rate=24000`** |
| `vcn` | 是 | 发音人 ID，须与当前 `engineid` 匹配，见 **§14.4** |
| `speed` | 否 | `[0,100]`，默认 `50` |
| `volume` | 否 | `[1,100]`，默认 `50` |
| `text` | 是 | **UTF-8 文本经 base64 后的字符串**；编码前最大 **2048 字节** |
| `encoding` | 是 | 固定 **`utf8`** |
| `reqId` | 是 | 请求 ID（文档示例为长整型时间戳等） |

### 14.3 合成下行（JSON）

| 路径 | 说明 |
| --- | --- |
| `error_code` / `error_msg` | `0` / `success` 为正常 |
| `sid` | 文本段 ID，**首帧**可能返回 |
| `ver` | 引擎版本号 |
| `data.audio` | **base64** 音频片段 |
| `data.status` | `0` 开始；`1` 合成中；`2` **结束**（最后一帧） |
| `data.progress` | 进度（文档：按句切割，**字节进度**形如 `"2-2"`） |
| `data.slice` | 第几帧 |

**业务错误码（摘录）**

| 区间 | 含义 |
| --- | --- |
| `10010`–`10012` | 非 JSON、缺参、签名 |
| `10030`–`10032` | 与引擎连接/无可用引擎 |
| `11001`–`11010` | 引擎负载、协议、session、文本非法、opus 等 |

### 14.4 发音人 `vcn`（按 `engineid`选用）

**`short_audio_synthesis_jovi`**：`vivoHelper`（奕雯）、`yunye`（云野）、`wanqing`（婉清）、`xiaofu`（晓芙）、`yige_child`（小萌）、`yige`、`yiyi`、`xiaoming`（小茗）。

**`long_audio_synthesis_screen`**：`x2_vivoHelper`、`x2_yige`、`x2_yige_news`、`x2_yunye`、`x2_yunye_news`、`x2_M02`、`x2_M05`、`x2_M10`、`x2_F163`、`x2_F25`、`x2_F22`、`x2_F82`（英文女声）等。

**`tts_humanoid_lam`**：`F245_natural`、`M24`、`M193`、`GAME_GIR_YG`、`GAME_GIR_MB`、`GAME_GIR_YJ`、`GAME_GIR_LTY`、`YIGEXIAOV`、`FY_CANTONESE`、`FY_SICHUANHUA`、`FY_MIAOYU` 等。

**运行示例**：文档提供 `requirements.txt`、`tts_examples.py`、`audio_decode.py` 及 Docker Makefile；环境变量 **`APP_ID` / `APP_KEY`**（与 Bearer 鉴权对应关系以官方说明为准）。

---

## 15. 声音复刻（定制 `vcn` + 短音频合成）

> 上传朗读音频生成 **`vcn`**，再与 **§14** 的 `engineid`（如短音频能力）组合做 TTS。

| 项目 | 说明 |
| --- | --- |
| 域名 | `http://api-ai.vivo.com.cn`（若支持 **HTTPS** 以环境为准） |
| 鉴权 | `Authorization: Bearer <AppKey>` |

### 15.1 `vcn_obj`（音色对象摘要）

| 字段 | 说明 |
| --- | --- |
| `vcn` | 音色 ID，合成时作 **`vcn`** 传入 |
| `status` | `1` 等待 / `2` 提取中 / `3` 完成 / `4` 失败 |
| `create_time` / `update_time` / `complete_time` | 时间字符串 |
| `est_wait_time` | 预估等待秒数 |
| `process` | 进度 `0–100` |
| `attribute` | 可选自定义属性 |
| `engineid` | 与该音色配套的 TTS **`engineid`** |

### 15.2 创建音色任务

| 项目 | 值 |
| --- | --- |
| URI | `POST /replica/create_vcn_task?req_id=<追踪ID>` |
| Header | **`Content-Type: multipart/form-data`** |
| Query | `requestId`（文档：便于追踪） |
| Body | `audio`：**wav文件**，**24 kHz、单声道、16 bit**；`text`：与音频一致的朗读文案 |

**响应（摘录）**：`error_code`、`error_msg`；可选 **`op_str`**（与文案等长的 **M/D/R/I** 编辑距离串，用于标红错读/漏读）、`org_text`、`asr_text`。

### 15.3 查询 / 列表 / 删除

| 功能 | 方法 | URI | Body / 说明 |
| --- | --- | --- | --- |
| 单个音色 | `POST` | `/replica/get_vcn_task?req_id=...` | JSON：`vcn`；Header：`application/json` |
| 音色列表 | `GET` 或 `POST` | `/replica/get_vcn_task_list?req_id=...` | Query：`requestId` |
| 删除 | `POST` | `/replica/del_task?req_id=...` | JSON：`vcn` |

响应体均为 JSON：`error_code`、`error_msg`，及 `vcn_obj` 或 `vcn_obj_list`。

### 15.4 错误码（摘录）

| code | 含义 |
| --- | --- |
| `0` | 成功 |
| `40000` | 未知异常 |
| `40001` | 缺参/参数错误 |
| `40002` | ASR 失败 |
| `40003` | 无此任务 |
| `40004` | 上传失败 |
| `40006` / `40007` | 生成/删除音色失败 |
| `40008` / `40009` | 音频与文本不对齐 / 多读 |
| `40012` | 超出音色数量上限（需删除历史任务） |

---

## 16. 地理编码（POI 搜索）

| 项目 | 值 |
| --- | --- |
| 地址 | **`GET https://api-ai.vivo.com.cn/search/geo`**（示例代码若写 `http://`，建议与网关一致改为 **HTTPS**） |
| Header | `Content-Type: application/json`，`Authorization: Bearer <AppKey>` |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `keywords` | 是 | 搜索关键字 |
| `city` | 是 | **城市名**或**行政区划编码**（如 `深圳市` / `440300`） |
| `page_num` | 否 | 页码；**&lt;1 按 1**；**&gt;20 按 20** |
| `page_size` | 否 | 每页条数；**&lt;1 按 10**；**&gt;15 按 15** |
| `requestId` | 是 | UUID |

**响应（文档字段 + 实际示例扩展字段）**

- 常见：`statusCode`、`statusInfo`、`total` / `totalCount`、`pois`（数组）、`currentDistrict`。
- **`pois[]`** 单项常含：`name`、`address`、`province`、`city`、`district`、`nid`、`phone`、`location`（**经度,纬度**，文档称 **GCJ-02 / 国测局「02」坐标**）、`distance`、`typeName`、`typeCode`、`adcode`、`naviLocation` 等（以实际 JSON 为准）。

**坐标系 FAQ（文档）**

- 接口返回为 **国测局坐标（02 系，与高德一致系别）**；**不支持**直接转百度坐标，需自研转换可参考 [coordTransform_py](https://github.com/wandergis/coordTransform_py)。

---

## 17. 开发清单（自检）

- [ ] 已从官网获取并配置 **AppKey**（及 OCR 等接口需要的 **AppId**），且未写入版本库。
- [ ] 所有需追踪的请求携带 **request_id / requestId**（各接口命名以文档为准）。
- [ ] Chat 多轮 **末尾仅一条 user**。
- [ ] 图片生成 URL 带 **module、request_id、system_time**，超时 **≥ 60s**。
- [ ] 视频生成：**submit** 后保存 **task_id**，**query** 轮询至终态；注意 **10次/日、200 个总量** 配额。
- [ ] 图片生成：响应从 **`data.images[]`** 取 URL；配额 **50次/日、500 次总量**；组图用 `sequential_image_generation=auto` 时核对 `usage.image_count`。
- [ ] OCR：**application/x-www-form-urlencoded** + **base64 图**；`businessid` 与 `pos` 取值正确。
- [ ] 翻译 / 向量 / 相似度 / 改写：注意 **字数限制** 与 **bge 检索 instruction**。
- [ ] **ASR**：选对 **`engineid`**；WebSocket **公参 encode**、`system_time` **毫秒**；短语音 **60 s** 与 **40 ms** 分帧；听写/同传解析 **`code` 8/0/9**；文件转写 **`x-sessionId` 全程一致**、分片 **5 MB**、`slice_num≤100`。
- [ ] **TTS**：**`wss`**、`/tts`、URL里 **`system_time` 为秒**；`text` **先 UTF-8 再 base64**、**≤2048 字节**；按 `engineid` 选 **`vcn`**；解析 **`data.status==2`** 结束；注意与 ASR 采样率差异（TTS **24 kHz**）。
- [ ] **声音复刻**：上传 **24 kHz 单声道 16 bit wav**；轮询 **`vcn_obj.status/process`**；合成时使用返回的 **`vcn` + `engineid`**。
- [ ] **POI**：分页边界 **`page_num`≤20、`page_size`≤15**；理解 **`location` 为 GCJ-02**。
- [ ] 处理 **401、限流、审核、配额** 等错误，避免死循环重试。

---

## 18. 参考链接（以官网为准）

- 开发文档、快应用开放平台、蓝心九问、蓝心智绘等入口均在 **AIGC / 参赛资源页** 提供，请以当前赛事页面链接为准。
- 文档中心目录含：**文本生成**（大模型、Function calling）、**图片/视频生成**、**视觉**（通用 OCR）、**NLP**（翻译、向量、相似度、查询改写）、**ASR**（实时短语音、长语音听写、长语音转写、方言自由说、同声传译）、**TTS**（音频生成、声音复刻）、**LBS**（地理编码/POI）等，接入时按栏目查阅最新版。

---

*文档整理日期：2026-06-05（配额 50/500、10/200；组图参数；Seedance 2.0 模型；若官网有更新请以官网为准）。*
