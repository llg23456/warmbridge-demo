package com.warmbridge.demo.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbBrandOrange

/** 太阳摇摆动画独立作用域；[animate] 为 false 时静止，避免二级页等待期间后台空转占 GPU。 */
@Composable
fun ParentAnimatedSun(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    tint: Color = WbBrandOrange,
) {
    if (animate) {
        val infinite = rememberInfiniteTransition(label = "sunSway")
        val sunRotate by infinite.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "sunRot",
        )
        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = null,
            tint = tint,
            modifier = modifier.graphicsLayer { rotationZ = sunRotate },
        )
    } else {
        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = null,
            tint = tint,
            modifier = modifier,
        )
    }
}
