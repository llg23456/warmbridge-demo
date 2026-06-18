package com.warmbridge.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WarmHomeHeaderOrangeTop
import com.warmbridge.demo.ui.theme.WarmHomeOnHeaderText
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted

/** 今日关注顶栏：标题 + 搜索 + 频道 Tab，紧凑高度由内容决定。 */
@Composable
fun WarmHotTopicsTopChrome(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    tabLabels: List<String>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(WarmHomeHeaderOrangeTop),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.focus_today),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        lineHeight = 22.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    fontWeight = FontWeight.Bold,
                    color = WarmHomeOnHeaderText,
                )
                WarmHotSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    onSearch = onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WbDimens.hotSearchFieldHeight)
                        .padding(horizontal = WbDimens.screenPadding),
                )
            }
        }

        WarmHotTopicsTabRow(
            labels = tabLabels,
            selectedIndex = selectedTabIndex,
            onSelect = onTabSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(WbDimens.hotTabRowHeight),
        )
    }
}

@Composable
private fun WarmHotSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchCd = stringResource(R.string.cd_hot_search)
    val hint = stringResource(R.string.hot_search_hint)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { contentDescription = searchCd },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        singleLine = true,
        cursorBrush = SolidColor(WbBrandOrange),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(WbDimens.hotSearchFieldHeight)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = WbTextMuted,
                    modifier = Modifier.size(18.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = WbTextMuted,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WarmHotTopicsTopChromePreview() {
    WarmBridgeTheme {
        WarmHotTopicsTopChrome(
            searchQuery = "",
            onSearchQueryChange = {},
            tabLabels = listOf("按兴趣", "孩子推荐", "年轻人都在看"),
            selectedTabIndex = 2,
            onTabSelect = {},
        )
    }
}
