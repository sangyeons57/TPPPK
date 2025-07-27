package com.example.data.model.remote

import com.example.domain.model.AggregateRoot
import com.example.domain.model.DTO
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelStatus
import com.example.domain.model.enum.ProjectChannelType
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/*
 * 프로젝트 채널 정보를 나타내는 DTO 클래스
 */
data class ProjectChannelDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(CHANNEL_NAME)
    val channelName: String = "",
    @get:PropertyName(CHANNEL_TYPE)
    val channelType: ProjectChannelType = ProjectChannelType.MESSAGES, // "MESSAGES", "TASKS" 등
    @get:PropertyName(ORDER)
    val order: Double = Category.NO_CATEGORY_ORDER.toDouble(), // Added order field
    @get:PropertyName(STATUS)
    val status: ProjectChannelStatus = ProjectChannelStatus.ACTIVE,
    @get:PropertyName(CATEGORY_ID)
    val categoryId: String = Category.NO_CATEGORY_ID, // Added categoryId field for category association
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = ProjectChannel.COLLECTION_NAME
        const val CHANNEL_NAME = ProjectChannel.KEY_CHANNEL_NAME
        const val CHANNEL_TYPE = ProjectChannel.KEY_CHANNEL_TYPE
        const val ORDER = ProjectChannel.KEY_ORDER
        const val STATUS = ProjectChannel.KEY_STATUS
        const val CATEGORY_ID = ProjectChannel.KEY_CATEGORY_ID
    }
}
