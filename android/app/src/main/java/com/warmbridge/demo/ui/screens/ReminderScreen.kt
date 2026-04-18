package com.warmbridge.demo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.reminder.ReminderWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("记得喝水，休息一下眼睛呀") }
    var seconds by remember { mutableStateOf("15") }
    var hint by remember { mutableStateOf<String?>(null) }

    val perm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hint =
            if (granted) "已授权通知，到点会弹出系统提醒。"
            else "未授权则部分机型可能收不到通知。"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("温情提醒", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            WarmPrimaryButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val sec = seconds.toLongOrNull()?.coerceIn(5, 600) ?: 15L
                    ReminderWorker.schedule(context, text, sec)
                    hint = "已安排约 ${sec} 秒后提醒。"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("定时发送", style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 8.dp),
        ) {
            Text(
                "定时后可在通知栏看到温和提醒（演示用，可先把 App 切到后台）。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text("提醒内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(12.dp),
                colors = ReminderFieldColors(),
                minLines = 4,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = seconds,
                onValueChange = { seconds = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("多少秒后提醒（建议 10～120）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(12.dp),
                colors = ReminderFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ReminderFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.secondary,
    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
)
