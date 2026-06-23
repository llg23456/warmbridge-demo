package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.R
import com.warmbridge.demo.data.local.ApiBaseUrlPreferences
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.ui.components.WarmHomeGroupCard
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.ui.components.WarmTextFieldShape
import com.warmbridge.demo.ui.components.WarmTopAppBar
import com.warmbridge.demo.ui.components.warmTextFieldColors
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbMinePageBg
import com.warmbridge.demo.ui.theme.WbTextMuted
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.launch

private const val EmulatorDefaultUrl = "http://10.0.2.2:8000/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { ApiBaseUrlPreferences(context) }

    var draftUrl by remember { mutableStateOf("") }
    var effectiveUrl by remember { mutableStateOf(BuildConfig.API_BASE_URL) }
    var testing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val override = prefs.getOverride()
        draftUrl = override ?: BuildConfig.API_BASE_URL
        effectiveUrl = prefs.effectiveBaseUrl()
    }

    LaunchedEffect(prefs) {
        prefs.overrideUrl.collect { override ->
            effectiveUrl = ApiBaseUrlPreferences.effectiveBaseUrl(override)
        }
    }

    fun validateDraft(): Boolean {
        if (!ApiBaseUrlPreferences.isValidHttpUrl(draftUrl)) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.api_settings_invalid_url))
            }
            return false
        }
        return true
    }

    Scaffold(
        containerColor = WbMinePageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            WarmTopAppBar(
                title = stringResource(R.string.mine_privacy),
                onNavigate = onBack,
                containerColor = WbMinePageBg,
            )
        },
        bottomBar = {
            WarmPrimaryButton(
                onClick = {
                    if (!validateDraft()) return@WarmPrimaryButton
                    scope.launch {
                        saving = true
                        runCatching {
                            NetworkModule.updateBaseUrl(draftUrl)
                        }.onSuccess {
                            snackbarHostState.showSnackbar(context.getString(R.string.api_settings_saved))
                            testMessage = null
                            testSuccess = null
                        }.onFailure {
                            snackbarHostState.showSnackbar(
                                humanizeNetworkError(it) ?: context.getString(R.string.api_settings_save_failed),
                            )
                        }
                        saving = false
                    }
                },
                enabled = !saving && !testing && draftUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = WbDimens.screenPadding,
                        end = WbDimens.screenPadding,
                        top = 12.dp,
                        bottom = 16.dp,
                    ),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.api_settings_save),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(WbMinePageBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WarmHomeGroupCard(title = stringResource(R.string.api_settings_section_privacy)) {
                Text(
                    text = stringResource(R.string.mine_privacy_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            WarmHomeGroupCard(title = stringResource(R.string.api_settings_section_server)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.api_settings_current_effective, effectiveUrl),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WbTextMuted,
                    )
                    Text(
                        text = stringResource(R.string.api_settings_build_default, BuildConfig.API_BASE_URL),
                        style = MaterialTheme.typography.bodySmall,
                        color = WbTextMuted,
                    )
                    OutlinedTextField(
                        value = draftUrl,
                        onValueChange = {
                            draftUrl = it
                            testMessage = null
                            testSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.api_settings_url_label)) },
                        placeholder = { Text(stringResource(R.string.api_settings_url_hint)) },
                        singleLine = true,
                        shape = WarmTextFieldShape,
                        colors = warmTextFieldColors(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { draftUrl = EmulatorDefaultUrl }) {
                            Text(stringResource(R.string.api_settings_preset_emulator))
                        }
                        TextButton(
                            onClick = {
                                draftUrl = BuildConfig.API_BASE_URL
                                scope.launch {
                                    NetworkModule.updateBaseUrl(BuildConfig.API_BASE_URL)
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.api_settings_reset_done),
                                    )
                                    testMessage = null
                                    testSuccess = null
                                }
                            },
                        ) {
                            Text(stringResource(R.string.api_settings_reset_default))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!validateDraft()) return@OutlinedButton
                            scope.launch {
                                testing = true
                                testMessage = null
                                testSuccess = null
                                val result = NetworkModule.testHealth(draftUrl)
                                result
                                    .onSuccess {
                                        testSuccess = true
                                        testMessage = context.getString(R.string.api_settings_test_ok)
                                    }
                                    .onFailure {
                                        testSuccess = false
                                        testMessage = context.getString(
                                            R.string.api_settings_test_failed,
                                            humanizeNetworkError(it) ?: it.message.orEmpty(),
                                        )
                                    }
                                testing = false
                            }
                        },
                        enabled = !testing && !saving && draftUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    color = WbBrandOrange,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.padding(start = 8.dp))
                            }
                            Text(
                                text = if (testing) {
                                    stringResource(R.string.api_settings_testing)
                                } else {
                                    stringResource(R.string.api_settings_test_connection)
                                },
                            )
                        }
                    }
                    testMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (testSuccess) {
                                true -> MaterialTheme.colorScheme.primary
                                false -> MaterialTheme.colorScheme.error
                                null -> WbTextMuted
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
