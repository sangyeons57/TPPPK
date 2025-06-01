package com.example.data.model.remote

import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.ProjectChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.core_common.util.DateTimeUtil

/*
 * 프로젝트 채널 정보를 나타내는 DTO 클래스
 */
data class ProjectChannelDTO(
    @DocumentId val id: String = "",
    val channelName: String = "",
    val channelType: String = "MESSAGES", // "MESSAGES", "TASKS" 등
    @ServerTimestamp val createdAt: Timestamp? = null,
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
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
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
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
