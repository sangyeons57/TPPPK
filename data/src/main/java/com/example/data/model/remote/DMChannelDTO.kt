package com.example.data.model.remote

import com.example.domain.model.base.DMChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview

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
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
) : DTO {

    companion object {
        const val COLLECTION_NAME = DMChannel.COLLECTION_NAME
        const val PARTICIPANTS = DMChannel.KEY_PARTICIPANTS
        const val LAST_MESSAGE_PREVIEW = DMChannel.KEY_LAST_MESSAGE_PREVIEW
        const val LAST_MESSAGE_TIMESTAMP = DMChannel.KEY_LAST_MESSAGE_TIMESTAMP
        const val CREATED_AT = DMChannel.KEY_CREATED_AT
        const val UPDATED_AT = DMChannel.KEY_UPDATED_AT

        fun from (domain: DMChannel): DMChannelDTO {
            return DMChannelDTO(
                id = domain.id.value,
                participants = domain.participants.map { it.value },
                lastMessagePreview = domain.lastMessagePreview?.value,
                lastMessageTimestamp = domain.lastMessageTimestamp?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(domain.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(domain.updatedAt),
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return DMChannel 도메인 모델
     */
    override fun toDomain(): DMChannel {
        return DMChannel.fromDataSource(
            id = VODocumentId(id),
            participants = participants.map { UserId(it) },
            lastMessagePreview = lastMessagePreview?.let{ DMChannelLastMessagePreview(it) },
            lastMessageTimestamp = lastMessageTimestamp?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt),
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt)
        )
    }
}

/**
 * DMChannel 도메인 모델을 DTO로 변환하는 확장 함수
 * @return DMChannelDTO 객체
 */
fun DMChannel.toDto(): DMChannelDTO {
    return DMChannelDTO(
        id = id.value,
        participants = participants.map { it.value },
        lastMessagePreview = lastMessagePreview?.value,
        lastMessageTimestamp = lastMessageTimestamp?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
