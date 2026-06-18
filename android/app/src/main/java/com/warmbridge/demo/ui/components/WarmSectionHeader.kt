package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens

@Composable
fun WarmSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!actionText.isNullOrBlank() && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = WbBrandOrange,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .semantics { contentDescription = actionText },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmSectionHeaderPreview() {
    WarmBridgeTheme {
        WarmSectionHeader(
            title = "精选内容",
            actionText = "查看更多",
            onAction = {},
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
