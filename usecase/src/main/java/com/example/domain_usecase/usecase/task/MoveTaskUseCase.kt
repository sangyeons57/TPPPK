package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Task
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskOrder
import com.example.domain_repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크를 다른 채널로 이동 (필요 시 순서 변경)
 * SSOT: Room 선저장 + Outbox UPSERT 처리
 */
interface MoveTaskUseCase {
    suspend operator fun invoke(
        taskId: String,
        toChannelId: ChannelId,
        newOrder: Int? = null
    ): CustomResult<Unit, Exception>
}

class MoveTaskUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : MoveTaskUseCase {
    override suspend fun invoke(
        taskId: String,
        toChannelId: ChannelId,
        newOrder: Int?
    ): CustomResult<Unit, Exception> {
        // 1) 로컬에서 태스크 로드
        val current = when (val res = taskRepository.findById(DocumentId(taskId))) {
            is CustomResult.Success -> res.data
            is CustomResult.Failure -> return CustomResult.Failure(res.error)
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Progress -> return CustomResult.Progress(res.progress)
        }

        // 2) 새 채널로 재구성 (도메인 Task는 channelId 변경 메서드가 없으므로 재구성)
        val moved = Task.fromDataSource(
            id = current.id,
            channelId = DocumentId(toChannelId.value),
            taskType = current.taskType,
            status = current.status,
            content = current.content,
            order = newOrder?.let { TaskOrder(it) } ?: current.order,
            checkedBy = current.checkedBy,
            checkedAt = current.checkedAt,
            createdAt = current.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )

        // 3) SSOT 저장 (Room upsert) + Outbox UPSERT enq
        taskRepository.addTask(moved)
        return CustomResult.Success(Unit)
    }
}
