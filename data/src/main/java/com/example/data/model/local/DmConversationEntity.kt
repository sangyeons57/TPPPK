package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * DM 대화 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(tableName = "dm_conversations")
data class DmConversationEntity(
    /**
     * DM 채널 ID (uid1_uid2 형식)
     */
    @PrimaryKey
    val id: String,
    
    /**
     * 대화 상대방 사용자 ID
     */
    val otherUserId: String,
    
    /**
     * 대화 상대방 닉네임
     */
    val otherUserNickname: String,
    
    /**
     * 대화 상대방 프로필 이미지 URL (nullable)
     */
    val otherUserProfileImageUrl: String?,
    
    /**
     * 마지막 메시지 내용 (nullable)
     */
    val lastMessage: String?,
    
    /**
     * 마지막 메시지 타임스탬프 (nullable)
     */
    val lastMessageTimestamp: LocalDateTime?,
    
    /**
     * 로컬 캐시 저장 시간
     */
    val cachedAt: LocalDateTime
) 