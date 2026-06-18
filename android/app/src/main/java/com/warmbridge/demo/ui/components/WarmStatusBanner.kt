package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbDimens

enum class WarmStatusBannerType {
    Success,
    Warning,
    Error,
    Info,
}

@Composable
fun WarmStatusBanner(
    message: String,
    type: WarmStatusBannerType,
    modifier: Modifier = Modifier,
) {
    val (icon, containerColor, contentColor) = when (type) {
        WarmStatusBannerType.Success -> Triple(
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        WarmStatusBannerType.Warning -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        WarmStatusBannerType.Error -> Triple(
            Icons.Default.Error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        WarmStatusBannerType.Info -> Triple(
            Icons.Default.Info,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = message },
        shape = RoundedCornerShape(WbDimens.compactCardRadius),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmStatusBannerPreview() {
    WarmBridgeTheme {
        WarmStatusBanner(
            message = "分享成功，父母端可见",
            type = WarmStatusBannerType.Success,
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
