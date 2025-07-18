package com.example.feature_task.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskType
import com.example.domain.provider.task.TaskUseCaseProvider
import com.example.domain.provider.task.TaskUseCases
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.domain.provider.user.UserUseCases
import com.example.feature_task.mapper.TaskMapper
import com.example.feature_task.model.TaskUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 작업 목록 화면 ViewModel
 * Google Keep 스타일의 간단한 메모 리스트
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskUseCaseProvider: TaskUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val navigationManger: NavigationManger,
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
    
    private val userUseCases: UserUseCases = userUseCaseProvider.createForUser()

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
                    viewModelScope.launch {
                        // 사용자 이름을 가져와서 TaskUiModel에 추가
                        val tasksWithUserNames = tasks.map { task ->
                            val checkedByName = task.checkedBy?.let { userId ->
                                when (val userResult = userUseCases.getUserByIdUseCase(DocumentId(userId.internalValue))) {
                                    is CustomResult.Success -> userResult.data.name.value
                                    else -> null
                                }
                            }
                            TaskMapper.toUiModel(task, checkedByName)
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            tasks = tasksWithUserNames,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun createTask(content: String, taskType: TaskType = TaskType.CHECKLIST) {
        viewModelScope.launch {
            val result = taskUseCases.createTaskUseCase.invoke(
                content = content,
                taskType = taskType
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
            val result = taskUseCases.toggleTaskCheckUseCase(taskId)
            
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun editTask(taskId: String, content: String) {
        viewModelScope.launch {
            val result = taskUseCases.updateTaskUseCase(
                taskId = taskId,
                content = content
            )
            
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
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
    
    fun navigateBack() {
        navigationManger.navigateBack()
    }
    
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                // 현재 Task 목록 가져오기
                val currentTasks = uiState.value.tasks.toMutableList()
                
                // 범위 검증
                if (fromIndex < 0 || fromIndex >= currentTasks.size || 
                    toIndex < 0 || toIndex >= currentTasks.size || 
                    fromIndex == toIndex) {
                    return@launch
                }
                
                // 스와핑 방식으로 두 아이템의 위치만 바꾸기
                val temp = currentTasks[fromIndex]
                currentTasks[fromIndex] = currentTasks[toIndex]
                currentTasks[toIndex] = temp
                
                // 임시로 UI 업데이트 (즉시 반영)
                _uiState.value = _uiState.value.copy(tasks = currentTasks)
                
                // 서버에 순서 업데이트 - 변경된 두 아이템의 순서만 업데이트
                val task1 = currentTasks[fromIndex]
                val task2 = currentTasks[toIndex]
                
                // 첫 번째 아이템 순서 업데이트
                val result1 = taskUseCases.reorderTaskUseCase(task1.id.value, fromIndex)
                result1.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "순서 변경 중 오류가 발생했습니다: ${error.message}"
                    )
                }
                
                // 두 번째 아이템 순서 업데이트
                val result2 = taskUseCases.reorderTaskUseCase(task2.id.value, toIndex)
                result2.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "순서 변경 중 오류가 발생했습니다: ${error.message}"
                    )
                }
                
            } catch (e: Exception) {
                // 전체 실패 시 에러 표시
                _uiState.value = _uiState.value.copy(
                    errorMessage = "순서 변경 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
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

