package com.warmbridge.demo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.warmbridge.demo.R
import com.warmbridge.demo.reminder.ReminderRepository
import com.warmbridge.demo.ui.components.ReminderContentCard
import com.warmbridge.demo.ui.components.ReminderDemoHintBanner
import com.warmbridge.demo.ui.components.ReminderHeroSection
import com.warmbridge.demo.ui.components.ReminderPresetsCard
import com.warmbridge.demo.ui.components.ReminderScheduleConfirmDialog
import com.warmbridge.demo.ui.components.ReminderTimeCard
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.theme.WbImageExplainBg
import com.warmbridge.demo.ui.theme.WbSurface
import kotlinx.coroutines.launch

@Composable
fun ReminderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminderRepo = remember { ReminderRepository(context) }
    var text by remember { mutableStateOf("记得喝水，休息一下眼睛呀") }
    var selectedQuickSeconds by remember { mutableLongStateOf(300L) }
    var useCustomTime by remember { mutableStateOf(false) }
    var customTriggerAtMillis by remember { mutableStateOf<Long?>(null) }
    var confirmDialogMessage by remember { mutableStateOf<String?>(null) }
    var pendingScheduleSeconds by remember { mutableLongStateOf(0L) }

    fun buildConfirmMessage(delaySeconds: Long, permissionGranted: Boolean?): String {
        val lines = buildList {
            add(context.getString(R.string.reminder_scheduled_hint, delaySeconds))
            when (permissionGranted) {
                true -> add(context.getString(R.string.reminder_perm_granted))
                false -> add(context.getString(R.string.reminder_perm_denied))
                null -> Unit
            }
        }
        return lines.joinToString("\n")
    }

    fun showConfirmDialog(delaySeconds: Long, permissionGranted: Boolean?) {
        confirmDialogMessage = buildConfirmMessage(delaySeconds, permissionGranted)
    }

    fun scheduleReminder(delaySeconds: Long) {
        val triggerAtMillis = System.currentTimeMillis() + delaySeconds * 1000
        scope.launch {
            reminderRepo.schedule(text, delaySeconds, triggerAtMillis)
        }
    }

    val perm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        showConfirmDialog(pendingScheduleSeconds, granted)
    }

    fun resolveDelaySeconds(): Long {
        if (useCustomTime && customTriggerAtMillis != null) {
            val diff = (customTriggerAtMillis!! - System.currentTimeMillis()) / 1000
            return diff.coerceIn(5, 3600)
        }
        return selectedQuickSeconds
    }

    fun onSetReminderClick() {
        val sec = resolveDelaySeconds()
        scheduleReminder(sec)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                showConfirmDialog(sec, true)
            } else {
                pendingScheduleSeconds = sec
                perm.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            showConfirmDialog(sec, null)
        }
    }

    confirmDialogMessage?.let { message ->
        ReminderScheduleConfirmDialog(
            message = message,
            onDismiss = { confirmDialogMessage = null },
        )
    }

    WarmToolScreenScaffold(
        title = stringResource(R.string.mine_reminder),
        onNavigate = onBack,
        primaryLabel = stringResource(R.string.reminder_set),
        onPrimaryClick = ::onSetReminderClick,
        primaryEnabled = text.isNotBlank(),
        primaryButtonBottomPadding = 28.dp,
        containerColor = WbImageExplainBg,
        topBarContainerColor = WbSurface,
        headerContent = {
            ReminderHeroSection()
        },
        bottomBarPrefix = {
            ReminderDemoHintBanner()
        },
        bottomBarPrefixHorizontalPadding = 12.dp,
    ) {
        ReminderContentCard(
            value = text,
            onValueChange = { text = it },
        )
        Spacer(Modifier.height(12.dp))
        ReminderPresetsCard(
            onPresetSelected = { presetText -> text = presetText },
        )
        Spacer(Modifier.height(12.dp))
        ReminderTimeCard(
            selectedQuickSeconds = selectedQuickSeconds,
            useCustomTime = useCustomTime,
            customTriggerAtMillis = customTriggerAtMillis,
            onQuickTimeSelected = { seconds ->
                useCustomTime = false
                selectedQuickSeconds = seconds
            },
            onCustomTimeSelected = { millis ->
                useCustomTime = true
                customTriggerAtMillis = millis
            },
        )
    }
}
