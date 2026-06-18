package com.warmbridge.demo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ParentHomeUiState {
    data object Loading : ParentHomeUiState

    data class Content(
        val childRecommend: List<FeedItemDto>,
        val curated: List<FeedItemDto>,
    ) : ParentHomeUiState

    data object Empty : ParentHomeUiState

    data class Error(val message: String) : ParentHomeUiState
}

class ParentHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ParentHomeUiState>(ParentHomeUiState.Loading)
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    private var lastSelectedTag: String? = null

    fun load(selectedTag: String?) {
        lastSelectedTag = selectedTag
        viewModelScope.launch {
            _uiState.value = ParentHomeUiState.Loading
            try {
                val content = fetchHomeContent(selectedTag)
                _uiState.value = when {
                    content.childRecommend.isEmpty() && content.curated.isEmpty() ->
                        ParentHomeUiState.Empty
                    else -> content
                }
            } catch (e: Exception) {
                _uiState.value = ParentHomeUiState.Error(
                    humanizeNetworkError(e) ?: "加载失败，请检查网络与后端地址。",
                )
            }
        }
    }

    fun retry() {
        load(lastSelectedTag)
    }

    private suspend fun fetchHomeContent(selectedTag: String?): ParentHomeUiState.Content =
        coroutineScope {
            val childDeferred = async {
                runCatching { NetworkModule.api.feed(channel = "child").items }.getOrDefault(emptyList())
            }
            val trendDeferred = async {
                runCatching { NetworkModule.api.feed(channel = "trend").items }.getOrDefault(emptyList())
            }
            val tagDeferred = async {
                runCatching {
                    NetworkModule.api.feed(tag = selectedTag, channel = null).items
                }.getOrDefault(emptyList())
            }

            val childItems = childDeferred.await()
            val trendItems = trendDeferred.await()
            val tagItems = tagDeferred.await()

            val childRecommend = childItems.take(2)
            val childIds = childRecommend.map { it.id }.toSet()

            val curated = buildList {
                addAll(trendItems.filter { it.id !in childIds })
                tagItems.filter { it.id !in childIds && none { c -> c.id == it.id } }
                    .forEach { add(it) }
            }.take(3)

            ParentHomeUiState.Content(
                childRecommend = childRecommend,
                curated = curated,
            )
        }
}
