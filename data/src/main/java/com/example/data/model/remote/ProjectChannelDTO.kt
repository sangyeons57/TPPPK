package com.example.data.model.remote

import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.ProjectChannel
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.data.model.DTO
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date
import com.example.domain.model.AggregateRoot

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
    val order: Double = 0.0, // Added order field
    @get:PropertyName(CATEGORY_ID)
    val categoryId: String? = null, // Added categoryId field for category association
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
        const val CATEGORY_ID = ProjectChannel.KEY_CATEGORY_ID
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectChannel 도메인 모델
     */
    override fun toDomain(): ProjectChannel {
        return ProjectChannel.fromDataSource(
            id = VODocumentId(id),
            channelName = Name(channelName),
            channelType = channelType,
            order = ProjectChannelOrder(order), // Added order mapping
            categoryId = categoryId?.let { VODocumentId(it) }, // Added categoryId mapping
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * ProjectChannel 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectChannelDTO 객체
 */
fun ProjectChannel.toDto(): ProjectChannelDTO {
    return ProjectChannelDTO(
        id = id.value,
        channelName = channelName.value,
        channelType = channelType,
        order = order.value, // Added order mapping
        categoryId = categoryId?.value, // Added categoryId mapping
        createdAt = null,
        updatedAt = null
    )
}
