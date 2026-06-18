package com.warmbridge.demo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warmbridge.demo.data.local.ChildLastShare
import com.warmbridge.demo.data.local.ChildShareLocalStore
import com.warmbridge.demo.data.local.DemoShareStatus
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChildRecommendItem(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
)

data class ChildRecentShareUi(
    val title: String,
    val note: String,
    val timeLabel: String,
)

data class ChildHomeContent(
    val demoFamilyLines: List<String>,
    val recentShare: ChildRecentShareUi?,
    val recommendations: List<ChildRecommendItem>,
)

sealed interface ChildHomeUiState {
    data object Loading : ChildHomeUiState
    data class Content(val data: ChildHomeContent) : ChildHomeUiState
    data class Error(val message: String) : ChildHomeUiState
}

class ChildHomeViewModel(
    private val shareStore: ChildShareLocalStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChildHomeUiState>(ChildHomeUiState.Loading)
    val uiState: StateFlow<ChildHomeUiState> = _uiState.asStateFlow()

    private var cachedLastShare: ChildLastShare? = null

    init {
        viewModelScope.launch {
            shareStore.lastShare.collect { last ->
                cachedLastShare = last
                loadContent(last)
            }
        }
    }

    fun retry() {
        viewModelScope.launch { loadContent(cachedLastShare) }
    }

    private suspend fun loadContent(lastShare: ChildLastShare?) {
        _uiState.value = ChildHomeUiState.Loading
        try {
            val trendItems = NetworkModule.api.feed(channel = "trend").items
            val recommendations = trendItems.take(2).map { it.toRecommendItem() }
            _uiState.value = ChildHomeUiState.Content(
                ChildHomeContent(
                    demoFamilyLines = DemoShareStatus.familyStatusLines,
                    recentShare = lastShare?.toRecentUi(),
                    recommendations = recommendations,
                ),
            )
        } catch (e: Exception) {
            _uiState.value = ChildHomeUiState.Error(
                humanizeNetworkError(e) ?: "加载失败，请检查网络与后端地址。",
            )
        }
    }
}

private fun FeedItemDto.toRecommendItem() = ChildRecommendItem(
    id = id,
    title = title,
    summary = summary,
    url = url,
    source = source,
)

private fun ChildLastShare.toRecentUi(): ChildRecentShareUi {
    val title = titleHint.ifBlank { note.ifBlank { url } }
    return ChildRecentShareUi(
        title = title,
        note = note.ifBlank { "已分享给父母" },
        timeLabel = formatShareTime(sharedAtMillis),
    )
}

private fun formatShareTime(millis: Long): String {
    if (millis <= 0L) return ""
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))
}
