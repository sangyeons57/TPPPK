package com.example.data.remote.dto

import com.google.firebase.Timestamp

/**
 * Firestore의 친구 관계 문서에 직접 매핑되는 Data Transfer Object (DTO) 입니다.
 * 경로: users/{userId}/friends/{friendId}
 *
 * @property status 친구 관계의 현재 상태 ("accepted", "pending_sent", "pending_received").
 * @property timestamp 관계가 시작된 (또는 요청된) 시간. Firestore 서버 시간을 사용하는 것이 좋습니다.
 * @property acceptedAt 관계가 수락된 시간. null일 수 있습니다.
 */
data class FriendRelationshipDto(
    val status: String? = null,
    val timestamp: Timestamp? = null,
    val acceptedAt: Timestamp? = null
) {
    // Firestore Data Class는 기본 생성자가 필요합니다.
    constructor() : this(null, null, null)
} 