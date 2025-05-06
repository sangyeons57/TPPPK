package com.example.domain.model

import java.util.Date // Firestore Timestamp는 보통 Date로 매핑됩니다.

/**
 * 사용자 간의 친구 관계를 나타내는 데이터 모델입니다.
 * Firestore의 `users/{userId}/friends/{friendId}` 문서에 해당합니다.
 *
 * @property friendId 친구의 사용자 ID. Firestore 문서의 ID에 해당합니다.
 * @property status 친구 관계의 현재 상태 (`accepted`, `pending_sent`, `pending_received`).
 * @property timestamp 관계가 시작된 (또는 요청된) 시간.
 * @property acceptedAt 관계가 수락된 시간. `status`가 'accepted'일 때 유효하며, 그 외에는 null일 수 있습니다.
 */
data class FriendRelationship(
    val friendId: String,
    val status: String,
    val timestamp: Date,
    val acceptedAt: Date? = null
) 