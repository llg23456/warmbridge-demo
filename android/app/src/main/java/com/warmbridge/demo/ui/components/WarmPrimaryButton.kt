package com.warmbridge.demo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** 按下时背景过渡到深陶土色；用 Surface 避免部分环境下 ButtonDefaults.buttonColors 不匹配。 */
@Composable
fun WarmPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedBg = Color(0xFFB8542E)
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.onSurface.copy(alpha = 0.12f)
            pressed -> pressedBg
            else -> scheme.primary
        },
        animationSpec = tween(durationMillis = 120),
        label = "warmPrimaryBg",
    )
    val fg by animateColorAsState(
        targetValue = if (enabled) scheme.onPrimary else scheme.onSurface.copy(alpha = 0.38f),
        animationSpec = tween(durationMillis = 120),
        label = "warmPrimaryFg",
    )
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = bg,
        interactionSource = interaction,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(ButtonDefaults.ContentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides fg) {
                content()
            }
        }
    }
}
