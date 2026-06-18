package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
    intro: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
    modifier: Modifier = Modifier,
    navigation: WarmTopBarNavigation = WarmTopBarNavigation.Back,
    navigationEnabled: Boolean = true,
    footerHint: String? = null,
    statusContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WarmTopAppBar(
                title = title,
                onNavigate = onNavigate,
                navigation = navigation,
                navigationEnabled = navigationEnabled,
            )
        },
        bottomBar = {
            WarmPrimaryButton(
                onClick = onPrimaryClick,
                enabled = primaryEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WbDimens.screenPadding, vertical = 16.dp)
                    .height(WbDimens.touchMin),
            ) {
                Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WbDimens.screenPadding)
                .padding(top = 16.dp, bottom = 8.dp),
        ) {
            Text(
                text = intro,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(WbDimens.sectionGap))
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
