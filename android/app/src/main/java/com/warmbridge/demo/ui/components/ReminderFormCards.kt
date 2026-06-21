package com.warmbridge.demo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbChipUnselectedBg
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted
import com.warmbridge.demo.ui.theme.WbTextPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val IconContainerBg = Color(0xFFFFE8D6)
private val InputBg = Color(0xFFFFF9F0)
private val InputBorder = Color(0xFFE8E0D6)
private val IconContainerRadius = 10.dp
private const val MaxContentLength = 100

data class ReminderPreset(
    val labelRes: Int,
    val textRes: Int,
    val icon: ImageVector,
)

private val reminderPresets = listOf(
    ReminderPreset(R.string.reminder_preset_water, R.string.reminder_preset_water_text, Icons.Outlined.LocalDrink),
    ReminderPreset(R.string.reminder_preset_medicine, R.string.reminder_preset_medicine_text, Icons.Outlined.Medication),
    ReminderPreset(R.string.reminder_preset_clothes, R.string.reminder_preset_clothes_text, Icons.Outlined.Checkroom),
    ReminderPreset(R.string.reminder_preset_sleep, R.string.reminder_preset_sleep_text, Icons.Outlined.Bedtime),
)

private val quickTimeOptions = listOf(
    10L to R.string.reminder_time_10s,
    30L to R.string.reminder_time_30s,
    60L to R.string.reminder_time_1m,
    300L to R.string.reminder_time_5m,
)

@Composable
private fun ReminderFormCardHeader(
    icon: ImageVector,
    title: String,
    trailingHint: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(IconContainerRadius),
            color = IconContainerBg,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WbBrandOrange,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WbBrandOrange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            fontSize = 15.sp,
        )
        trailingHint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = WbTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ReminderContentCard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                start = WbDimens.contentPadding,
                end = WbDimens.contentPadding,
                top = 8.dp,
                bottom = WbDimens.contentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReminderFormCardHeader(
                icon = Icons.Outlined.Edit,
                title = stringResource(R.string.reminder_content_label),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(WbDimens.compactCardRadius),
                color = InputBg,
                border = BorderStroke(1.dp, InputBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp),
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { new ->
                                if (new.length <= MaxContentLength) onValueChange(new)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = WbTextPrimary),
                            cursorBrush = SolidColor(WbBrandOrange),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.reminder_content_counter,
                                value.length,
                                MaxContentLength,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = WbTextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderPresetsCard(
    onPresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReminderFormCardHeader(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.reminder_presets_title),
                trailingHint = stringResource(R.string.reminder_presets_hint),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reminderPresets.forEach { preset ->
                    val presetText = stringResource(preset.textRes)
                    ReminderPresetChip(
                        label = stringResource(preset.labelRes),
                        icon = preset.icon,
                        onClick = { onPresetSelected(presetText) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderPresetChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(WbDimens.chipInnerRadius),
        color = WbChipUnselectedBg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WbTextMuted,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = WbTextPrimary,
                fontSize = 13.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeCard(
    selectedQuickSeconds: Long?,
    useCustomTime: Boolean,
    customTriggerAtMillis: Long?,
    onQuickTimeSelected: (Long) -> Unit,
    onCustomTimeSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val customTimeLabel = remember(customTriggerAtMillis) {
        customTriggerAtMillis?.let { millis ->
            SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReminderFormCardHeader(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.reminder_time_title),
                trailingHint = stringResource(R.string.reminder_time_hint),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickTimeOptions.forEach { (seconds, labelRes) ->
                    val selected = !useCustomTime && selectedQuickSeconds == seconds
                    ReminderQuickTimeChip(
                        label = stringResource(labelRes),
                        selected = selected,
                        onClick = { onQuickTimeSelected(seconds) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(WbDimens.compactCardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (useCustomTime) WbBrandOrange else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = WbBrandOrange,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.reminder_time_custom),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WbTextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                    )
                    Text(
                        text = customTimeLabel
                            ?: stringResource(R.string.reminder_time_custom_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                        color = WbTextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customTriggerAtMillis ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pendingDateMillis = it }
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            is24Hour = true,
            initialHour = Calendar.getInstance().apply {
                customTriggerAtMillis?.let { timeInMillis = it }
            }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply {
                customTriggerAtMillis?.let { timeInMillis = it }
            }.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val combined = combineDateAndTime(
                            pendingDateMillis,
                            timePickerState.hour,
                            timePickerState.minute,
                        )
                        onCustomTimeSelected(combined)
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}

@Composable
private fun ReminderQuickTimeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(WbDimens.compactCardRadius),
            color = if (selected) Color(0xFFFFF5ED) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) WbBrandOrange else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) WbBrandOrange else WbTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = WbBrandOrange,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .padding(end = 2.dp),
            )
        }
    }
}

private fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
