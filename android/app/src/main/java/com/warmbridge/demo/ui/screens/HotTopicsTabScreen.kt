package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.components.InterestTagChips
import com.warmbridge.demo.ui.components.WarmHeaderGradientBackground
import com.warmbridge.demo.ui.components.WarmSegmentedControl
import androidx.compose.ui.res.stringResource

@Composable
fun HotTopicsTabScreen(
    showChildChannel: Boolean,
    interestTags: Set<String>,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    showTagFilterEditor: Boolean = false,
    serverTags: List<String> = emptyList(),
    onInterestTagsChange: (Set<String>) -> Unit = {},
) {
    var segment by rememberSaveable { mutableIntStateOf(0) }

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

    Column(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            WarmHeaderGradientBackground(Modifier.matchParentSize())
            Text(
                text = stringResource(R.string.hot_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
            )
        }

        WarmSegmentedControl(
            labels = labels,
            selectedIndex = segment,
            onSelect = { segment = it },
            modifier = Modifier.padding(top = 12.dp),
        )

        val tagSegmentSelected = segment == 0
        if (showTagFilterEditor && tagSegmentSelected) {
            InterestTagChips(
                allTags = serverTags,
                selectedTags = interestTags,
                onSelectedTagsChange = onInterestTagsChange,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }

        val (feedChannel, tagToken) = if (showChildChannel) {
            when (segment) {
                0 -> {
                    val token = if (interestTags.isEmpty()) "ALL"
                    else Uri.encode(interestTags.sorted().joinToString("|"))
                    "tag" to token
                }
                1 -> "child" to "ALL"
                else -> "trend" to "ALL"
            }
        } else {
            when (segment) {
                0 -> {
                    val token = if (interestTags.isEmpty()) "ALL"
                    else Uri.encode(interestTags.sorted().joinToString("|"))
                    "tag" to token
                }
                else -> "trend" to "ALL"
            }
        }

        FeedListContent(
            channel = feedChannel,
            tagToken = tagToken,
            onOpenDetail = onOpenDetail,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, fill = true),
        )
    }
}
