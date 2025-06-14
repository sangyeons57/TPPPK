package com.example.data.model.remote

import com.example.domain.model.base.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.constants.FirestoreConstants
import com.google.firebase.firestore.PropertyName

/*
 * 메시지 정보를 나타내는 DTO 클래스
 */
data class MessageDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(FirestoreConstants.MessageFields.SENDER_ID)
    val senderId: String = "",
    @get:PropertyName(FirestoreConstants.MessageFields.SENDER_NAME)
    val senderName: String = "",
    @get:PropertyName(FirestoreConstants.MessageFields.SENDER_PROFILE_IMAGE_URL)
    val senderProfileImageUrl: String? = null,
    @get:PropertyName(FirestoreConstants.MessageFields.SEND_MESSAGE)
    val content: String = "",
    @get:PropertyName(FirestoreConstants.MessageFields.SENT_AT)
    @ServerTimestamp val sentAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.MessageFields.UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.MessageFields.REPLY_TO_MESSAGE_ID)
    val replyToMessageId: String? = null,
    @get:PropertyName(FirestoreConstants.MessageFields.IS_DELETED)
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
