package com.example.data.model.remote

import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 태스크 정보를 나타내는 DTO 클래스
 */
data class TaskDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(TASK_TYPE)
    val taskType: String = "general",
    @get:PropertyName(STATUS)
    val status: String = "pending",
    @get:PropertyName(CONTENT)
    val content: String = "",
    @get:PropertyName(ORDER)
    val order: Int = 0,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Task.COLLECTION_NAME
        const val TASK_TYPE = Task.KEY_TASK_TYPE
        const val STATUS = Task.KEY_STATUS
        const val CONTENT = Task.KEY_CONTENT
        const val ORDER = Task.KEY_ORDER
    }

    /**
     * DTO를 도메인 모델로 변환
     * @return Task 도메인 모델
     */
    override fun toDomain(): Task {
        return Task.fromDataSource(
            id = VODocumentId(id),
            taskType = TaskType.fromValue(taskType),
            status = TaskStatus.fromValue(status),
            content = TaskContent(content),
            order = TaskOrder(order),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * Task 도메인 모델을 DTO로 변환하는 확장 함수
 * @return TaskDTO 객체
 */
fun Task.toDto(): TaskDTO {
    return TaskDTO(
        id = id.value,
        taskType = taskType.value,
        status = status.value,
        content = content.value,
        order = order.value,
        createdAt = null,
        updatedAt = null
    )
}