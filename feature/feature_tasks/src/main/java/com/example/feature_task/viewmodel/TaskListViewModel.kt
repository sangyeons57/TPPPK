package com.example.feature_task.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskType
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.auth.AuthSessionUseCases
import com.example.domain_usecase.provider.task.TaskUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectAuthorizationUseCaseProvider
import com.example.domain.model.data.project.RolePermission
import com.example.domain_usecase.provider.task.TaskUseCases
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCases
import com.example.feature_task.mapper.TaskMapper
import com.example.feature_task.model.TaskUiModel
import com.example.orchestrator.SyncManagerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val navigationManger: NavigationManger,
    private val syncManagerFactory: SyncManagerFactory,
    private val projectAuthorizationUseCaseProvider: ProjectAuthorizationUseCaseProvider,
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
    
    private val authSessionUseCases: AuthSessionUseCases = authSessionUseCaseProvider.create()
    private val userUseCases: UserUseCases = userUseCaseProvider.createForUser()

    private val _uiState = MutableStateFlow(
        TaskListUiState(
            projectId = projectId,
            channelId = channelId
        )
    )
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()
    
    init {
        val composed = ChannelId.compose(projectId, channelId)

        // Cache write permission once for this project/channel (read gate is done in Home)
        viewModelScope.launch {
            try {
                val auth =
                    projectAuthorizationUseCaseProvider.createForProject(DocumentId(projectId))
                val canWrite = when (val res = auth.ownerOrPermissionUseCase.invoke(
                    DocumentId(projectId),
                    RolePermission.CHANNEL_WRITE
                )) {
                    is CustomResult.Success -> res.data
                    else -> false
                }
                _uiState.value = _uiState.value.copy(canWrite = canWrite)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(canWrite = false)
            }
        }

        // Kick off incremental sync (push outbox + pull remote) for tasks in this channel
        viewModelScope.launch {
            try {
                val coordinator = syncManagerFactory.forChannel(
                    composed.value,
                    includeMessages = false,
                    includeTasks = true
                )
                coordinator.syncAll()
            } catch (_: Exception) {
                // best-effort
            }
        }
        taskUseCases.observeChannelTasksUseCase(composed)
            .onEach { taskResult ->
                android.util.Log.d(
                    "TaskListViewModel",
                    "observeChannelTasks emitted result: ${taskResult::class.simpleName}"
                )
                if (taskResult is CustomResult.Success) {
                    android.util.Log.d(
                        "TaskListViewModel",
                        "Received ${taskResult.data.size} tasks from repository"
                    )
                    taskResult.data.forEach { task ->
                        android.util.Log.d(
                            "TaskListViewModel",
                            "Task: id=${task.id.value}, content=${task.content.value}, order=${task.order.value}"
                        )
                    }
                }
            }
            .flatMapLatest { taskResult ->
                if (taskResult is CustomResult.Success) {
                    val tasks = taskResult.data
                    val userIds = tasks.mapNotNull { it.checkedBy?.value }.distinct()

                    if (userIds.isEmpty()) {
                        flowOf(Pair(tasks, emptyMap()))
                    } else {
                        userUseCases.getUsersUseCase(userIds).map {
                            val userMap = if (it is CustomResult.Success) it.data.associateBy { user -> user.id } else emptyMap()
                            Pair(tasks, userMap)
                        }
                    }
                } else {
                    flowOf(Pair(emptyList(), emptyMap()))
                }
            }
            .onEach { (tasks, userMap) ->
                android.util.Log.d(
                    "TaskListViewModel",
                    "Processing ${tasks.size} tasks for UI update"
                )
                val uiTasks = tasks.map { task ->
                    val checkedByName = task.checkedBy?.let { userMap[DocumentId.from(it)]?.name?.value }
                    TaskMapper.toUiModel(task, checkedByName)
                }

                android.util.Log.d(
                    "TaskListViewModel",
                    "Updating UI state with ${uiTasks.size} tasks"
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = uiTasks,
                    errorMessage = null
                )
                android.util.Log.d("TaskListViewModel", "UI state updated successfully")
            }
            .launchIn(viewModelScope)
    }
    
    fun createTask(content: String, taskType: TaskType = TaskType.CHECKLIST) {
        viewModelScope.launch {
            android.util.Log.d(
                "TaskListViewModel",
                "Creating task with content='$content', taskType=$taskType"
            )
            val result = taskUseCases.createTaskUseCase.invoke(
                content = content,
                taskType = taskType,
                channelId = ChannelId.compose(projectId, channelId)
            )

            result.onSuccess {
                android.util.Log.d("TaskListViewModel", "Successfully created task")
            }
            result.onFailure { error ->
                android.util.Log.e("TaskListViewModel", "Failed to create task: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun updateTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            android.util.Log.d("TaskListViewModel", "Toggling task check status for taskId=$taskId")
            val result = taskUseCases.toggleTaskCheckUseCase(taskId)

            result.onSuccess {
                android.util.Log.d(
                    "TaskListViewModel",
                    "Successfully toggled task check for taskId=$taskId"
                )
            }
            result.onFailure { error ->
                android.util.Log.e(
                    "TaskListViewModel",
                    "Failed to toggle task check for taskId=$taskId: ${error.message}"
                )
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun editTask(taskId: String, content: String) {
        viewModelScope.launch {
            android.util.Log.d(
                "TaskListViewModel",
                "Editing task taskId=$taskId with content='$content'"
            )
            val result = taskUseCases.updateTaskUseCase(
                taskId = taskId,
                content = content
            )

            result.onSuccess {
                android.util.Log.d("TaskListViewModel", "Successfully edited task taskId=$taskId")
            }
            result.onFailure { error ->
                android.util.Log.e(
                    "TaskListViewModel",
                    "Failed to edit task taskId=$taskId: ${error.message}"
                )
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            android.util.Log.d("TaskListViewModel", "Deleting task taskId=$taskId")
            val result = taskUseCases.deleteTaskUseCase(taskId)

            result.onSuccess {
                android.util.Log.d("TaskListViewModel", "Successfully deleted task taskId=$taskId")
            }
            result.onFailure { error ->
                android.util.Log.e(
                    "TaskListViewModel",
                    "Failed to delete task taskId=$taskId: ${error.message}"
                )
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
    
    // 드래그 중에는 데이터 변경 없이 시각적 피드백만 제공
    // 실제 데이터 변경은 finalizeReorder에서만 수행
    
    fun finalizeReorder(realtimeOrderedTasks: List<TaskUiModel>) {
        viewModelScope.launch {
            try {
                // 드래그 중 이미 적용된 실시간 순서를 그대로 사용
                _uiState.value = _uiState.value.copy(tasks = realtimeOrderedTasks)
                
                // 서버에 새로운 순서 저장 (이미 정렬된 순서를 그대로 저장)
                val composed = ChannelId.compose(projectId, channelId)
                realtimeOrderedTasks.forEachIndexed { index, task ->
                    val result = taskUseCases.moveTaskUseCase(task.id.value, composed, index)
                    when (result) {
                        is CustomResult.Success -> {
                            // 성공 시 계속 진행
                        }
                        is CustomResult.Failure -> {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "서버 동기화 중 오류가 발생했습니다: ${result.error.message}"
                            )
                            return@launch
                        }
                        is CustomResult.Initial -> {
                            // 초기 상태 - 아직 처리되지 않음
                        }
                        is CustomResult.Loading -> {
                            // 로딩 중 - 계속 대기
                        }
                        is CustomResult.Progress -> {
                            // 진행 중 - 계속 대기
                        }
                    }
                }
                
            } catch (e: Exception) {
                // 서버 동기화 실패 시 에러 표시
                _uiState.value = _uiState.value.copy(
                    errorMessage = "서버 동기화 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }
    
    @Deprecated("Use finalizeReorder instead")
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        // 기존 방식: 현재는 사용하지 않음
        // finalizeReorder는 실시간 순서 리스트를 파라미터로 받음
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
    val errorMessage: String? = null,
    val canWrite: Boolean = true
)

