package com.warmbridge.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.warmbridge.demo.ui.theme.WbHeaderGradientOrange

/** 自顶向下 主色 10% → 透明；尺寸由 modifier 决定（如 fillMaxWidth + height） */
@Composable
fun WarmHeaderGradientBackground(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    WbHeaderGradientOrange,
                    WbHeaderGradientOrange.copy(alpha = 0.04f),
                    Color.Transparent,
                ),
            ),
        ),
    )
}
