package com.warmbridge.demo.ui.preview

import com.warmbridge.demo.data.remote.FeedItemDto

/**
 * 仅供 @Preview 使用的静态数据，不参与真实业务状态。
 */
object WarmBridgePreviewData {

    const val PARENT_NAME = "李阿姨"
    const val CHILD_NAME = "乐乐"

    val sampleFeedItem = FeedItemDto(
        id = "preview-1",
        title = "示例：健康科普文章标题",
        summary = "这是一句摘要，帮助长辈快速了解内容大意。",
        source = "暖桥日报",
        url = "https://example.com/article",
        tag = "健康",
        channel = "tag",
        updatedAt = "2026-06-11",
    )

    val sampleChildFeedItem = FeedItemDto(
        id = "preview-child-1",
        title = "孩子分享：剑风传奇相关话题",
        summary = "妈，这个番剧最近很火，给您讲讲背景。",
        source = "B站",
        url = "https://example.com/video",
        tag = "动漫",
        channel = "child",
        updatedAt = "2026-06-10",
    )

    val curatedItems: List<FeedItemDto> = listOf(
        sampleFeedItem,
        sampleChildFeedItem,
        sampleFeedItem.copy(
            id = "preview-2",
            title = "年轻人话题：AI 新应用",
            source = "科技日报",
            tag = "AI",
            channel = "trend",
        ),
    )

    val demoShareStatusLines = listOf(
        "妈妈已看完你分享的内容",
        "爸爸还有 1 条待查看",
    )

    val interestTagSummary = "健康、生活"
}
