package com.example.data.model.remote

import com.example.domain.model.base.DMChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

import com.google.firebase.firestore.PropertyName

/**
 * 개인 메시지 채널 정보를 나타내는 DTO 클래스
 */
data class DMChannelDTO(
    @DocumentId val id: String = "",
    // userId1, userId2 대신 참여자 목록으로 관리하면 확장성 및 쿼리에 유리합니다.
    @get:PropertyName(PARTICIPANTS)
    val participants: List<String> = emptyList(),
    @get:PropertyName(LAST_MESSAGE_PREVIEW)
    val lastMessagePreview: String? = null,
    @get:PropertyName(LAST_MESSAGE_TIMESTAMP)
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {

    companion object {
        const val COLLECTION_NAME = "dm_channels"
        const val PARTICIPANTS = "participants" // List<String> = "userId1"
        const val LAST_MESSAGE_PREVIEW = "lastMessagePreview"
        const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return DMChannel 도메인 모델
     */
    fun toDomain(): DMChannel {
        return DMChannel(
            id = id,
            participants = participants,
            lastMessagePreview = lastMessagePreview,
            lastMessageTimestamp = lastMessageTimestamp?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * DMChannel 도메인 모델을 DTO로 변환하는 확장 함수
 * @return DMChannelDTO 객체
 */
fun DMChannel.toDto(): DMChannelDTO {
    return DMChannelDTO(
        id = id,
        participants = participants,
        lastMessagePreview = lastMessagePreview,
        lastMessageTimestamp = lastMessageTimestamp?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
