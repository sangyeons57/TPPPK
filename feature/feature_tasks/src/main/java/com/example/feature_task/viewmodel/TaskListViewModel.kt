package com.example.feature_task.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.extension.getRequiredString
import com.example.core_navigation.destination.RouteArgs
import com.example.domain.provider.task.TaskUseCaseProvider
import com.example.domain.provider.task.TaskUseCases
import com.example.domain.model.base.Task
import com.example.feature_task.model.TaskUiModel
import com.example.feature_task.mapper.TaskMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 작업 목록 화면 ViewModel (임시 구현)
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskUseCaseProvider: TaskUseCaseProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val containerId: String = "default" // Default container ID
    
    private val taskUseCases: TaskUseCases = taskUseCaseProvider.createForTasks(
        projectId = projectId,
        channelId = channelId,
        containerId = containerId
    )
    
    private val _uiState = MutableStateFlow(
        TaskListUiState(
            projectId = projectId,
            channelId = channelId
        )
    )
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()
    
    init {
        observeTasks()
    }
    
    private fun observeTasks() {
        taskUseCases.observeTasksUseCase()
            .onEach { result ->
                result.onSuccess { tasks ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = tasks.map { TaskMapper.toUiModel(it) },
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun createTask(title: String, description: String = "") {
        viewModelScope.launch {
            val result = taskUseCases.createTaskUseCase.invoke(
                title = title,
                description = description
            )
            
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun updateTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val result = taskUseCases.updateTaskStatusUseCase.invoke(
                taskId = taskId,
                isCompleted = isCompleted
            )
            
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun startEditingTask(taskId: String) {
        _uiState.value = _uiState.value.copy(
            tasks = _uiState.value.tasks.map { task ->
                if (task.id.value == taskId) {
                    task.toEditingState()
                } else {
                    task
                }
            }
        )
    }
    
    fun updateTaskContent(taskId: String, newContent: String) {
        _uiState.value = _uiState.value.copy(
            tasks = _uiState.value.tasks.map { task ->
                if (task.id.value == taskId) {
                    task.copy(content = com.example.domain.model.vo.task.TaskContent(newContent))
                } else {
                    task
                }
            }
        )
    }
    
    fun saveTaskChanges(taskId: String) {
        val task = _uiState.value.tasks.find { it.id.value == taskId }
        if (task?.isEditing == true) {
            viewModelScope.launch {
                // Check for conflicts before saving
                if (task.hasContentChanged()) {
                    // Set updating state
                    _uiState.value = _uiState.value.copy(
                        tasks = _uiState.value.tasks.map { t ->
                            if (t.id.value == taskId) {
                                t.toUpdatingState()
                            } else {
                                t
                            }
                        }
                    )
                    
                    // Attempt to save
                    val result = taskUseCases.updateTaskUseCase.invoke(
                        taskId = taskId,
                        title = task.title,
                        description = task.description,
                        taskType = task.taskType,
                        status = task.status,
                        order = task.order
                    )
                    
                    result.onSuccess {
                        // Reset to normal state on success
                        _uiState.value = _uiState.value.copy(
                            tasks = _uiState.value.tasks.map { t ->
                                if (t.id.value == taskId) {
                                    t.toNormalState()
                                } else {
                                    t
                                }
                            }
                        )
                    }.onFailure { error ->
                        // Check if it's a conflict error
                        if (error.message?.contains("conflict") == true || error.message?.contains("modified") == true) {
                            _uiState.value = _uiState.value.copy(
                                tasks = _uiState.value.tasks.map { t ->
                                    if (t.id.value == taskId) {
                                        t.toConflictState()
                                    } else {
                                        t
                                    }
                                }
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = error.message,
                                tasks = _uiState.value.tasks.map { t ->
                                    if (t.id.value == taskId) {
                                        t.toNormalState()
                                    } else {
                                        t
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // No changes, just reset to normal state
                    _uiState.value = _uiState.value.copy(
                        tasks = _uiState.value.tasks.map { t ->
                            if (t.id.value == taskId) {
                                t.toNormalState()
                            } else {
                                t
                            }
                        }
                    )
                }
            }
        }
    }
    
    fun cancelEditingTask(taskId: String) {
        _uiState.value = _uiState.value.copy(
            tasks = _uiState.value.tasks.map { task ->
                if (task.id.value == taskId) {
                    task.originalContent?.let { original ->
                        task.copy(
                            content = original,
                            isEditing = false,
                            originalContent = null
                        )
                    } ?: task.toNormalState()
                } else {
                    task
                }
            }
        )
    }
    
    fun resolveConflict(taskId: String, overwrite: Boolean) {
        val task = _uiState.value.tasks.find { it.id.value == taskId }
        if (task?.hasConflict == true) {
            if (overwrite) {
                // Force save with overwrite
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        tasks = _uiState.value.tasks.map { t ->
                            if (t.id.value == taskId) {
                                t.toUpdatingState()
                            } else {
                                t
                            }
                        }
                    )
                    
                    // Force update (implementation depends on use case)
                    val result = taskUseCases.updateTaskUseCase.invoke(
                        taskId = taskId,
                        title = task.title,
                        description = task.description,
                        taskType = task.taskType,
                        status = task.status,
                        order = task.order
                    )
                    
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            tasks = _uiState.value.tasks.map { t ->
                                if (t.id.value == taskId) {
                                    t.toNormalState()
                                } else {
                                    t
                                }
                            }
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message,
                            tasks = _uiState.value.tasks.map { t ->
                                if (t.id.value == taskId) {
                                    t.toNormalState()
                                } else {
                                    t
                                }
                            }
                        )
                    }
                }
            } else {
                // Discard changes and reload data
                _uiState.value = _uiState.value.copy(
                    tasks = _uiState.value.tasks.map { t ->
                        if (t.id.value == taskId) {
                            t.toNormalState()
                        } else {
                            t
                        }
                    }
                )
                // The observeTasks() will automatically reload the latest data
            }
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = taskUseCases.deleteTaskUseCase.invoke(taskId)
            
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
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
 * 작업 목록 UI 상태
 */
data class TaskListUiState(
    val projectId: String = "",
    val channelId: String = "",
    val tasks: List<TaskUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

