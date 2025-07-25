package com.example.feature_chat.model

import com.example.domain.model.vo.message.MentionInfo
import java.time.Instant // Import Instant

/**
 * UI에 채팅 메시지를 어떻게 보여줄지 정의하는 UI 전용 데이터 모델
 * 단일 ID 시스템으로 중복 제거 - Firestore document ID가 유일한 식별자
 */
data class ChatMessageUiModel(
    val messageId: String, // Firestore document ID (LazyColumn Key 및 모든 작업의 식별자)
    val userId: String,
    val userName: String,
    val userProfileUrl: String?,
    val message: String,
    val formattedTimestamp: String, // UI 표시용 포맷된 시간
    val actualTimestamp: Instant, // Raw Instant for logic like pagination cursors
    val isModified: Boolean,
    val attachmentImageUrls: List<String> = emptyList(),
    val isMyMessage: Boolean,
    val isSending: Boolean = false, // 메시지 전송 중 상태 (UI 피드백용)
    val sendFailed: Boolean = false, // 메시지 전송 실패 상태 (UI 피드백용)
    val isDeleted: Boolean = false, // Added to reflect soft delete status in UI  
    val deliveryState: MessageDeliveryState = MessageDeliveryState.Sent,
    val isOptimistic: Boolean = false, // 낙관적 업데이트로 추가된 메시지인지
    val clientSentAt: Instant? = null, // 클라이언트에서 전송한 시간 (메모리 정리 보호용)
    val retryCount: Int = 0, // 재전송 시도 횟수
    val canRetry: Boolean = false, // 재전송 가능 여부
    val errorMessage: String? = null, // 실패 시 에러 메시지
    // 답장 기능
    val replyToMessageId: String? = null, // 답장 대상 메시지 ID
    val replyToContent: String? = null, // 답장 대상 메시지 내용 (UI 표시용)
    val replyToUserName: String? = null, // 답장 대상 메시지 작성자
    // 멘션 기능
    val mentions: List<MentionInfo> = emptyList(), // 메시지에 포함된 멘션들
    val isMentionedMessage: Boolean = false // 현재 사용자가 멘션된 메시지인지
) 