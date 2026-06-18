package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.components.InterestTagChips
import com.warmbridge.demo.ui.components.WarmHotTopicsTopChrome
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbPageBg
import com.warmbridge.demo.ui.theme.WbSurface

@Composable
fun HotTopicsTabScreen(
    showChildChannel: Boolean,
    selectedInterestTag: String?,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    showTagFilterEditor: Boolean = false,
    serverTags: List<String> = emptyList(),
    onSelectedInterestTagChange: (String?) -> Unit = {},
) {
    var segment by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val labels = if (showChildChannel) {
        listOf(
            stringResource(R.string.hot_segment_interest),
            stringResource(R.string.hot_segment_child),
            stringResource(R.string.hot_segment_trend),
        )
    } else {
        listOf(
            stringResource(R.string.hot_segment_interest),
            stringResource(R.string.hot_segment_trend),
        )
    }

    val tagSegmentSelected = segment == 0

    val (feedChannel, tagToken) = if (showChildChannel) {
        when (segment) {
            0 -> {
                val token = selectedInterestTag?.let { Uri.encode(it) } ?: "ALL"
                "tag" to token
            }
            1 -> "child" to "ALL"
            else -> "trend" to "ALL"
        }
    } else {
        when (segment) {
            0 -> {
                val token = selectedInterestTag?.let { Uri.encode(it) } ?: "ALL"
                "tag" to token
            }
            else -> "trend" to "ALL"
        }
    }

    Column(modifier.fillMaxSize()) {
        WarmHotTopicsTopChrome(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            tabLabels = labels,
            selectedTabIndex = segment,
            onTabSelect = { segment = it },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showTagFilterEditor && tagSegmentSelected) {
            InterestTagChips(
                allTags = serverTags,
                selectedTag = selectedInterestTag,
                onSelectedTagChange = onSelectedInterestTagChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WbSurface)
                    .padding(horizontal = WbDimens.screenPadding)
                    .padding(top = 10.dp, bottom = 8.dp),
            )
        }

        FeedListContent(
            channel = feedChannel,
            tagToken = tagToken,
            onOpenDetail = onOpenDetail,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, fill = true)
                .background(WbPageBg),
        )
    }
}
