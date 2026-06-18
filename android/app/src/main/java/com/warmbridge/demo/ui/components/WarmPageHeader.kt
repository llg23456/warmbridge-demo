package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbDimens

/**
 * Tab 页透明问候/标题区，无独立色块背景。
 */
@Composable
fun WarmPageHeader(
    modifier: Modifier = Modifier,
    height: Dp = WbDimens.pageHeaderHeightParent,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = WbDimens.screenPadding),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(Modifier.padding(bottom = 8.dp), content = content)
    }
}
