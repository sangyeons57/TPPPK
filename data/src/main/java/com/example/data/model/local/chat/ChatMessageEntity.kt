package com.example.data.model.local.chat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 로컬 데이터베이스에 저장되는 채팅 메시지 엔티티
 * 
 * @property id 로컬 데이터베이스에서의 자동 생성 ID
 * @property chatId 서버/원격 메시지 ID
 * @property channelId 채팅 채널 ID
 * @property userId 메시지 작성자 ID
 * @property userName 메시지 작성자 이름
 * @property userProfileUrl 메시지 작성자 프로필 이미지 URL (nullable)
 * @property message 메시지 내용
 * @property sentAt 메시지 전송 시간 (Unix timestamp)
 * @property isModified 메시지 수정 여부
 * @property attachmentImageUrls 첨부된 이미지 URL 목록 (콤마로 구분된 문자열)
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index("channelId"),
        Index("chatId", "channelId", unique = true)
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: Int,
    val channelId: String,
    val userId: Int,
    val userName: String,
    val userProfileUrl: String? = null,
    val message: String,
    val sentAt: Long, // Unix timestamp (milliseconds)
    val isModified: Boolean = false,
    val attachmentImageUrls: String? = null // 콤마로 구분된 URL 목록 문자열
) 