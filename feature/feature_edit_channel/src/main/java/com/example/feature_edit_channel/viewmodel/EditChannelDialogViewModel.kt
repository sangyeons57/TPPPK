package com.example.feature_edit_channel.viewmodel

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
 * EditChannelDialogViewModel: 채널 편집 다이얼로그의 비즈니스 로직을 처리하는 ViewModel
 */
@HiltViewModel
class EditChannelDialogViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditChannelDialogUiState())
    val uiState: StateFlow<EditChannelDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditChannelDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun initialize(channelName: String, projectId: String, channelId: String) {
        _uiState.value = _uiState.value.copy(
            channelName = channelName,
            projectId = projectId,
            channelId = channelId
        )
    }

    fun onEditChannelClick() {
        val state = _uiState.value
        
        // Validate required parameters before navigation
        if (state.projectId.isNotEmpty() && state.channelId.isNotEmpty()) {
            navigationManger.navigateToEditChannel(state.projectId, state.channelId)
        }

        viewModelScope.launch {
            _eventFlow.emit(EditChannelDialogEvent.DismissDialog)
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            _eventFlow.emit(EditChannelDialogEvent.DismissDialog)
        }
    }
}

/**
 * EditChannelDialogUiState: 채널 편집 다이얼로그의 UI 상태
 */
data class EditChannelDialogUiState(
    val channelName: String = "",
    val projectId: String = "",
    val channelId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * EditChannelDialogEvent: 채널 편집 다이얼로그에서 발생하는 이벤트
 */
sealed class EditChannelDialogEvent {
    object DismissDialog : EditChannelDialogEvent()
    object NavigateToEditChannel : EditChannelDialogEvent()
}