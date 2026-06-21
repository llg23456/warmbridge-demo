package com.warmbridge.demo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbDivider

@Composable
fun WarmSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    prominentTitle: Boolean = false,
    showTitleDivider: Boolean = false,
    compactContentSpacing: Boolean = false,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val titleStyle: TextStyle = if (prominentTitle) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.titleMedium
    }
    val headerPadding = 10.dp 
    val bodyPadding = if (compactContentSpacing) {
        PaddingValues(
            start = WbDimens.contentPadding,
            end = WbDimens.contentPadding,
            top = ExplainSectionSpacing,
            bottom = 10.dp,
        )
    } else {
        PaddingValues(WbDimens.contentPadding)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (collapsible) {
                            Modifier.clickable { expanded = !expanded }
                        } else {
                            Modifier
                        },
                    )
                    .padding(headerPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = WbBrandOrange,
                    modifier = Modifier.weight(1f),
                )
                headerAction?.invoke()
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) R.string.detail_section_collapse else R.string.detail_section_expand,
                        ),
                        tint = WbBrandOrange,
                    )
                }
            }
            if (showTitleDivider) {
                HorizontalDivider(color = WbDivider)
            }
            if (collapsible) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(Modifier.padding(bodyPadding)) {
                        content()
                    }
                }
            } else {
                Column(Modifier.padding(bodyPadding)) {
                    content()
                }
            }
        }
    }
}

/** 与解读卡片内小标题区块间距一致，供 [WarmSectionCard] 紧凑模式复用。 */
val ExplainSectionSpacing = 2.dp

/** 解读卡片内小标题之间的分隔线留白。 */
val ExplainSectionDividerSpacing = 6.dp
