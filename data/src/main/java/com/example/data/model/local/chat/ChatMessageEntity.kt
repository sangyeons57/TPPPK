package com.example.data.model.local.chat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 로컬 데이터베이스에 저장되는 채팅 메시지 엔티티
 * 
 * @property id 로컬 데이터베이스에서의 자동 생성 ID (Primary Key)
 * @property chatId 메시지의 고유 ID (Firestore Document ID)
 * @property channelId 이 메시지가 속한 채널의 ID (DM ID 또는 Project Channel ID)
 * @property channelType 채널의 유형 ("DM", "PROJECT_CATEGORY", "PROJECT_DIRECT")
 * @property userId 메시지 작성자 ID (Firebase Auth UID)
 * @property userName 메시지 작성자 이름
 * @property userProfileUrl 메시지 작성자 프로필 이미지 URL (nullable)
 * @property message 메시지 내용
 * @property sentAt 메시지 전송 시간 (Unix timestamp, milliseconds)
 * @property isModified 메시지 수정 여부
 * @property attachmentImageUrls 첨부된 이미지 URL 목록 (JSON String or comma-separated)
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        // Query messages for a specific channel (of any type)
        Index(value = ["channelId", "sentAt"]),
        // Query messages for a specific type of channel
        Index(value = ["channelType", "channelId", "sentAt"]),
        // Ensure message ID is unique within its channel context (optional, chatId should be globally unique)
        // Index(value = ["chatId", "channelId", "channelType"], unique = true) 
        // Index on chatId for fetching specific messages (if needed often)
        Index(value = ["chatId"], unique = true) 
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: String, // Changed from Int to String (Firestore Document ID)
    val channelId: String,
    val channelType: String, // Added: "DM", "PROJECT_CATEGORY", "PROJECT_DIRECT"
    val userId: String, // Changed from Int to String (Firebase Auth UID)
    val userName: String,
    val userProfileUrl: String? = null,
    val message: String,
    val sentAt: Long, // Unix timestamp (milliseconds)
    val isModified: Boolean = false,
    val attachmentImageUrls: String? = null // Consider storing as JSON String for easier parsing
) 