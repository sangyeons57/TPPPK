package com.example.feature_edit_channel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReorderItem {
    val order: Int

    data class CategoryItem(val category: Category) : ReorderItem {
        override val order: Int get() = category.order.value
    }

    data class ChannelItem(val channel: ProjectChannel) : ReorderItem {
        override val order: Int get() = channel.order.value
    }
}

data class ChannelReorderTestUiState(
    val items: List<ReorderItem> = emptyList(),
    val currentCategory: Category? = null,
    val categoryChannels: List<ProjectChannel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChannelReorderTestViewModel @Inject constructor(
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelReorderTestUiState())
    val uiState: StateFlow<ChannelReorderTestUiState> = _uiState.asStateFlow()

    private var categoryChannelMap: Map<Category, List<ProjectChannel>> = emptyMap()

    fun initialize(projectId: String) {
        viewModelScope.launch {
            val useCases = projectStructureUseCaseProvider.createForProject(DocumentId(projectId))
            useCases.getProjectStructureUseCase(DocumentId(projectId)).collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        categoryChannelMap = result.data.categoryChannelMap
                        val items = buildList {
                            categoryChannelMap.keys.forEach { add(ReorderItem.CategoryItem(it)) }
                            result.data.directChannels.forEach { add(ReorderItem.ChannelItem(it)) }
                        }.sortedBy { it.order }
                        _uiState.update { it.copy(items = items, isLoading = false, error = null) }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                    }
                    is CustomResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    else -> {}
                }
            }
        }
    }

    fun onCategorySelected(item: ReorderItem.CategoryItem) {
        val channels = categoryChannelMap[item.category].orEmpty().sortedBy { it.order.value }
        _uiState.update { it.copy(currentCategory = item.category, categoryChannels = channels) }
    }

    fun onBackToRoot() {
        _uiState.update { it.copy(currentCategory = null, categoryChannels = emptyList()) }
    }

    fun moveItemUp(index: Int) {
        val list = _uiState.value.items.toMutableList()
        if (index <= 0 || index >= list.size) return
        val tmp = list[index - 1]
        list[index - 1] = list[index]
        list[index] = tmp
        _uiState.update { it.copy(items = list) }
    }

    fun moveItemDown(index: Int) {
        val list = _uiState.value.items.toMutableList()
        if (index < 0 || index >= list.lastIndex) return
        val tmp = list[index + 1]
        list[index + 1] = list[index]
        list[index] = tmp
        _uiState.update { it.copy(items = list) }
    }

    fun moveChannelUp(index: Int) {
        val list = _uiState.value.categoryChannels.toMutableList()
        if (index <= 0 || index >= list.size) return
        val tmp = list[index - 1]
        list[index - 1] = list[index]
        list[index] = tmp
        _uiState.update { it.copy(categoryChannels = list) }
    }

    fun moveChannelDown(index: Int) {
        val list = _uiState.value.categoryChannels.toMutableList()
        if (index < 0 || index >= list.lastIndex) return
        val tmp = list[index + 1]
        list[index + 1] = list[index]
        list[index] = tmp
        _uiState.update { it.copy(categoryChannels = list) }
    }
}

