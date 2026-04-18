package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.data.remote.NetworkModule
import java.util.Calendar

private val DefaultInterestTags = listOf("科技", "军事", "人文", "健康", "社会")

private fun parentGreeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 0..10 -> "早上好"
        in 11..16 -> "下午好"
        else -> "晚上好"
    }
}

@Composable
fun ParentHomeScreen(
    onPickTagFeed: (String) -> Unit,
    onChildChannel: () -> Unit,
    onTrendChannel: () -> Unit,
    onReminder: () -> Unit,
    onSwitchRole: () -> Unit,
) {
    var tags by remember { mutableStateOf(DefaultInterestTags) }
    /** 空集 = 未选具体标签（等同「全部」）；可多选 */
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        runCatching { NetworkModule.api.tags().tags }
            .onSuccess { remote -> if (remote.isNotEmpty()) tags = remote }
    }

    val chipLabels = remember(tags) { listOf("全部") + tags }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = parentGreeting(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "今天想看些什么？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(chipLabels, key = { it }) { label ->
                val allMode = selectedTags.isEmpty()
                val sel = if (label == "全部") allMode else label in selectedTags
                val ink = MaterialTheme.colorScheme.secondary
                FilterChip(
                    selected = sel,
                    onClick = {
                        if (label == "全部") {
                            selectedTags = emptySet()
                        } else {
                            selectedTags =
                                if (label in selectedTags) selectedTags - label else selectedTags + label
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ink.copy(alpha = 0.15f),
                        selectedLabelColor = ink,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }

        HomeEntryCard(
            icon = Icons.Filled.Bookmark,
            title = "我的标签",
            subtitle = when {
                selectedTags.isEmpty() -> "按兴趣看热点（当前：全部）"
                else -> "已选 ${selectedTags.size} 个：${selectedTags.joinToString("、")}"
            },
            onClick = {
                val token =
                    if (selectedTags.isEmpty()) "" else selectedTags.sorted().joinToString("|")
                onPickTagFeed(token)
            },
        )
        HomeEntryCard(
            icon = Icons.Filled.Favorite,
            title = "孩子推荐",
            subtitle = "孩子发来的链接会出现在这里。",
            onClick = onChildChannel,
        )
        HomeEntryCard(
            icon = Icons.Filled.Whatshot,
            title = "今日年轻人话题",
            subtitle = "换换口味，看看年轻人在讨论什么梗或话题。",
            onClick = onTrendChannel,
        )

        WarmPrimaryButton(
            onClick = onReminder,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("发一条温情提醒", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = onSwitchRole,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        ) {
            Text(
                "切换身份",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HomeEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.secondary
    val iconBg = ink.copy(alpha = 0.10f)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
