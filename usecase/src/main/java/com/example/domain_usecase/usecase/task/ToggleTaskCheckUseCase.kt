package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Task
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.TaskRepository
import javax.inject.Inject

/**
 * 체크박스 태스크의 체크 상태를 토글하는 유스케이스
 */
interface ToggleTaskCheckUseCase {
    suspend operator fun invoke(taskId: String): CustomResult<Unit, Exception>
}

class ToggleTaskCheckUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
) : ToggleTaskCheckUseCase {
    
    override suspend operator fun invoke(taskId: String): CustomResult<Unit, Exception> {
        return try {
            android.util.Log.d("ToggleTaskCheckUseCase", "Starting toggle for taskId=$taskId")

            val task = when (val result = taskRepository.findById(DocumentId(taskId))) {
                is CustomResult.Success -> result.data as Task
                is CustomResult.Failure -> {
                    android.util.Log.e(
                        "ToggleTaskCheckUseCase",
                        "Failed to find task: ${result.error.message}"
                    )
                    return CustomResult.Failure(result.error)
                }

                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(result.progress)
            }

            android.util.Log.d(
                "ToggleTaskCheckUseCase",
                "Found task: id=${task.id.value}, currentTaskType=${task.taskType.value}"
            )

            // 체크박스 타입인 경우에만 토글
            if (task.taskType.isCheckbox()) {
                val newTaskType = task.taskType.toggleChecked()
                android.util.Log.d(
                    "ToggleTaskCheckUseCase",
                    "Toggling from ${task.taskType.value} to ${newTaskType.value}"
                )

                // 체크된 상태로 변경하는 경우 현재 사용자와 시간 저장
                if (newTaskType.isChecked()) {
                    android.util.Log.d(
                        "ToggleTaskCheckUseCase",
                        "Setting task as checked - getting current user"
                    )
                    val currentUserResult = authRepository.getCurrentUserSession()
                    when (currentUserResult) {
                        is CustomResult.Success -> {
                            val currentUserId = UserId(currentUserResult.data.userId.internalValue)
                            android.util.Log.d(
                                "ToggleTaskCheckUseCase",
                                "Setting checked by user: $currentUserId"
                            )
                            task.updateTaskType(
                                newTaskType,
                                currentUserId,
                                DateTimeUtil.nowInstant()
                            )
                        }

                        is CustomResult.Failure -> {
                            android.util.Log.e(
                                "ToggleTaskCheckUseCase",
                                "Failed to get current user: ${currentUserResult.error.message}"
                            )
                            return CustomResult.Failure(currentUserResult.error)
                        }

                        is CustomResult.Initial -> return CustomResult.Initial
                        is CustomResult.Loading -> return CustomResult.Loading
                        is CustomResult.Progress -> return CustomResult.Progress(currentUserResult.progress)
                    }
                } else {
                    // 체크 해제하는 경우 체크 정보 제거
                    android.util.Log.d(
                        "ToggleTaskCheckUseCase",
                        "Unchecking task - removing check info"
                    )
                    task.updateTaskType(newTaskType, null, null)
                }
            } else {
                android.util.Log.d(
                    "ToggleTaskCheckUseCase",
                    "Task is not checkbox type, skipping toggle"
                )
            }

            val updated = Task.fromDataSource(
                id = task.id,
                channelId = task.channelId,
                taskType = task.taskType,
                status = task.status,
                content = task.content,
                order = task.order,
                checkedBy = task.checkedBy,
                checkedAt = task.checkedAt,
                createdAt = task.createdAt,
                updatedAt = DateTimeUtil.nowInstant()
            )

            android.util.Log.d(
                "ToggleTaskCheckUseCase",
                "Calling taskRepository.updateTask for updated task"
            )
            taskRepository.updateTask(updated)
            android.util.Log.d(
                "ToggleTaskCheckUseCase",
                "Successfully completed toggle for taskId=$taskId"
            )

            CustomResult.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ToggleTaskCheckUseCase", "Failed to toggle taskId=$taskId", e)
            CustomResult.Failure(e)
        }
    }
}
