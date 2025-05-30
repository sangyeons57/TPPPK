package com.example.data.model.remote

import com.example.domain.model.base.DMChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/**
 * 개인 메시지 채널 정보를 나타내는 DTO 클래스
 */
data class DMChannelDTO(
    @DocumentId val id: String = "",
    // userId1, userId2 대신 참여자 목록으로 관리하면 확장성 및 쿼리에 유리합니다.
    val participants: List<String> = emptyList(),
    val lastMessagePreview: String? = null,
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
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
