package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.PopularVideoJobDto
import com.warmbridge.demo.ui.components.WarmHomeGroupCard
import com.warmbridge.demo.ui.components.WarmPopularVideoStatusChip
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbCardTitle
import com.warmbridge.demo.ui.theme.WbDivider
import com.warmbridge.demo.ui.theme.WbMinePageBg
import com.warmbridge.demo.ui.theme.WbTextMuted
import com.warmbridge.demo.ui.theme.WarmHomeOnHeaderSubtext
import com.warmbridge.demo.ui.theme.WarmHomeOnHeaderText
import com.warmbridge.demo.ui.theme.warmHomeGradientBrush
import com.warmbridge.demo.util.humanizeNetworkError

private val MineProfileContentHeight = 204.dp
private val MineProfileRowTopGap = 28.dp
/** 渐变底部对齐「任务与提醒」卡片中部：头区 − overlap + 半卡高约 84dp */
private val MineGradientHeight = 276.dp
private val MineContentOverlap = 12.dp
private val MineRowIconSize = 32.dp
private const val MineRowIconStrokeScale = 0.86f

private enum class MinePlaceholderDialog {
    None,
    History,
    Favorites,
    Questions,
    EditProfile,
    JobsEmpty,
}

@Composable
fun MineScreen(
    isParent: Boolean,
    onReminder: () -> Unit,
    onSwitchRole: () -> Unit,
    onOpenPopularVideoJob: (itemId: String, jobId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var placeholderDialog by remember { mutableStateOf(MinePlaceholderDialog.None) }
    var popularJobs by remember { mutableStateOf<List<PopularVideoJobDto>>(emptyList()) }
    var jobsErr by remember { mutableStateOf<String?>(null) }
    var jobsLoading by remember { mutableStateOf(true) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadNonce) {
        jobsLoading = true
        jobsErr = null
        runCatching { NetworkModule.api.popularVideoJobs().jobs }
            .onSuccess { popularJobs = it }
            .onFailure {
                jobsErr = humanizeNetworkError(it) ?: "加载任务列表失败。"
            }
        jobsLoading = false
    }

    val displayName = if (isParent) {
        stringResource(R.string.mine_profile_name_parent)
    } else {
        stringResource(R.string.mine_profile_name_child)
    }
    val demoBadge = if (isParent) {
        stringResource(R.string.mine_demo_parent)
    } else {
        stringResource(R.string.mine_demo_child)
    }

    Column(
        modifier
            .fillMaxSize()
            .background(WbMinePageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(MineGradientHeight)
                    .background(warmHomeGradientBrush()),
            )
            Column(Modifier.fillMaxWidth()) {
                MineProfileHeader(
                    displayName = displayName,
                    demoBadge = demoBadge,
                    onEditProfile = { placeholderDialog = MinePlaceholderDialog.EditProfile },
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = -MineContentOverlap)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
            WarmHomeGroupCard(title = stringResource(R.string.mine_tasks_reminders)) {
                val continueJob = popularJobs.firstOrNull {
                    it.status == "running" || it.status == "interrupted"
                }
                if (continueJob != null) {
                    MineContinueVideoRow(
                        job = continueJob,
                        onClick = { onOpenPopularVideoJob(continueJob.itemId, continueJob.jobId) },
                    )
                }

                when {
                    jobsLoading -> {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WbBrandOrange,
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(R.string.mine_jobs_loading),
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        MineRowDivider()
                    }

                    else -> {
                        val listedJobs = if (continueJob != null) {
                            popularJobs.filter { it.jobId != continueJob.jobId }
                        } else {
                            popularJobs
                        }
                        when {
                            listedJobs.isNotEmpty() -> {
                                listedJobs.forEach { job ->
                                    MineVideoJobRow(
                                        job = job,
                                        onClick = { onOpenPopularVideoJob(job.itemId, job.jobId) },
                                        showDivider = true,
                                    )
                                }
                            }
                            continueJob == null -> {
                                MineIconRow(
                                    title = stringResource(R.string.mine_popular_video_section),
                                    icon = Icons.Outlined.PlayCircleOutline,
                                    onClick = { placeholderDialog = MinePlaceholderDialog.JobsEmpty },
                                )
                            }
                        }
                    }
                }

                MineIconRow(
                    title = stringResource(R.string.mine_reminder),
                    icon = Icons.Outlined.Notifications,
                    onClick = onReminder,
                    showDivider = false,
                )
            }

            jobsErr?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            WarmHomeGroupCard(title = stringResource(R.string.mine_history_favorites)) {
                MineIconRow(
                    title = stringResource(R.string.mine_browse_history),
                    icon = Icons.Outlined.History,
                    onClick = { placeholderDialog = MinePlaceholderDialog.History },
                )
                MineIconRow(
                    title = stringResource(R.string.mine_my_favorites),
                    icon = Icons.Outlined.StarOutline,
                    onClick = { placeholderDialog = MinePlaceholderDialog.Favorites },
                )
                MineIconRow(
                    title = stringResource(R.string.mine_my_questions),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = { placeholderDialog = MinePlaceholderDialog.Questions },
                    showDivider = false,
                )
            }

            WarmHomeGroupCard(title = stringResource(R.string.mine_more)) {
                MineIconRow(
                    title = stringResource(R.string.mine_switch_role),
                    icon = Icons.Outlined.SwitchAccount,
                    onClick = onSwitchRole,
                )
                MineIconRow(
                    title = stringResource(R.string.mine_about),
                    icon = Icons.Outlined.Info,
                    onClick = { showAbout = true },
                )
                MineIconRow(
                    title = stringResource(R.string.mine_privacy),
                    icon = Icons.Outlined.PrivacyTip,
                    onClick = { showPrivacy = true },
                    showDivider = false,
                )
            }
                }
            }
        }
    }

    when (placeholderDialog) {
        MinePlaceholderDialog.History -> MineEmptyPlaceholderDialog(
            title = stringResource(R.string.mine_browse_history),
            message = stringResource(R.string.mine_history_empty_sub),
            onDismiss = { placeholderDialog = MinePlaceholderDialog.None },
        )
        MinePlaceholderDialog.Favorites -> MineEmptyPlaceholderDialog(
            title = stringResource(R.string.mine_favorites_empty_title),
            message = stringResource(R.string.mine_favorites_empty_sub),
            onDismiss = { placeholderDialog = MinePlaceholderDialog.None },
        )
        MinePlaceholderDialog.Questions -> MineEmptyPlaceholderDialog(
            title = stringResource(R.string.mine_questions_empty_title),
            message = stringResource(R.string.mine_questions_empty_sub),
            onDismiss = { placeholderDialog = MinePlaceholderDialog.None },
        )
        MinePlaceholderDialog.EditProfile -> MineEmptyPlaceholderDialog(
            title = stringResource(R.string.mine_edit_profile),
            message = stringResource(R.string.mine_profile_edit_hint),
            onDismiss = { placeholderDialog = MinePlaceholderDialog.None },
        )
        MinePlaceholderDialog.JobsEmpty -> MineEmptyPlaceholderDialog(
            title = stringResource(R.string.mine_popular_video_section),
            message = stringResource(R.string.mine_jobs_empty),
            onDismiss = { placeholderDialog = MinePlaceholderDialog.None },
        )
        MinePlaceholderDialog.None -> Unit
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.mine_about)) },
            text = {
                Text(
                    "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}\n\n${stringResource(R.string.mine_about_message)}",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.mine_close))
                }
            },
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.mine_privacy)) },
            text = { Text(stringResource(R.string.mine_privacy_message)) },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text(stringResource(R.string.mine_close))
                }
            },
        )
    }
}

