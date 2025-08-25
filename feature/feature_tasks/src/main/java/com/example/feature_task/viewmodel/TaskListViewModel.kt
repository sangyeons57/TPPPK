package com.example.feature_task.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskType
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.auth.AuthSessionUseCases
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.task.TaskUseCaseProvider
import com.example.domain_usecase.provider.task.TaskUseCases
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCases
import com.example.feature_task.mapper.TaskMapper
import com.example.feature_task.model.TaskUiModel
import com.example.orchestrator.SyncManagerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
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
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskUseCaseProvider: TaskUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val navigationManger: NavigationManger,
    private val syncManagerFactory: SyncManagerFactory,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
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

    // Throttled sync trigger to coalesce rapid edits
    private val syncRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Auto-sync timer job
    private var autoSyncJob: Job? = null
    private var isAutoSyncActive = false

    companion object {
        private const val AUTO_SYNC_INTERVAL_MS = 60_000L // 1 minute
    }
    
    init {
        val composed = ChannelId.compose(projectId, channelId)

        // Cache write permission once for this project/channel (read gate is done in Home)
        viewModelScope.launch {
            try {
                val memberUseCases =
                    projectMemberUseCaseProvider.createForProject(DocumentId(projectId))
                val canWrite = when (val res = memberUseCases.ownerOrPermissionUseCase.invoke(
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
                    composed,
                    includeTasks = true
                )
                coordinator.syncAll()
            } catch (_: Exception) {
                // best-effort
            }
        }
        // Consume sync requests with debounce to avoid spamming
        syncRequests
            .debounce(300)
            .onEach {
                try {
                    val coordinator = syncManagerFactory.forChannel(
                        composed,
                        includeTasks = true
                    )
                    coordinator.syncAll()
                } catch (_: Exception) {
                    // ignore
                }
            }
            .launchIn(viewModelScope)
        taskUseCases.observeChannelTasksUseCase(composed)
            .flatMapLatest { taskResult ->
                // taskResult가 성공 상태가 아니거나 데이터가 비어있으면, 사용자 정보를 조회할 필요 없이 그대로 전달합니다.
                if (taskResult !is CustomResult.Success || taskResult.data.isEmpty()) {
                    return@flatMapLatest flowOf(Pair(taskResult, emptyMap()))
                }

                // 태스크 목록이 성공적으로 로드되었으면, 완료한 사용자 ID들을 추출합니다.
                val tasks = taskResult.data
                val userIds = tasks.mapNotNull { it.checkedBy?.value }.distinct()

                // 사용자 ID가 없으면 사용자 정보를 조회할 필요가 없습니다.
                if (userIds.isEmpty()) {
                    flowOf(Pair(taskResult, emptyMap()))
                } else {
                    // 사용자 ID로 사용자 정보를 조회한 후, 원래의 taskResult와 짝을 지어(Pair) 반환합니다.
                    userUseCases.getUsersUseCase(userIds).map { userResult ->
                        val userMap = if (userResult is CustomResult.Success) {
                            userResult.data.associateBy { it.id }
                        } else {
                            // 사용자 정보 조회를 실패하면 비어있는 맵을 사용합니다.
                            emptyMap()
                        }
                        Pair(taskResult, userMap)
                    }
                }
            }
            .map { (taskResult, userMap) ->
                // 태스크 결과와 사용자 정보를 바탕으로 최종 UI State를 만듭니다.
                // 이 map 블록에서는 UI 상태를 변환하는 책임만 가집니다.
                when (taskResult) {
                    is CustomResult.Success -> {
                        val uiTasks = taskResult.data
                            .filter { it.deletedAt == null }
                            .map { task ->
                            val checkedByName =
                                task.checkedBy?.let { userMap[DocumentId.from(it)]?.name?.value }
                            TaskMapper.toUiModel(task, checkedByName)
                        }
                        _uiState.value.copy(
                            isLoading = false,
                            tasks = uiTasks,
                            errorMessage = null
                        )
                    }

                    is CustomResult.Failure -> {
                        _uiState.value.copy(
                            isLoading = false,
                            // 에러 발생 시 기존 태스크 목록은 유지하면서 에러 메시지를 표시합니다.
                            errorMessage = taskResult.error.message ?: "태스크를 불러오는데 실패했습니다."
                        )
                    }

                    is CustomResult.Loading -> {
                        _uiState.value.copy(isLoading = true)
                    }
                    // CustomResult.Initial 등 다른 상태는 현재 상태를 그대로 유지합니다.
                    else -> _uiState.value
                }
            }
            .onEach { newState ->
                // 최종적으로 변환된 UI State를 실제 StateFlow에 적용합니다.
                Log.d("TaskListViewModel", "Updating UI state(count): ${newState.tasks.size}")
                _uiState.value = newState
            }
            .launchIn(viewModelScope)

        // Start auto-sync timer
        startAutoSync()
    }
    
    fun createTask(content: String, taskType: TaskType = TaskType.CHECKLIST) {
        viewModelScope.launch {
            Log.d(
                "TaskListViewModel",
                "Creating task with content='$content', taskType=$taskType"
            )
            val result = taskUseCases.createTaskUseCase.invoke(
                content = content,
                taskType = taskType,
                channelId = ChannelId.compose(projectId, channelId)
            )

            result.onSuccess {
                Log.d("TaskListViewModel", "Successfully created task")
                syncRequests.tryEmit(Unit)
            }
            result.onFailure { error ->
                Log.e("TaskListViewModel", "Failed to create task: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun updateTaskStatus(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            Log.d("TaskListViewModel", "Toggling task check status for taskId=$taskId")
            val result = taskUseCases.toggleTaskCheckUseCase(taskId)

            result.onSuccess {
                Log.d(
                    "TaskListViewModel",
                    "Successfully toggled task check for taskId=$taskId"
                )
                syncRequests.tryEmit(Unit)
            }
            result.onFailure { error ->
                Log.e(
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
            Log.d(
                "TaskListViewModel",
                "Editing task taskId=$taskId with content='$content'"
            )
            val result = taskUseCases.updateTaskUseCase(
                taskId = taskId,
                content = content
            )

            result.onSuccess {
                Log.d("TaskListViewModel", "Successfully edited task taskId=$taskId")
                syncRequests.tryEmit(Unit)
            }
            result.onFailure { error ->
                Log.e(
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
            Log.d("TaskListViewModel", "Deleting task taskId=$taskId")
            val result = taskUseCases.deleteTaskUseCase(taskId)

            result.onSuccess {
                Log.d("TaskListViewModel", "Successfully deleted task taskId=$taskId")
                syncRequests.tryEmit(Unit)
            }
            result.onFailure { error ->
                Log.e(
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
                // Trigger a single sync after batch reorder completes
                syncRequests.tryEmit(Unit)

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

    /**
     * 자동 동기화 시작
     */
    private fun startAutoSync() {
        if (isAutoSyncActive) return

        isAutoSyncActive = true
        autoSyncJob = viewModelScope.launch {
            Log.d("TaskListViewModel", "Auto-sync started (interval: ${AUTO_SYNC_INTERVAL_MS}ms)")

            while (isAutoSyncActive) {
                delay(AUTO_SYNC_INTERVAL_MS)

                if (isAutoSyncActive) {
                    Log.d("TaskListViewModel", "Triggering auto-sync")
                    syncRequests.tryEmit(Unit)
                }
            }
        }
    }

    /**
     * 자동 동기화 중지
     */
    fun stopAutoSync() {
        if (!isAutoSyncActive) return

        Log.d("TaskListViewModel", "Auto-sync stopped")
        isAutoSyncActive = false
        autoSyncJob?.cancel()
        autoSyncJob = null
    }

    /**
     * 자동 동기화 재시작
     */
    fun resumeAutoSync() {
        if (isAutoSyncActive) return

        Log.d("TaskListViewModel", "Auto-sync resumed")
        // Immediately sync once when resuming, then start the timer
        syncRequests.tryEmit(Unit)
        startAutoSync()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoSync()
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
