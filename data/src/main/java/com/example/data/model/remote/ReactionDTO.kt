package com.example.data.model.remote

import com.example.domain.model.base.Reaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.google.firebase.firestore.PropertyName
import com.example.domain.model.vo.DocumentId as VODocumentId
import java.util.Date

/**
 * 메시지 리액션 정보를 나타내는 DTO 클래스
 */
data class ReactionDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(USER_ID)
    val userId: String = "", // 리액션을 남긴 사용자 ID
    @get:PropertyName(EMOJI)
    val emoji: String = "",  // 유니코드 이모지
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {
    companion object {
        const val COLLECTION_NAME = Reaction.COLLECTION_NAME
        const val USER_ID = Reaction.KEY_USER_ID
        const val EMOJI = Reaction.KEY_EMOJI
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Reaction 도메인 모델
     */
    override fun toDomain(): Reaction {
        return Reaction.fromDataSource(
            id = VODocumentId(id),
            userId = UserId(userId),
            emoji = Emoji(emoji),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * Reaction 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ReactionDTO 객체
 */
fun Reaction.toDto(): ReactionDTO {
    return ReactionDTO(
        id = id.value,
        userId = userId.value,
        emoji = emoji.value,
        createdAt = Date.from(createdAt),
        updatedAt = Date.from(updatedAt)
    )
}
