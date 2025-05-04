package com.example.data.model.remote.chat

import com.google.firebase.Timestamp

/**
 * Firestore와 통신하기 위한 채팅 메시지 정보 DTO 입니다.
 */
data class ChatMessageDto(
    val id: String = "", // Firestore 문서 ID (메시지 ID)
    val chatId: String = "", // 어떤 채팅방(ChatEntity)에 속하는지 식별
    val senderId: String = "", // 발신자 사용자 ID
    val message: String = "", // 메시지 내용
    val sentAt: Timestamp = Timestamp.now(), // 메시지 발신 시간
    val isRead: Boolean = false // 수신 확인 여부 (예시)
) 