@Composable
private fun MineContinueVideoRow(
    job: PopularVideoJobDto,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MineRowIcon(icon = Icons.Outlined.PlayCircleOutline)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.mine_continue_video_task),
                    fontSize = 17.sp,
                    color = WbCardTitle,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = job.title.ifBlank { stringResource(R.string.mine_popular_video_section) },
                    fontSize = 15.sp,
                    color = WbTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                WarmPopularVideoStatusChip(
                    status = job.status,
                    progress = job.progress,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_forward),
                tint = WbTextMuted,
            )
        }
        MineRowDivider()
    }
}

@Composable
private fun MineProfileHeader(
    displayName: String,
    demoBadge: String,
    onEditProfile: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(MineProfileContentHeight)
            .statusBarsPadding()
            .padding(top = 4.dp, bottom = 12.dp),
    ) {
        Text(
                text = stringResource(R.string.mine_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = WarmHomeOnHeaderText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = MineProfileRowTopGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 6.dp),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmHomeOnHeaderText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = demoBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmHomeOnHeaderSubtext,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 34.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmHomeOnHeaderText.copy(alpha = 0.85f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WarmHomeOnHeaderText,
                        containerColor = Color.Transparent,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.mine_edit_profile),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
            }
        }
    }
}

@Composable
private fun MineIconRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MineRowIcon(icon = icon)
            Text(
                text = title,
                fontSize = 17.sp,
                color = WbCardTitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_forward),
                tint = WbTextMuted,
            )
        }
        if (showDivider) {
            MineRowDivider()
        }
    }
}

@Composable
private fun MineRowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.size(MineRowIconSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WbBrandOrange,
            modifier = Modifier
                .size(MineRowIconSize)
                .graphicsLayer {
                    scaleX = MineRowIconStrokeScale
                    scaleY = MineRowIconStrokeScale
                },
        )
    }
}

@Composable
private fun MineVideoJobRow(
    job: PopularVideoJobDto,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MineRowIcon(icon = Icons.Outlined.PlayCircleOutline)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
            ) {
                Text(
                    text = job.title.ifBlank { stringResource(R.string.mine_popular_video_section) },
                    fontSize = 17.sp,
                    color = WbCardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                WarmPopularVideoStatusChip(
                    status = job.status,
                    progress = job.progress,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_forward),
                tint = WbTextMuted,
            )
        }
        if (showDivider) {
            MineRowDivider()
        }
    }
}

@Composable
private fun MineRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = WbDivider,
        thickness = 0.5.dp,
    )
}

@Composable
private fun MineEmptyPlaceholderDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mine_close))
            }
        },
    )
}
