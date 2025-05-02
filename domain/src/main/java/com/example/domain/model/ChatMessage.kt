package com.example.domain.model

import java.time.LocalDateTime // 순수 시간 데이터 사용

data class ChatMessage(
    val chatId: Int, // 서버 기준 고유 ID
    val channelId: String, // 메시지가 속한 채널 ID
    val userId: Int,
    val userName: String,
    val userProfileUrl: String?,
    val message: String, // 원본 메시지 내용
    val sentAt: LocalDateTime, // 메시지 전송/수신 시각 (LocalDateTime 사용)
    val isModified: Boolean,
    val attachmentImageUrls: List<String> = emptyList()
    // 'id' (로컬 ID)는 Domain 모델에 불필요, 필요시 data 계층이나 ViewModel에서 관리
)