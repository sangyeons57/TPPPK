package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 친구 요청 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    /**
     * 요청자의 사용자 ID (Firebase Auth UID)
     */
    @PrimaryKey
    val userId: String,
    
    /**
     * 요청자의 닉네임
     */
    val nickname: String,
    
    /**
     * 요청자의 프로필 이미지 URL (nullable)
     */
    val profileImageUrl: String?,
    
    /**
     * 요청 타임스탬프
     */
    val timestamp: Instant?,
    
    /**
     * 로컬 캐시 저장 시간
     */
    val cachedAt: Instant
) 