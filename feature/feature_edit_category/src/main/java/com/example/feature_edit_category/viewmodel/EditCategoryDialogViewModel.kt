package com.example.feature_edit_category.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.core.NavigationManger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EditCategoryDialogViewModel: 카테고리 편집 다이얼로그의 비즈니스 로직을 처리하는 ViewModel
 */
@HiltViewModel
class EditCategoryDialogViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditCategoryDialogUiState())
    val uiState: StateFlow<EditCategoryDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditCategoryDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun initialize(categoryName: String, projectId: String, categoryId: String) {
        _uiState.value = _uiState.value.copy(
            categoryName = categoryName,
            projectId = projectId,
            categoryId = categoryId
        )
    }

    fun onEditCategoryClick() {
        val state = _uiState.value
        if (state.projectId.isNotEmpty() && state.categoryId.isNotEmpty()) {
            navigationManger.navigateToEditCategory(state.projectId, state.categoryId)
        }
        viewModelScope.launch {
            _eventFlow.emit(EditCategoryDialogEvent.DismissDialog)
        }
    }

    fun onCreateChannelClick() {
        viewModelScope.launch {
            // TODO: Navigate to channel creation screen
            _eventFlow.emit(EditCategoryDialogEvent.NavigateToCreateChannel)
            _eventFlow.emit(EditCategoryDialogEvent.DismissDialog)
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            _eventFlow.emit(EditCategoryDialogEvent.DismissDialog)
        }
    }
}

/**
 * EditCategoryDialogUiState: 카테고리 편집 다이얼로그의 UI 상태
 */
data class EditCategoryDialogUiState(
    val categoryName: String = "",
    val projectId: String = "",
    val categoryId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * EditCategoryDialogEvent: 카테고리 편집 다이얼로그에서 발생하는 이벤트
 */
sealed class EditCategoryDialogEvent {
    object DismissDialog : EditCategoryDialogEvent()
    object NavigateToEditCategory : EditCategoryDialogEvent()
    object NavigateToCreateChannel : EditCategoryDialogEvent()
}