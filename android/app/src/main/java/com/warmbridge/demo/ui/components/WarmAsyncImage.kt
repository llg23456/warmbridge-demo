package com.warmbridge.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import coil.compose.SubcomposeAsyncImage
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbSoft

/**
 * 网络图片加载（Coil）。当前 Feed 无远程封面，组件预留给后续 URL 场景。
 */
@Composable
fun WarmAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholderColor: Color = WbSoft,
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            WarmImagePlaceholder(
                color = placeholderColor,
                showProgress = true,
            )
        },
        error = {
            WarmImagePlaceholder(color = placeholderColor, showProgress = false)
        },
    )
}

@Composable
private fun WarmImagePlaceholder(
    color: Color,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier,
                color = WbBrandOrange,
                strokeWidth = 2.dp,
            )
        }
    }
}
