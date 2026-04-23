package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbChipUnselectedBg
import com.warmbridge.demo.ui.theme.WbRippleOrange
import com.warmbridge.demo.ui.theme.WbTextMuted

@Composable
fun InterestTagChips(
    allTags: List<String>,
    selectedTags: Set<String>,
    onSelectedTagsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipLabels = remember(allTags) { listOf("全部") + allTags }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(chipLabels, key = { it }) { label ->
            val allMode = selectedTags.isEmpty()
            val sel = if (label == "全部") allMode else label in selectedTags
            val interaction = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(color = WbRippleOrange),
                    ) {
                        if (label == "全部") {
                            onSelectedTagsChange(emptySet())
                        } else {
                            val next =
                                if (label in selectedTags) selectedTags - label else selectedTags + label
                            onSelectedTagsChange(next)
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                color = if (sel) WbBrandOrange else WbChipUnselectedBg,
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (sel) Color.White else WbTextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
