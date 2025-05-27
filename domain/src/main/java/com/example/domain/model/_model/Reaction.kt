package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * 메시지 반응 정보를 나타내는 도메인 모델입니다.
 * Firestore의 ReactionDTO와 1:1 매핑됩니다.
 */
data class Reaction(
    /**
     * 반응의 고유 ID (Firestore Document ID)
     */
    @DocumentId
    val id: String,

    /**
     * 반응을 남긴 사용자의 ID입니다.
     */
    val userId: String,

    /**
     * 사용된 유니코드 이모지 문자열입니다.
     */
    val emoji: String,

    /**
     * 반응 생성 시간 (UTC, 선택 사항) 입니다.
     */
    val createdAt: Instant?
) 