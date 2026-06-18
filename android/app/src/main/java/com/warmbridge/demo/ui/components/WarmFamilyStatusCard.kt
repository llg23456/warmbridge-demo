package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.preview.WarmBridgePreviewData
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted

/** Demo 家庭分享状态，非服务端真实已读。 */
@Composable
fun WarmFamilyStatusCard(
    statusLines: List<String>,
    modifier: Modifier = Modifier,
    demoLabel: String = "演示状态",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.compactCardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = demoLabel,
                style = MaterialTheme.typography.labelSmall,
                color = WbTextMuted,
            )
            statusLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmFamilyStatusCardPreview() {
    WarmBridgeTheme {
        WarmFamilyStatusCard(
            statusLines = WarmBridgePreviewData.demoShareStatusLines,
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
