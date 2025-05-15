package com.example.domain.model

import java.time.Instant // Import Instant
// import java.util.Date // Remove Date import

/**
 * 사용자 간의 친구 관계를 나타내는 데이터 모델입니다.
 * Firestore의 `users/{ownerUserId}/friends/{friendUserId}` 문서에 해당합니다.
 *
 * @property ownerUserId 이 친구 관계 목록의 소유자 ID.
 * @property friendUserId 친구의 사용자 ID. Firestore 문서의 ID에 해당합니다.
 * @property status 친구 관계의 현재 상태.
 * @property timestamp 관계가 시작된 (또는 요청/업데이트된) 시간.
 * @property acceptedAt 관계가 수락된 시간. `status`가 'ACCEPTED'일 때 유효하며, 그 외에는 null일 수 있습니다.
 */
data class FriendRelationship(
    val ownerUserId: String, // Added: The user to whom this friend list belongs
    val friendUserId: String, // Renamed from friendId for clarity with ownerUserId
    val status: FriendRequestStatus, // Changed to Enum
    val timestamp: Instant,
    val acceptedAt: Instant? = null
) 