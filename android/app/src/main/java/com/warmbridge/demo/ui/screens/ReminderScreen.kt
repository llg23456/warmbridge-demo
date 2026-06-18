package com.warmbridge.demo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.reminder.ReminderWorker
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.components.warmTextFieldColors

@Composable
fun ReminderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("记得喝水，休息一下眼睛呀") }
    var seconds by remember { mutableStateOf("15") }
    var hint by remember { mutableStateOf<String?>(null) }

    val perm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hint = context.getString(
            if (granted) R.string.reminder_perm_granted else R.string.reminder_perm_denied,
        )
    }

    WarmToolScreenScaffold(
        title = stringResource(R.string.mine_reminder),
        onNavigate = onBack,
        intro = stringResource(R.string.reminder_intro),
        primaryLabel = stringResource(R.string.reminder_schedule),
        onPrimaryClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perm.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            val sec = seconds.toLongOrNull()?.coerceIn(5, 600) ?: 15L
            ReminderWorker.schedule(context, text, sec)
            hint = context.getString(R.string.reminder_scheduled_hint, sec)
        },
        primaryEnabled = text.isNotBlank(),
        footerHint = stringResource(R.string.reminder_demo_local_only),
        statusContent = {
            WarmStatusBanner(
                message = stringResource(R.string.reminder_demo_local_only),
                type = WarmStatusBannerType.Info,
            )
            Spacer(Modifier.height(12.dp))
            hint?.let { message ->
                WarmStatusBanner(
                    message = message,
                    type = WarmStatusBannerType.Success,
                )
                Spacer(Modifier.height(12.dp))
            }
        },
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text(stringResource(R.string.reminder_content_label)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            colors = warmTextFieldColors(),
            minLines = 4,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = seconds,
            onValueChange = { seconds = it.filter { c -> c.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.reminder_delay_label)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            colors = warmTextFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
}
