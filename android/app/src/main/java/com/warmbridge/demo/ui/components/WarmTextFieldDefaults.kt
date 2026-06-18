package com.warmbridge.demo.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens

@Composable
fun warmTextFieldColors(): TextFieldColors {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = WbBrandOrange,
        unfocusedBorderColor = scheme.outline,
        focusedLabelColor = WbBrandOrange,
        cursorColor = WbBrandOrange,
    )
}

val WarmTextFieldShape = RoundedCornerShape(WbDimens.cardRadius - 4.dp)
