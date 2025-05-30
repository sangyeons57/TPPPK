package com.example.data.model.remote

import com.example.domain.model.base.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/*
 * 메시지 정보를 나타내는 DTO 클래스
 */
data class MessageDTO(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String? = null,
    val content: String = "", // sendMessage 보다 content가 더 명확한 필드명입니다.
    @ServerTimestamp val sentAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val replyToMessageId: String? = null,
    val isDeleted: Boolean = false
) {
    /*
     * DTO를 도메인 모델로 변환
     * @return Message 도메인 모델
     */
    fun toDomain(): Message {
        return Message(
            id = id,
            senderId = senderId,
            senderName = senderName,
            senderProfileImageUrl = senderProfileImageUrl,
            content = content,
            sentAt = sentAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            replyToMessageId = replyToMessageId,
            isDeleted = isDeleted
        )
    }
}

/*
 * Message 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MessageDTO 객체
 */
fun Message.toDto(): MessageDTO {
    return MessageDTO(
        id = id,
        senderId = senderId,
        senderName = senderName,
        senderProfileImageUrl = senderProfileImageUrl,
        content = content,
        sentAt = sentAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        replyToMessageId = replyToMessageId,
        isDeleted = isDeleted
    )
}
