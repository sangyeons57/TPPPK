package com.example.data.model.remote

import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Task
import com.example.domain.model.base.TaskContainer
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskContainerOrder
import com.example.domain.model.vo.task.TaskContainerStatus
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 태스크 컨테이너와 태스크 정보를 통합한 DTO 클래스
 * type 필드로 문서 타입을 구분합니다.
 */
data class TaskContainerUnifiedDTO(
    @DocumentId override val id: String = "",
    
    // Type discriminator
    @get:PropertyName(TYPE)
    val type: String = "container", // "container" or "task"
    
    // TaskContainer 전용 필드들
    @get:PropertyName(CONTAINER_ORDER)
    val containerOrder: Int? = null,
    @get:PropertyName(CONTAINER_STATUS)
    val containerStatus: String? = null,
    
    // Task 전용 필드들
    @get:PropertyName(TASK_TYPE)
    val taskType: String? = null,
    @get:PropertyName(TASK_STATUS)
    val taskStatus: String? = null,
    @get:PropertyName(CONTENT)
    val content: String? = null,
    @get:PropertyName(TASK_ORDER)
    val taskOrder: Int? = null,
    
    // 공통 필드들
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = "task_container"
        const val TYPE = "type"
        
        // TaskContainer 필드들
        const val CONTAINER_ORDER = "containerOrder"
        const val CONTAINER_STATUS = "containerStatus"
        
        // Task 필드들
        const val TASK_TYPE = "taskType"
        const val TASK_STATUS = "taskStatus"
        const val CONTENT = "content"
        const val TASK_ORDER = "taskOrder"
        
        // 타입 상수들
        const val TYPE_CONTAINER = "container"
        const val TYPE_TASK = "task"
        
        // 고정된 컨테이너 ID
        const val FIXED_CONTAINER_ID = "container"
    }

    /**
     * DTO를 도메인 모델로 변환
     * type 필드에 따라 적절한 도메인 모델을 반환합니다.
     */
    override fun toDomain(): AggregateRoot {
        return when (type) {
            TYPE_CONTAINER -> toTaskContainer()
            TYPE_TASK -> toTask()
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }
    
    /**
     * TaskContainer 도메인 모델로 변환
     */
    private fun toTaskContainer(): TaskContainer {
        requireNotNull(containerOrder) { "containerOrder is required for TaskContainer" }
        requireNotNull(containerStatus) { "containerStatus is required for TaskContainer" }
        
        return TaskContainer.fromDataSource(
            id = VODocumentId(id),
            order = TaskContainerOrder(containerOrder),
            status = TaskContainerStatus.fromValue(containerStatus),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
    
    /**
     * Task 도메인 모델로 변환
     */
    private fun toTask(): Task {
        requireNotNull(taskType) { "taskType is required for Task" }
        requireNotNull(taskStatus) { "taskStatus is required for Task" }
        requireNotNull(content) { "content is required for Task" }
        requireNotNull(taskOrder) { "taskOrder is required for Task" }
        
        return Task.fromDataSource(
            id = VODocumentId(id),
            taskType = TaskType.fromValue(taskType),
            status = TaskStatus.fromValue(taskStatus),
            content = TaskContent(content),
            order = TaskOrder(taskOrder),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * TaskContainer 도메인 모델을 통합 DTO로 변환하는 확장 함수
 */
fun TaskContainer.toUnifiedDto(): TaskContainerUnifiedDTO {
    return TaskContainerUnifiedDTO(
        id = id.value,
        type = TaskContainerUnifiedDTO.TYPE_CONTAINER,
        containerOrder = order.value,
        containerStatus = status.value,
        createdAt = null,
        updatedAt = null
    )
}

/**
 * Task 도메인 모델을 통합 DTO로 변환하는 확장 함수
 */
fun Task.toUnifiedDto(): TaskContainerUnifiedDTO {
    return TaskContainerUnifiedDTO(
        id = id.value,
        type = TaskContainerUnifiedDTO.TYPE_TASK,
        taskType = taskType.value,
        taskStatus = status.value,
        content = content.value,
        taskOrder = order.value,
        createdAt = null,
        updatedAt = null
    )
}