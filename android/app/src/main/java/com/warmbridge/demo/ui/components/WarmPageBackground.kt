package com.warmbridge.demo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbGradientMid
import com.warmbridge.demo.ui.theme.WbGradientTop
import com.warmbridge.demo.ui.theme.WbPageBg

/**
 * 全页渐变背景 + 极淡几何装饰；可选叠加低透明度头图。
 */
@Composable
fun WarmPageBackground(
    modifier: Modifier = Modifier,
    decorationAsset: String? = null,
    decorationAlpha: Float = 0.05f,
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to WbGradientTop,
                            0.35f to WbGradientMid,
                            1f to WbPageBg,
                        ),
                    ),
                ),
        )
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val arcColor = WbBrandOrange.copy(alpha = 0.05f)
            val lineColor = WbBrandOrange.copy(alpha = 0.035f)

            drawArc(
                color = arcColor,
                startAngle = 200f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * 0.55f, -h * 0.08f),
                size = androidx.compose.ui.geometry.Size(w * 0.7f, w * 0.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = arcColor.copy(alpha = 0.035f),
                startAngle = 210f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(w * 0.62f, h * 0.02f),
                size = androidx.compose.ui.geometry.Size(w * 0.55f, w * 0.55f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )

            val lineSpacing = 28.dp.toPx()
            val lineStartX = -w * 0.05f
            var y = h * 0.55f
            while (y < h * 1.05f) {
                drawLine(
                    color = lineColor,
                    start = Offset(lineStartX, y),
                    end = Offset(w * 0.45f, y + w * 0.35f),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                y += lineSpacing
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WbBrandOrange.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.35f,
                ),
                radius = w * 0.35f,
                center = Offset(w * 0.85f, h * 0.12f),
            )
        }
        decorationAsset?.let { path ->
            AssetPhoto(
                assetPath = path,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(decorationAlpha),
                contentScale = ContentScale.Crop,
                showPlaceholder = false,
            )
        }
    }
}

/** @deprecated 使用 [WarmPageBackground]；保留兼容旧引用。 */
@Deprecated("Use WarmPageBackground", ReplaceWith("WarmPageBackground(modifier, decorationAsset, decorationAlpha)"))
@Composable
fun WarmHeaderGradientBackground(
    modifier: Modifier,
    decorationAsset: String? = null,
    decorationAlpha: Float = 0.12f,
) {
    WarmPageBackground(
        modifier = modifier,
        decorationAsset = decorationAsset,
        decorationAlpha = decorationAlpha,
    )
}

@Preview(showBackground = true, heightDp = 640, widthDp = 360)
@Composable
private fun WarmPageBackgroundPreview() {
    WarmBridgeTheme {
        Box(Modifier.fillMaxSize()) {
            WarmPageBackground(Modifier.fillMaxSize())
        }
    }
}
