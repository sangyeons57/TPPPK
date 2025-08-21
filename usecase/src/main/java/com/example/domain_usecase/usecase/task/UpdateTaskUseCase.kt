package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Task
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskContent
import com.example.domain.vo.task.TaskOrder
import com.example.domain.vo.task.TaskStatus
import com.example.domain.vo.task.TaskType
import com.example.domain_repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크를 업데이트하는 유스케이스
 */
interface UpdateTaskUseCase {
    suspend operator fun invoke(
        taskId: String,
        content: String? = null,
        taskType: TaskType? = null,
        status: TaskStatus? = null,
        order: TaskOrder? = null
    ): CustomResult<Unit, Exception>
}

class UpdateTaskUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : UpdateTaskUseCase {
    
    override suspend operator fun invoke(
        taskId: String,
        content: String?,
        taskType: TaskType?,
        status: TaskStatus?,
        order: TaskOrder?
    ): CustomResult<Unit, Exception> {
        return try {
            android.util.Log.d(
                "UpdateTaskUseCase",
                "Starting update for taskId=$taskId, content=$content, taskType=$taskType, status=$status, order=$order"
            )

            val task = when (val result = taskRepository.findById(DocumentId(taskId))) {
                is CustomResult.Success -> result.data
                is CustomResult.Failure -> {
                    android.util.Log.e(
                        "UpdateTaskUseCase",
                        "Failed to find task: ${result.error.message}"
                    )
                    return CustomResult.Failure(result.error)
                }

                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(result.progress)
            }

            android.util.Log.d(
                "UpdateTaskUseCase",
                "Found task: id=${task.id.value}, currentContent=${task.content.value}"
            )

            // Apply changes, then reconstitute with refreshed updatedAt for sync ordering
            taskType?.let {
                android.util.Log.d(
                    "UpdateTaskUseCase",
                    "Updating taskType from ${task.taskType.value} to ${it.value}"
                )
                task.updateTaskType(it)
            }
            status?.let {
                android.util.Log.d(
                    "UpdateTaskUseCase",
                    "Updating status from ${task.status.value} to ${it.value}"
                )
                task.updateStatus(it)
            }
            order?.let {
                android.util.Log.d(
                    "UpdateTaskUseCase",
                    "Updating order from ${task.order.value} to ${it.value}"
                )
                task.updateOrder(it)
            }
            content?.let {
                android.util.Log.d(
                    "UpdateTaskUseCase",
                    "Updating content from ${task.content.value} to $it"
                )
                task.updateContent(TaskContent(it))
            }

            val updated = Task.fromDataSource(
                id = task.id,
                channelId = task.channelId,
                taskType = task.taskType,
                status = task.status,
                content = task.content,
                order = task.order,  // 🔥 Preserve original order - don't change position
                checkedBy = task.checkedBy,
                checkedAt = task.checkedAt,
                createdAt = task.createdAt,  // 🔥 Preserve original createdAt
                updatedAt = DateTimeUtil.nowInstant()  // Only update timestamp
            )

            android.util.Log.d(
                "UpdateTaskUseCase",
                "Calling taskRepository.updateTask for updated task"
            )
            taskRepository.updateTask(updated)
            android.util.Log.d(
                "UpdateTaskUseCase",
                "Successfully completed update for taskId=$taskId"
            )

            CustomResult.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UpdateTaskUseCase", "Failed to update taskId=$taskId", e)
            CustomResult.Failure(e)
        }
    }
}
