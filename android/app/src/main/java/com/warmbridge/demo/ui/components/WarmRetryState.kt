package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbDimens

@Composable
fun WarmRetryState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    retryLabel: String = stringResource(R.string.status_retry),
    usePrimaryButton: Boolean = false,
    illustrationAsset: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(WbDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        illustrationAsset?.let { path ->
            AssetPhoto(
                assetPath = path,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit,
                showPlaceholder = false,
            )
            Spacer(Modifier.height(WbDimens.sectionGap))
        }
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
        }
        WarmStatusBanner(message = message, type = WarmStatusBannerType.Error)
        Spacer(Modifier.height(WbDimens.sectionGap))
        if (usePrimaryButton) {
            WarmPrimaryButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(retryLabel)
            }
        } else {
            TextButton(onClick = onRetry) {
                Text(retryLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmRetryStatePreview() {
    WarmBridgeTheme {
        WarmRetryState(
            message = "加载失败，请检查网络与后端地址。",
            onRetry = {},
            title = "今日关注",
        )
    }
}
