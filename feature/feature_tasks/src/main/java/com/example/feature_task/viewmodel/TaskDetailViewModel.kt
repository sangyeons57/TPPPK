package com.example.feature_task.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.extension.getRequiredString
import com.example.core_navigation.destination.RouteArgs
import com.example.domain.provider.task.TaskUseCaseProvider
import com.example.domain.provider.task.TaskUseCases
import com.example.domain.model.vo.DocumentId
import com.example.feature_task.model.TaskUiModel
import com.example.feature_task.mapper.TaskMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 작업 상세 화면 ViewModel
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskUseCaseProvider: TaskUseCaseProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val taskId: String = savedStateHandle.getRequiredString(RouteArgs.TASK_ID)
    private val containerId: String = "default" // Default container ID
    
    private val taskUseCases: TaskUseCases = taskUseCaseProvider.createForTasks(
        projectId = projectId,
        channelId = channelId,
        containerId = containerId
    )
    
    private val _uiState = MutableStateFlow(
        TaskDetailUiState(
            projectId = projectId,
            channelId = channelId,
            taskId = taskId
        )
    )
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadTask()
    }
    
    private fun loadTask() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = taskUseCases.taskRepository.findById(DocumentId(taskId))
            
            result.onSuccess { task ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    task = TaskMapper.toUiModel(task),
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    task = null,
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
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