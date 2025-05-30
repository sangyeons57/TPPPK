package com.example.data.model.remote

import com.example.domain.model.base.Reaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/**
 * 메시지 리액션 정보를 나타내는 DTO 클래스
 */
data class ReactionDTO(
    @DocumentId val id: String = "",
    val userId: String = "", // 리액션을 남긴 사용자 ID
    val emoji: String = "",  // 유니코드 이모지
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    /*
     * DTO를 도메인 모델로 변환
     * @return Reaction 도메인 모델
     */
    fun toDomain(): Reaction {
        return Reaction(
            id = id,
            userId = userId,
            emoji = emoji,
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * Reaction 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ReactionDTO 객체
 */
fun Reaction.toDto(): ReactionDTO {
    return ReactionDTO(
        id = id,
        userId = userId,
        emoji = emoji,
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
