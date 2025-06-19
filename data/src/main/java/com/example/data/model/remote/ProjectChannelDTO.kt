package com.example.data.model.remote

import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.ProjectChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.core_common.util.DateTimeUtil

import com.google.firebase.firestore.PropertyName

/*
 * 프로젝트 채널 정보를 나타내는 DTO 클래스
 */
data class ProjectChannelDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(CHANNEL_NAME)
    val channelName: String = "",
    @get:PropertyName(CHANNEL_TYPE)
    val channelType: ProjectChannelType = ProjectChannelType.MESSAGES, // "MESSAGES", "TASKS" 등
    @get:PropertyName(ORDER)
    val order: Double = 0.0, // Added order field
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null
) {

    companion object {
        const val COLLECTION_NAME = "project_channels"
        const val CHANNEL_NAME = "channelName"
        const val CHANNEL_TYPE = "channelType"
        const val ORDER = "order"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
    /*
     * DTO를 도메인 모델로 변환
     * @return ProjectChannel 도메인 모델
     */
    fun toDomain(): ProjectChannel {
        return ProjectChannel(
            id = id,
            channelName = channelName,
            channelType = channelType,
            order = order, // Added order mapping
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
            // Note: ProjectChannel domain model has more fields (categoryId, createdBy, etc.)
            // which are not present in this DTO. This DTO might be a partial representation.
            // For update operations, ensure all necessary fields are handled or consider a specific UpdateChannelDTO.
        )
    }
}

/**
 * ProjectChannel 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectChannelDTO 객체
 */
fun ProjectChannel.toDto(): ProjectChannelDTO {
    return ProjectChannelDTO(
        id = id,
        channelName = channelName,
        channelType = channelType,
        order = order, // Added order mapping
        // createdAt and updatedAt are set by @ServerTimestamp on write, 
        // so sending them from client during an update might be ignored or could be problematic.
        // For updates, often only mutable fields are sent. 
        // However, toDto() is a general mapper, so keeping them for now.
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
