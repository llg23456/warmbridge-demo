package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbChipUnselectedBg
import com.warmbridge.demo.ui.theme.WbTextMuted

@Composable
fun WarmSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = WbChipUnselectedBg,
    ) {
        Row(
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .then(
                            if (selected) {
                                Modifier.shadow(
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.06f),
                                    spotColor = Color.Black.copy(alpha = 0.08f),
                                )
                            } else {
                                Modifier
                            },
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) Color.White else Color.Transparent,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) WbBrandOrange else WbTextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
