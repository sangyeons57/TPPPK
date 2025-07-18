package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.TaskRepository
import com.example.domain.usecase.user.GetCurrentUserUseCase
import javax.inject.Inject

/**
 * 체크박스 태스크의 체크 상태를 토글하는 유스케이스
 */
interface ToggleTaskCheckUseCase {
    suspend operator fun invoke(taskId: String): CustomResult<Unit, Exception>
}

class ToggleTaskCheckUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ToggleTaskCheckUseCase {
    
    override suspend operator fun invoke(taskId: String): CustomResult<Unit, Exception> {
        val task = when (val result = taskRepository.findById(DocumentId(taskId))) {
            is CustomResult.Success -> result.data as Task
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }

        // 체크박스 타입인 경우에만 토글
        if (task.taskType.isCheckbox()) {
            val newTaskType = task.taskType.toggleChecked()
            
            // 체크된 상태로 변경하는 경우 현재 사용자와 시간 저장
            if (newTaskType.isChecked()) {
                val currentUserResult = getCurrentUserUseCase()
                when (currentUserResult) {
                    is CustomResult.Success -> {
                        val currentUserId = UserId(currentUserResult.data.id.internalValue)
                        task.updateTaskType(newTaskType, currentUserId, DateTimeUtil.SERVER_TIMESTAMP_MARKER)
                    }
                    is CustomResult.Failure -> return CustomResult.Failure(currentUserResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(currentUserResult.progress)
                }
            } else {
                // 체크 해제하는 경우 체크 정보 제거
                task.updateTaskType(newTaskType, null, null)
            }
        }

        return when (val result = taskRepository.save(task)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}