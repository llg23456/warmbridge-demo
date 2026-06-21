package com.warmbridge.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted

/**
 * 二级工具页通用结构：TopBar → 说明 → 输入区 → 状态 → 辅助说明；主按钮固定底栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmToolScreenScaffold(
    title: String,
    onNavigate: () -> Unit,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
    modifier: Modifier = Modifier,
    intro: String? = null,
    navigation: WarmTopBarNavigation = WarmTopBarNavigation.Back,
    navigationEnabled: Boolean = true,
    footerHint: String? = null,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    primaryContent: (@Composable RowScope.() -> Unit)? = null,
    bottomBarPrefix: (@Composable ColumnScope.() -> Unit)? = null,
    bottomBarPrefixHorizontalPadding: Dp = WbDimens.screenPadding,
    primaryButtonBottomPadding: Dp = 16.dp,
    containerColor: Color? = null,
    topBarContainerColor: Color? = null,
    statusContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scaffoldBg = containerColor ?: MaterialTheme.colorScheme.background
    val topBarBg = topBarContainerColor ?: containerColor ?: MaterialTheme.colorScheme.surface
    Scaffold(
        modifier = modifier,
        containerColor = scaffoldBg,
        topBar = {
            WarmTopAppBar(
                title = title,
                onNavigate = onNavigate,
                navigation = navigation,
                navigationEnabled = navigationEnabled,
                containerColor = topBarBg,
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth()) {
                bottomBarPrefix?.let { prefix ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = bottomBarPrefixHorizontalPadding),
                    ) {
                        prefix()
                    }
                    Spacer(Modifier.height(12.dp))
                }
                WarmPrimaryButton(
                    onClick = onPrimaryClick,
                    enabled = primaryEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WbDimens.screenPadding,
                            end = WbDimens.screenPadding,
                            top = if (bottomBarPrefix != null) 0.dp else 16.dp,
                            bottom = primaryButtonBottomPadding,
                        )
                        .height(WbDimens.touchMin),
                ) {
                    if (primaryContent != null) {
                        primaryContent()
                    } else {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .background(scaffoldBg)
                .padding(pad)
                .verticalScroll(rememberScrollState()),
        ) {
            headerContent?.let { header ->
                header()
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WbDimens.screenPadding)
                    .padding(
                        top = when {
                            headerContent != null -> 12.dp
                            intro != null -> 16.dp
                            else -> 16.dp
                        },
                        bottom = 8.dp,
                    ),
            ) {
                intro?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(WbDimens.sectionGap))
                }
                statusContent()
                content()
                footerHint?.let { hint ->
                    Spacer(Modifier.height(WbDimens.sectionGap))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = WbTextMuted,
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
