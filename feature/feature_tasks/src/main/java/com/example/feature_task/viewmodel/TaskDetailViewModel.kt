package com.example.feature_task.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.extension.getRequiredString
import com.example.core_navigation.destination.RouteArgs
import com.example.domain.provider.task.TaskUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 작업 상세 화면 ViewModel (임시 구현)
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskUseCaseProvider: TaskUseCaseProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val taskId: String = savedStateHandle.getRequiredString(RouteArgs.TASK_ID)
    
    private val _uiState = MutableStateFlow(
        TaskDetailUiState(
            projectId = projectId,
            channelId = channelId,
            taskId = taskId
        )
    )
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadTaskDetail()
    }
    
    private fun loadTaskDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: 실제 작업 상세 로딩 로직 구현
            // val taskUseCases = taskUseCaseProvider.createForTasks()
            // val result = taskUseCases.getTaskDetailUseCase(taskId)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                task = null // 임시로 null
            )
        }
    }
}

/**
 * 작업 상세 UI 상태
 */
data class TaskDetailUiState(
    val projectId: String = "",
    val channelId: String = "",
    val taskId: String = "",
    val task: TaskUiModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)