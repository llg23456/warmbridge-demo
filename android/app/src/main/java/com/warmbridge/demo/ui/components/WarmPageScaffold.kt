package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbDimens

/**
 * Tab 页通用壳：全页渐变背景 + 透明 header + 卡片化内容区（首卡 overlap 衔接）。
 */
@Composable
fun WarmPageScaffold(
    modifier: Modifier = Modifier,
    decorationAsset: String? = null,
    decorationAlpha: Float = 0.05f,
    scrollable: Boolean = true,
    contentOverlap: Dp = WbDimens.pageContentOverlap,
    header: @Composable () -> Unit,
    bottomContent: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        WarmPageBackground(
            modifier = Modifier.fillMaxSize(),
            decorationAsset = decorationAsset,
            decorationAlpha = decorationAlpha,
        )

        val contentModifier = Modifier
            .padding(horizontal = WbDimens.screenPadding)
            .offset(y = -contentOverlap)
            .padding(bottom = 24.dp)

        if (scrollable) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                header()
                Column(
                    contentModifier,
                    verticalArrangement = Arrangement.spacedBy(WbDimens.sectionGap),
                    content = content,
                )
                bottomContent()
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                header()
                Column(
                    contentModifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WbDimens.sectionGap),
                    content = content,
                )
            }
        }
    }
}
