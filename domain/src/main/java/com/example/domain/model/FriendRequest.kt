// 경로: domain/model/FriendRequest.kt
package com.example.domain.model

import java.time.LocalDateTime

data class FriendRequest(
    val userId: String, // 요청 보낸 사람 ID
    val userName: String,
    val profileImageUrl: String?,
    val timestamp: LocalDateTime? // 요청 시간 추가
    // 필요시 요청 시간 등 추가
)