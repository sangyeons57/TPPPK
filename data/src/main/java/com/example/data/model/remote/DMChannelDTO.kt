package com.example.data.model.remote

import com.example.domain.model.base.DMChannel
import com.google.firebase.firestore.DocumentId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.data.model.DTO
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview

import com.google.firebase.firestore.PropertyName
import com.example.domain.model.AggregateRoot
import java.util.Date

/**
 * 개인 메시지 채널 정보를 나타내는 DTO 클래스
 */
data class DMChannelDTO(
    @DocumentId override val id: String = "",
    // userId1, userId2 대신 참여자 목록으로 관리하면 확장성 및 쿼리에 유리합니다.
    @get:PropertyName(PARTICIPANTS)
    val participants: List<String> = emptyList(),
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = DMChannel.COLLECTION_NAME
        const val PARTICIPANTS = DMChannel.KEY_PARTICIPANTS
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return DMChannel 도메인 모델
     */
    override fun toDomain(): DMChannel {
        return DMChannel.fromDataSource(
            id = VODocumentId(id),
            participants = participants.map { UserId(it) },
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
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
        participants = participants.map { it.value } // createdAt/updatedAt omitted for ServerTimestamp
    )
}
