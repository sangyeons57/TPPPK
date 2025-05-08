package com.example.data.model.remote.friend

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore 'users/{userId}/friends/{friendId}' 문서와 매핑되는 데이터 클래스
 * 친구 관계 정보를 담고 있습니다.
 */
data class FriendRelationshipDto(
    /**
     * 친구 관계 상태
     * - "pending_sent": 현재 사용자가 친구 요청을 보낸 상태
     * - "pending_received": 현재 사용자가 친구 요청을 받은 상태
     * - "accepted": 친구 관계가 성립된 상태
     */
    val status: String? = null,
    
    /**
     * 친구 관계가 시작된 시간 (요청이 보내진 시간)
     */
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    
    /**
     * 친구 요청이 수락된 시간 (status가 "accepted"인 경우에만 값이 존재)
     */
    @PropertyName("accepted_at")
    val acceptedAt: Timestamp? = null
) 