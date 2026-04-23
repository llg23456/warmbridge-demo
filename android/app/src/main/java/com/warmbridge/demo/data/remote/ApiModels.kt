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
