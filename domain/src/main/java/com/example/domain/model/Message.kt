package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * 채팅 메시지 정보를 나타내는 도메인 모델입니다.
 * Firestore의 MessageDTO와 1:1 매핑됩니다.
 */
data class Message(
    /**
     * 메시지의 고유 ID (Firestore Document ID)
     */
    @DocumentId
    val id: String,

    /**
     * 메시지 발신자의 사용자 ID입니다.
     */
    val senderId: String,

    /**
     * 메시지 발신자의 이름입니다. UI 표시용입니다.
     */
    val senderName: String,

    /**
     * 메시지 발신자의 프로필 이미지 URL입니다. (선택 사항)
     */
    val senderProfileImageUrl: String?,

    /**
     * 메시지 내용입니다.
     */
    val content: String,

    /**
     * 메시지 전송 시간 (UTC, 선택 사항) 입니다.
     */
    val sentAt: Instant?,

    /**
     * 메시지가 마지막으로 수정된 시간 (UTC, 선택 사항) 입니다.
     */
    val updatedAt: Instant?,

    /**
     * 답장 대상 메시지의 ID입니다. 답장이 아닌 경우 null입니다. (선택 사항)
     */
    val replyToMessageId: String?,

    /**
     * 메시지가 삭제되었는지 여부입니다. (소프트 삭제)
     */
    val isDeleted: Boolean
) 