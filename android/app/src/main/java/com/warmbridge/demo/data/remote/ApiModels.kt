package com.warmbridge.demo.data.remote

import com.google.gson.annotations.SerializedName

data class TagsResponse(@SerializedName("tags") val tags: List<String>)

data class FeedResponse(@SerializedName("items") val items: List<FeedItemDto>)

data class FeedItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("source") val source: String,
    @SerializedName("url") val url: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("channel") val channel: String = "tag",
    @SerializedName("updated_at") val updatedAt: String = "",
)

data class ExplainRequest(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("question") val question: String? = null,
)

data class ExplainResponse(
    @SerializedName("plain_summary") val plainSummary: String,
    @SerializedName("background") val background: String,
    @SerializedName("glossary") val glossary: String,
    @SerializedName("disclaimer") val disclaimer: String,
    @SerializedName("from_llm") val fromLlm: Boolean = false,
    @SerializedName("suggested_questions") val suggestedQuestions: List<String> = emptyList(),
)

data class ImageExplainResponse(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("ocr_text") val ocrText: String = "",
)

data class VideoQuickRequest(
    /** 单框整段粘贴（推荐） */
    @SerializedName("paste") val paste: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("raw_paste") val rawPaste: String = "",
)

data class VideoQuickResponse(
    @SerializedName("item_id") val itemId: String,
)

data class TtsRequest(
    @SerializedName("text") val text: String,
)

data class TtsResponseDto(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("from_llm") val fromLlm: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("audio_base64") val audioBase64: String? = null,
)

data class ShareRequest(
    @SerializedName("url") val url: String,
    @SerializedName("note") val note: String = "",
    /** 链接框内用户粘贴的整段口令，与 url 一并提交便于服务端抽标题与联网检索 */
    @SerializedName("raw_paste") val rawPaste: String = "",
    @SerializedName("parent_user_id") val parentUserId: String = "parent_demo",
)

data class ShareResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("item_id") val itemId: String,
)

data class PopularVideoStartRequest(
    @SerializedName("item_id") val itemId: String,
)

data class PopularVideoStartResponse(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("reused") val reused: Boolean = false,
    @SerializedName("message") val message: String = "",
)

data class PopularVideoJobDto(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("item_id") val itemId: String,
    @SerializedName("title") val title: String = "",
    @SerializedName("status") val status: String = "running",
    @SerializedName("step") val step: String = "prepare",
    @SerializedName("progress") val progress: Int = 0,
    @SerializedName("error_step") val errorStep: String = "",
    @SerializedName("error_message") val errorMessage: String = "",
    @SerializedName("video_url") val videoUrl: String = "",
    @SerializedName("share_page_url") val sharePageUrl: String = "",
    @SerializedName("narration_preview") val narrationPreview: String = "",
    @SerializedName("created_at") val createdAt: Double = 0.0,
)

data class PopularVideoStatusResponse(
    @SerializedName("job") val job: PopularVideoJobDto,
    @SerializedName("step_label") val stepLabel: String = "",
)

data class PopularVideoJobsResponse(
    @SerializedName("jobs") val jobs: List<PopularVideoJobDto>,
)
