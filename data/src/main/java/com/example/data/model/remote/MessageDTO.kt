package com.example.data.model.remote

import com.example.domain.model.base.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName

/*
 * 메시지 정보를 나타내는 DTO 클래스
 */
data class MessageDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(SENDER_ID)
    val senderId: String = "",
    @get:PropertyName(SEND_MESSAGE)
    val content: String = "",
    @get:PropertyName(SENT_AT)
    @ServerTimestamp val sentAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(REPLY_TO_MESSAGE_ID)
    val replyToMessageId: String? = null,
    @get:PropertyName(IS_DELETED)
    val isDeleted: Boolean = false
) : DTO {

    companion object {
        const val COLLECTION_NAME = Message.COLLECTION_NAME
        const val SENDER_ID = Message.KEY_SENDER_ID
        const val SEND_MESSAGE = Message.KEY_SEND_MESSAGE
        const val SENT_AT = Message.KEY_SENT_AT
        const val UPDATED_AT = Message.KEY_UPDATED_AT
        const val REPLY_TO_MESSAGE_ID = Message.KEY_REPLY_TO_MESSAGE_ID
        const val IS_DELETED = Message.KEY_IS_DELETED

        fun from(message: Message): MessageDTO {
            return MessageDTO(
                id = message.id.value,
                senderId = message.senderId.value,
                content = message.content.value,
                sentAt = DateTimeUtil.instantToFirebaseTimestamp(message.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(message.updatedAt),
                replyToMessageId = message.replyToMessageId?.value,
                isDeleted = message.isDeleted.value
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Message 도메인 모델
     */
    override fun toDomain(): Message {
        return Message.fromDataSource(
            id = VODocumentId(id),
            senderId = UserId(senderId),
            content = MessageContent(content),
            createdAt = sentAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            replyToMessageId = replyToMessageId?.let{VODocumentId(it)},
            isDeleted = MessageIsDeleted(isDeleted)
        )
    }
}

/**
 * Message 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MessageDTO 객체
 */
fun Message.toDto(): MessageDTO {
    return MessageDTO(
        id = id.value,
        senderId = senderId.value,
        content = content.value,
        sentAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        replyToMessageId = replyToMessageId?.value,
        isDeleted = isDeleted.value
    )
}
