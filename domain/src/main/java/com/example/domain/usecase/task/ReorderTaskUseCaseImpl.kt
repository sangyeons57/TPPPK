package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.repository.base.TaskRepository

/**
 * Task 순서를 변경하는 UseCase 구현체
 */
class ReorderTaskUseCaseImpl(
    private val taskRepository: TaskRepository
) : ReorderTaskUseCase {
    
    override suspend fun invoke(
        taskId: String,
        newOrder: Int
    ): CustomResult<Unit, Exception> {
        return try {
            val taskDocumentId = DocumentId(taskId)
            
            // Task를 조회
            val taskResult = taskRepository.findById(taskDocumentId)

            taskResult.suspendSuccessProcess {
                val task = it as Task
                val updatedTask = task.updateOrder(TaskOrder(newOrder))
                taskRepository.save(task)
                Unit
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}