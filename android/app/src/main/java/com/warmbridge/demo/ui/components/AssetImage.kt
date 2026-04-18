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

object WbAssetPhotos {
    const val ROLE_SELECT_HERO = "photos/role_select_hero.png"
    const val REMINDER_DIALOG_HEADER = "photos/reminder_dialog_header.png"
}

/**
 * 从 assets加载图片；不存在时显示占位底色。
 */
@Composable
fun AssetPhoto(
    assetPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color = Color(0xFFE8E4DF),
) {
    val ctx = LocalContext.current
    val bitmap = remember(assetPath) {
        runCatching {
            ctx.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier.background(placeholderColor),
            contentAlignment = Alignment.Center,
        ) {
            // 占位：无图时仍保留区域高度
        }
    }
}
