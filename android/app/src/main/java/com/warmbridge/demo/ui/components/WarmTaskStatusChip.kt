package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WarmBridgeTheme

enum class WarmTaskStatus {
    Running,
    Done,
    Failed,
    Interrupted,
}

fun warmTaskStatusFromApi(status: String): WarmTaskStatus = when (status) {
    "done" -> WarmTaskStatus.Done
    "failed" -> WarmTaskStatus.Failed
    "interrupted" -> WarmTaskStatus.Interrupted
    else -> WarmTaskStatus.Running
}

@Composable
fun popularVideoStatusLabel(status: String, progress: Int): String = when (status) {
    "done" -> stringResource(R.string.mine_popular_status_done)
    "failed" -> stringResource(R.string.mine_popular_status_failed)
    "interrupted" -> stringResource(R.string.mine_popular_status_interrupted)
    else -> stringResource(R.string.mine_popular_status_running, progress)
}

@Composable
fun WarmTaskStatusChip(
    label: String,
    status: WarmTaskStatus,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor) = when (status) {
        WarmTaskStatus.Done -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        WarmTaskStatus.Failed -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
        WarmTaskStatus.Interrupted -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        WarmTaskStatus.Running -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun WarmPopularVideoStatusChip(
    status: String,
    progress: Int = 0,
    modifier: Modifier = Modifier,
) {
    WarmTaskStatusChip(
        label = popularVideoStatusLabel(status, progress),
        status = warmTaskStatusFromApi(status),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun WarmTaskStatusChipPreview() {
    WarmBridgeTheme {
        WarmPopularVideoStatusChip(
            status = "running",
            progress = 42,
            modifier = Modifier.padding(16.dp),
        )
    }
}
