package com.example.data.model.local.chat

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.local.ChatEntity // Assuming ChatEntity exists for ForeignKey
import java.time.LocalDateTime

/**
 * Room 데이터베이스에 저장될 개별 채팅 메시지 엔티티입니다.
 */
@Entity(
    tableName = "chat_messages",
    // ChatEntity 를 참조하는 외래 키 설정 (ChatEntity 가 정의되었다고 가정)
    foreignKeys = [ForeignKey(
        entity = ChatEntity::class,
        parentColumns = ["id"],
        childColumns = ["chatId"],
        onDelete = ForeignKey.CASCADE // 채팅방 삭제 시 관련 메시지도 삭제
    )],
    // 빠른 조회를 위해 chatId 와 sentAt 에 인덱스 추가
    indices = [Index(value = ["chatId"]), Index(value = ["sentAt"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String, // 메시지 ID (UUID 등 고유 값)
    val chatId: String, // 어떤 채팅방(ChatEntity)에 속하는지 식별
    val senderId: String, // 발신자 사용자 ID
    val message: String, // 메시지 내용
    val sentAt: LocalDateTime, // 메시지 발신 시간 (UTC 저장, TypeConverter 사용)
    val isRead: Boolean = false // 수신 확인 여부
) 