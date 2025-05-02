package com.example.feature_chat.model

/**
 * UI에 채팅 메시지를 어떻게 보여줄지 정의하는 UI 전용 데이터 모델
 */
data class ChatMessageUiModel(
    val localId: String, // LazyColumn Key 및 임시 ID용 (String으로 변경하여 임시/실제 ID 모두 처리)
    val chatId: Int, // 서버 ID (아직 없으면 0 또는 음수)
    val userId: Int,
    val userName: String,
    val userProfileUrl: String?,
    val message: String,
    val formattedTimestamp: String, // UI 표시용 포맷된 시간
    val isModified: Boolean,
    val attachmentImageUrls: List<String> = emptyList(),
    val isMyMessage: Boolean,
    val isSending: Boolean = false, // 메시지 전송 중 상태 (UI 피드백용)
    val sendFailed: Boolean = false // 메시지 전송 실패 상태 (UI 피드백용)
) 