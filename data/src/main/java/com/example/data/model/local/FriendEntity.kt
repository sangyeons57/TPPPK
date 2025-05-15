package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 친구 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(tableName = "friends")
data class FriendEntity(
    /**
     * 친구의 사용자 ID (Firebase Auth UID)
     */
    @PrimaryKey
    val id: String,
    
    /**
     * 친구의 닉네임
     */
    val nickname: String,
    
    /**
     * 친구 관계 상태 (accepted, pending_sent, pending_received)
     */
    val status: String,
    
    /**
     * 친구의 프로필 이미지 URL (nullable)
     */
    val profileImageUrl: String?,
    
    /**
     * 친구 관계 수락 시간 (nullable)
     */
    val acceptedAt: Instant?,
    
    /**
     * 마지막 갱신 시간
     */
    val lastUpdatedAt: Instant
) 