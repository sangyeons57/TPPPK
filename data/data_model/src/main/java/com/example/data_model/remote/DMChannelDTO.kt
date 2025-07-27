package com.example.data.model.remote

import com.example.domain.model.AggregateRoot
import com.example.domain.model.DTO
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 개인 메시지 채널 정보를 나타내는 DTO 클래스
 */
data class DMChannelDTO(
    @DocumentId override val id: String = "",
    // userId1, userId2 대신 참여자 목록으로 관리하면 확장성 및 쿼리에 유리합니다.
    @get:PropertyName(PARTICIPANTS)
    val participants: List<String> = emptyList(),
    @get:PropertyName(STATUS)
    val status: DMChannelStatus = DMChannelStatus.ACTIVE,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = DMChannel.COLLECTION_NAME
        const val PARTICIPANTS = DMChannel.KEY_PARTICIPANTS
        const val STATUS = DMChannel.KEY_STATUS
    }
}
