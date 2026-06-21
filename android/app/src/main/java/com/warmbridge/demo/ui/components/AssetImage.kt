package com.warmbridge.demo.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * 应用内插画路径集中管理。文件置于 `assets/photos/`，命名小写+下划线。
 * 缺失时不崩溃；[AssetPhoto] 默认显示占位色块，可设 [showPlaceholder] = false 静默跳过。
 */
object WbAssetPhotos {
    private const val P = "photos/"

    const val ROLE_SELECT_HERO = "${P}role_select_hero.png"
    const val ROLE_SELECT_AVATAR_PARENT = "${P}role_select_avatar_parent.png"
    const val ROLE_SELECT_AVATAR_CHILD = "${P}role_select_avatar_child.png"
    const val REMINDER_DIALOG_HEADER = "${P}reminder_dialog_header.png"

    /** 温情提醒页 Hero（左侧留白叠字，右侧铃铛插画） */
    const val REMINDER_HERO = "${P}reminder_hero.png"

    /** 图片识梗页 Hero（左侧留白叠字，右侧插画） */
    const val IMAGE_EXPLAIN_HERO = "${P}image_explain_hero.png"

    /** 视频快解析页 Hero */
    const val VIDEO_QUICK_HERO = "${P}video_quick_hero.png"

    /** 双端首页上半屏背景插画 */
    const val HOME_HEADER_BACKGROUND = "${P}home_header_background.png"

    /** 家长首页头区装饰（与 [PARENT_HOME_WATERMARK] 二选一或叠加使用） */
    const val PARENT_HEADER_DECORATION = "${P}parent_header_decoration.png"
    /** 家长首页底部线稿，代码中建议 α≈0.1 */
    const val PARENT_HOME_WATERMARK = "${P}parent_home_watermark.png"

    const val CHILD_HOME_HEADER = "${P}child_home_header.png"
    const val HOT_TOPICS_HEADER = "${P}hot_topics_header.png"
    const val MINE_HEADER = "${P}mine_header.png"

    const val ILL_EMPTY_FEED = "${P}ill_empty_feed.png"
    /** 与 [ILL_EMPTY_FEED] 同用途，规划书备用文件名 */
    const val EMPTY_FEED_ILLUSTRATION = "${P}empty_feed_illustration.png"

    const val ILL_ERROR_NETWORK = "${P}error_network_illustration.png"
}

/**
 * 从 assets 加载图片；不存在时显示占位底色或静默跳过。
 */
@Composable
fun AssetPhoto(
    assetPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholderColor: Color = Color(0xFFE8E4DF),
    showPlaceholder: Boolean = true,
) {
    val ctx = LocalContext.current
    val bitmap = remember(assetPath) {
        runCatching {
            ctx.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    when {
        bitmap != null -> {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale,
                alignment = alignment,
            )
        }
        showPlaceholder -> {
            Box(
                modifier = modifier.background(placeholderColor),
                contentAlignment = Alignment.Center,
            ) {
                // 占位：无图时仍保留区域高度
            }
        }
    }
}

/** 尝试按多个文件名加载同一逻辑资源（任一存在即可）。 */
@Composable
fun AssetPhotoFirstAvailable(
    assetPaths: List<String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholderColor: Color = Color(0xFFE8E4DF),
    showPlaceholder: Boolean = true,
) {
    val ctx = LocalContext.current
    val bitmap = remember(assetPaths) {
        assetPaths.firstNotNullOfOrNull { path ->
            runCatching {
                ctx.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    when {
        bitmap != null -> {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale,
                alignment = alignment,
            )
        }
        showPlaceholder -> {
            Box(
                modifier = modifier.background(placeholderColor),
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}
