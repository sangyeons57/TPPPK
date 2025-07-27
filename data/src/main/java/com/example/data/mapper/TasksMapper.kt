package com.example.data.mapper

import com.example.data.model.local.TasksEntity
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType

/**
 * TasksEntity와 Task 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object TasksMapper {

    /**
     * Task 도메인 모델을 TasksEntity로 변환
     * @param task 변환할 Task 도메인 모델
     * @param channelId 태스크가 속한 채널 ID
     * @return TasksEntity
     */
    fun toEntity(task: Task, channelId: String): TasksEntity {
        return TasksEntity(
            id = task.id.value,
            channelId = channelId,
            taskType = task.taskType.value,
            status = task.status.value,
            content = task.content.value,
            order = task.order.value,
            checkedBy = task.checkedBy?.value,
            checkedAt = task.checkedAt,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }

    /**
     * TasksEntity를 Task 도메인 모델로 변환
     * @param entity 변환할 TasksEntity
     * @return Task 도메인 모델
     */
    fun toDomain(entity: TasksEntity): Task {
        return Task.fromDataSource(
            id = DocumentId(entity.id),
            taskType = TaskType.fromString(entity.taskType),
            status = TaskStatus.fromString(entity.status),
            content = TaskContent(entity.content),
            order = TaskOrder(entity.order),
            checkedBy = entity.checkedBy?.let { UserId(it) },
            checkedAt = entity.checkedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Task 도메인 모델 리스트를 TasksEntity 리스트로 변환
     * @param tasks 변환할 Task 도메인 모델 리스트
     * @param channelId 태스크들이 속한 채널 ID
     * @return TasksEntity 리스트
     */
    fun toEntityList(tasks: List<Task>, channelId: String): List<TasksEntity> {
        return tasks.map { toEntity(it, channelId) }
    }

    /**
     * TasksEntity 리스트를 Task 도메인 모델 리스트로 변환
     * @param entities 변환할 TasksEntity 리스트
     * @return Task 도메인 모델 리스트
     */
    fun toDomainList(entities: List<TasksEntity>): List<Task> {
        return entities.map { toDomain(it) }
    }
}