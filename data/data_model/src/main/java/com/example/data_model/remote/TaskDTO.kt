package com.example.data_model.remote

import com.example.domain.AggregateRoot
import com.example.domain.DTO
import com.example.domain.model.base.Task
import com.example.domain.vo.task.TaskStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 태스크 정보를 나타내는 DTO 클래스
 * 통합된 task_container collection에 저장됩니다. 불필요한 "type" 필드는 제거되었습니다.
 */
data class TaskDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(CHANNEL_ID)
    val channelId: String = "",
    @get:PropertyName(TASK_TYPE)
    val taskType: String = "general",
    @get:PropertyName(STATUS)
    val status: TaskStatus = TaskStatus.PENDING,
    @get:PropertyName(CONTENT)
    val content: String = "",
    @get:PropertyName(ORDER)
    val order: Int = 0,
    @get:PropertyName(CHECKED_BY)
    val checkedBy: String? = null,
    @get:PropertyName(CHECKED_AT)
    val checkedAt: Date? = null,
    @get:PropertyName(DELETED_AT)
    val deletedAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Task.COLLECTION_NAME
        const val CHANNEL_ID = Task.KEY_CHANNEL_ID
        const val TASK_TYPE = Task.KEY_TASK_TYPE
        const val STATUS = Task.KEY_STATUS
        const val CONTENT = Task.KEY_CONTENT
        const val ORDER = Task.KEY_ORDER
        const val CHECKED_BY = Task.KEY_CHECKED_BY
        const val CHECKED_AT = Task.KEY_CHECKED_AT
        const val DELETED_AT = Task.KEY_DELETED_AT
    }

}
