package com.warmbridge.demo.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val WarmHomeHeaderOrangeTop = Color(0xFFE07A3D)
val WarmHomeHeaderOrangeMid = Color(0xFFF0A060)
val WarmHomeHeaderOrangeLight = Color(0xFFFFE8D6)
val WarmHomeOnHeaderText = Color.White
val WarmHomeOnHeaderSubtext = Color(0xE6FFFFFF)

fun warmHomeGradientBrush(): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to WarmHomeHeaderOrangeTop,
        0.32f to WarmHomeHeaderOrangeTop,
        0.68f to WarmHomeHeaderOrangeMid,
        0.92f to WarmHomeHeaderOrangeLight,
        1f to WbMinePageBg,
    ),
)

fun warmHomePageGradientBrush(): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to WarmHomeHeaderOrangeTop,
        0.42f to WarmHomeHeaderOrangeTop,
        0.68f to WarmHomeHeaderOrangeMid,
        0.9f to WarmHomeHeaderOrangeLight,
        1f to WbMinePageBg,
    ),
)
