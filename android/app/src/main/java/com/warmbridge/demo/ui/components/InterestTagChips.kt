package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbChipUnselectedBg
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbRippleOrange
import com.warmbridge.demo.ui.theme.WbTextMuted

private enum class InterestChipKind {
    All,
    Tag,
    More,
}

private data class InterestChipItem(
    val label: String,
    val kind: InterestChipKind,
    val tag: String? = null,
)

@Composable
fun InterestTagChips(
    allTags: List<String>,
    selectedTag: String?,
    onSelectedTagChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
) {
    val allLabel = stringResource(R.string.hot_interest_all)
    val moreLabel = stringResource(R.string.hot_interest_more)
    val chips = remember(allTags, allLabel, moreLabel) {
        buildList {
            add(InterestChipItem(allLabel, InterestChipKind.All))
            allTags.forEach { add(InterestChipItem(it, InterestChipKind.Tag, tag = it)) }
            add(InterestChipItem(moreLabel, InterestChipKind.More))
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(chips, key = { "${it.kind}:${it.label}" }) { chip ->
            val selected = when (chip.kind) {
                InterestChipKind.All -> selectedTag.isNullOrBlank()
                InterestChipKind.Tag -> chip.tag == selectedTag
                InterestChipKind.More -> false
            }
            InterestChip(
                label = chip.label,
                selected = selected,
                showChevron = chip.kind == InterestChipKind.More,
                onClick = {
                    when (chip.kind) {
                        InterestChipKind.All -> onSelectedTagChange(null)
                        InterestChipKind.Tag -> onSelectedTagChange(chip.tag)
                        InterestChipKind.More -> onMoreClick()
                    }
                },
            )
        }
    }
}

@Composable
private fun InterestChip(
    label: String,
    selected: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember(label, showChevron) { MutableInteractionSource() }
    val chipShape = RoundedCornerShape(WbDimens.interestChipRadius)
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 14.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    Surface(
        modifier = Modifier
            .height(WbDimens.interestChipHeight)
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = WbRippleOrange),
                onClick = onClick,
            ),
        shape = chipShape,
        color = if (selected) WbBrandOrange else WbChipUnselectedBg,
    ) {
        Box(
            Modifier
                .height(WbDimens.interestChipHeight)
                .padding(horizontal = WbDimens.interestChipHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = textStyle,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White else WbTextMuted,
                )
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = WbTextMuted,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(WbDimens.interestChipIconSize),
                    )
                }
            }
        }
    }
}
