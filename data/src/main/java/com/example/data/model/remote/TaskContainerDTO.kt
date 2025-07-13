package com.example.data.model.remote

import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.TaskContainer
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.domain.model.vo.task.TaskContainerOrder
import com.example.domain.model.vo.task.TaskContainerStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 태스크 컨테이너 정보를 나타내는 DTO 클래스
 * 통합된 task_container collection에서 type="container"로 저장됩니다.
 */
data class TaskContainerDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(TYPE)
    val type: String = TYPE_CONTAINER,
    @get:PropertyName(ORDER)
    val order: Int = 0,
    @get:PropertyName(STATUS)
    val status: TaskContainerStatus = TaskContainerStatus.ACTIVE,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = TaskContainer.COLLECTION_NAME
        const val TYPE = "type"
        const val ORDER = TaskContainer.KEY_ORDER
        const val STATUS = TaskContainer.KEY_STATUS
        const val TYPE_CONTAINER = "container"
        
        /**
         * 고정된 컨테이너 ID
         */
        const val FIXED_CONTAINER_ID = TaskContainer.FIXED_CONTAINER_ID
    }

    /**
     * DTO를 도메인 모델로 변환
     * @return TaskContainer 도메인 모델
     */
    override fun toDomain(): TaskContainer {
        return TaskContainer.fromDataSource(
            id = VODocumentId(id),
            order = TaskContainerOrder(order),
            status = status,
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * TaskContainer 도메인 모델을 DTO로 변환하는 확장 함수
 * @return TaskContainerDTO 객체
 */
fun TaskContainer.toDto(): TaskContainerDTO {
    return TaskContainerDTO(
        id = id.value,
        type = TaskContainerDTO.TYPE_CONTAINER,
        order = order.value,
        status = status,
        createdAt = null,
        updatedAt = null
    )
}