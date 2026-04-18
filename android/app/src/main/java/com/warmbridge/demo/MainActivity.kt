package com.warmbridge.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.warmbridge.demo.reminder.ReminderWorker
import com.warmbridge.demo.ui.WarmBridgeRoot
import com.warmbridge.demo.ui.theme.WarmBridgeTheme

class MainActivity : ComponentActivity() {

    private val reminderDialogState = mutableStateOf<String?>(null)

    private val reminderReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ReminderWorker.BROADCAST_REMINDER_FIRED) return
            val text = intent.getStringExtra(ReminderWorker.EXTRA_REMINDER_TEXT) ?: return
            reminderDialogState.value = text
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WarmBridgeTheme {
                val payload by reminderDialogState
                WarmBridgeRoot(
                    reminderDialogText = payload,
                    onDismissReminderDialog = { reminderDialogState.value = null },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ReminderWorker.BROADCAST_REMINDER_FIRED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(reminderReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(reminderReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(reminderReceiver)
    }
}
