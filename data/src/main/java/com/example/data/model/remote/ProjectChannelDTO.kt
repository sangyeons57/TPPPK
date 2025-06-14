package com.example.data.model.remote

import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.ProjectChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.constants.FirestoreConstants
import com.google.firebase.firestore.PropertyName

/*
 * 프로젝트 채널 정보를 나타내는 DTO 클래스
 */
data class ProjectChannelDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(FirestoreConstants.Project.Channels.CHANNEL_NAME)
    val channelName: String = "",
    @get:PropertyName(FirestoreConstants.Project.Channels.CHANNEL_TYPE)
    val channelType: String = "MESSAGES", // "MESSAGES", "TASKS" 등
    @get:PropertyName(FirestoreConstants.Project.Channels.ORDER)
    val order: Double = 0.0, // Added order field
    @get:PropertyName(FirestoreConstants.Project.Channels.CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Project.Channels.UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /*
     * DTO를 도메인 모델로 변환
     * @return ProjectChannel 도메인 모델
     */
    fun toDomain(): ProjectChannel {
        return ProjectChannel(
            id = id,
            channelName = channelName,
            channelType = try {
                ProjectChannelType.valueOf(channelType.uppercase())
            } catch (e: Exception) {
                ProjectChannelType.MESSAGES
            },
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
        channelType = channelType.name.lowercase(),
        order = order, // Added order mapping
        // createdAt and updatedAt are set by @ServerTimestamp on write, 
        // so sending them from client during an update might be ignored or could be problematic.
        // For updates, often only mutable fields are sent. 
        // However, toDto() is a general mapper, so keeping them for now.
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
