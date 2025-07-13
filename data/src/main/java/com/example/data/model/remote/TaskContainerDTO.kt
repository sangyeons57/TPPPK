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
 */
data class TaskContainerDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(ORDER)
    val order: Int = 0,
    @get:PropertyName(STATUS)
    val status: String = "active",
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = TaskContainer.COLLECTION_NAME
        const val ORDER = TaskContainer.KEY_ORDER
        const val STATUS = TaskContainer.KEY_STATUS
    }

    /**
     * DTO를 도메인 모델로 변환
     * @return TaskContainer 도메인 모델
     */
    override fun toDomain(): TaskContainer {
        return TaskContainer.fromDataSource(
            id = VODocumentId(id),
            order = TaskContainerOrder(order),
            status = TaskContainerStatus.fromValue(status),
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
        order = order.value,
        status = status.value,
        createdAt = null,
        updatedAt = null
    )
}