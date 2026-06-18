package com.warmbridge.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbDivider
import com.warmbridge.demo.ui.theme.WbPageBg
import com.warmbridge.demo.ui.theme.WbTextMuted

data class WarmNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
)

@Composable
fun WarmBottomNavBar(
    selectedRoute: String,
    tabs: List<WarmNavTab>,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(WbPageBg),
    ) {
        HorizontalDivider(color = WbDivider, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WbDimens.navBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                WarmBottomNavItem(
                    selected = selectedRoute == tab.route,
                    onClick = { onTabSelected(tab.route) },
                    icon = tab.icon,
                    label = tab.label,
                    contentDescription = tab.contentDescription,
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun RowScope.WarmBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    contentDescription: String,
) {
    val tint = if (selected) WbBrandOrange else WbTextMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(WbDimens.navBarIconSize),
        )
        Spacer(Modifier.height(WbDimens.navBarIconLabelGap))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
