package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.util.buildImageUploadPart
import com.warmbridge.demo.util.copyUriToSessionCover
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageExplainScreen(
    onBack: () -> Unit,
    onDoneToDetail: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var pickedLabel by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val pick = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        pendingUri = uri
        err = null
        pickedLabel = uri?.let { u ->
            u.lastPathSegment?.takeIf { it.length < 80 && !it.all { c -> c.isDigit() } }
                ?: context.getString(R.string.image_explain_pick)
        }
    }

    fun upload(uri: Uri) {
        scope.launch {
            loading = true
            err = null
            try {
                val part = withContext(Dispatchers.IO) {
                    buildImageUploadPart(context, uri)
                }
                val resp = NetworkModule.api.imageExplain(part)
                copyUriToSessionCover(context, resp.itemId, uri)
                onDoneToDetail(resp.itemId)
            } catch (e: Exception) {
                err = humanizeNetworkError(e) ?: context.getString(R.string.image_upload_fail)
            } finally {
                loading = false
            }
        }
    }

    WarmToolScreenScaffold(
        title = stringResource(R.string.media_image_title),
        onNavigate = onBack,
        intro = stringResource(R.string.image_explain_intro),
        primaryLabel = stringResource(R.string.image_explain_upload),
        onPrimaryClick = {
            pendingUri?.let { upload(it) }
        },
        primaryEnabled = !loading && pendingUri != null,
        navigationEnabled = !loading,
        footerHint = stringResource(R.string.image_explain_legal),
        statusContent = {
            if (loading) {
                WarmLoadingContent(message = stringResource(R.string.image_uploading))
                Spacer(Modifier.height(12.dp))
            }
            err?.let { message ->
                WarmStatusBanner(message = message, type = WarmStatusBannerType.Error)
                Spacer(Modifier.height(12.dp))
            }
        },
    ) {
        OutlinedButton(
            onClick = {
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            enabled = !loading,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, WbBrandOrange),
        ) {
            Icon(Icons.Filled.Image, contentDescription = stringResource(R.string.cd_pick_image), modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.image_explain_pick))
        }
        pickedLabel?.let { label ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.image_selected, label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
