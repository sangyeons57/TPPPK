package com.example.domain.model

import java.time.Instant
// import java.time.LocalDateTime // 주석 처리된 함수에서만 사용되므로 제거 가능
// import java.time.ZoneId // 주석 처리된 함수에서만 사용되므로 제거 가능

/**
 * 모든 유형의 채널에서 사용되는 통합 메시지 모델입니다.
 */
data class ChatMessage(
    /**
     * 메시지의 고유 ID입니다.
     */
    val id: String,
    
    /**
     * 메시지가 속한 채널 ID입니다.
     */
    val channelId: String,
    
    /**
     * 메시지 발신자의 ID입니다.
     */
    val senderId: String,
    
    /**
     * 메시지 발신자의 이름입니다. UI 표시용입니다.
     */
    val senderName: String,
    
    /**
     * 메시지 발신자의 프로필 이미지 URL입니다.
     */
    val senderProfileUrl: String?,
    
    /**
     * 메시지 내용입니다.
     */
    val text: String,
    
    /**
     * 메시지 전송 시간입니다.
     * UTC 기준 시간으로 저장됩니다.
     */
    val timestamp: Instant,
    
    /**
     * 메시지가 마지막으로 수정된 시간입니다. (선택 사항)
     * UTC 기준 시간으로 저장됩니다.
     */
    val updatedAt: Instant? = null,
    
    /**
     * 메시지에 대한 반응(이모지) 맵입니다.
     * 키는 유니코드 이모지이고, 값은 반응한 사용자 ID 목록입니다.
     */
    val reactions: Map<String, List<String>> = emptyMap(),
    
    /**
     * 메시지에 포함된 첨부파일 목록입니다.
     */
    val attachments: List<MessageAttachment> = emptyList(),
    
    /**
     * 답장 대상 메시지 ID입니다. 답장이 아닌 경우 null입니다.
     */
    val replyToMessageId: String? = null,
    
    /**
     * 메시지가 수정되었는지 여부입니다.
     */
    val isEdited: Boolean = false,
    
    /**
     * 메시지가 삭제되었는지 여부입니다. (소프트 삭제)
     */
    val isDeleted: Boolean = false,
    
    /**
     * 메시지 관련 추가 메타데이터입니다.
     */
    val metadata: Map<String, Any>? = null
) {
    /**
     * 메시지 전송 시간을 UI 표시용 LocalDateTime으로 변환합니다.
     */
    // fun getTimestampLocal(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    //     return LocalDateTime.ofInstant(timestamp, zoneId)
    // }
}

// AttachmentType enum 및 MessageAttachment data class는 MessageAttachment.kt 파일로 이동됨