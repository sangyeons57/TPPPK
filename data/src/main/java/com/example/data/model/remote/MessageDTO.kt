package com.example.data.model.remote

import com.example.domain.model.base.Message
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import com.example.data.model.DTO
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.MentionType
import com.example.domain.model.vo.message.MentionInfo

/*
 * 메시지 정보를 나타내는 DTO 클래스
 */
data class MessageDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(SENDER_ID)
    val senderId: String = "",
    @get:PropertyName(SEND_MESSAGE)
    val content: String = "",
    @get:PropertyName(REPLY_TO_MESSAGE_ID)
    val replyToMessageId: String? = null,
    @get:PropertyName(IS_DELETED)
    val isDeleted: Boolean = false,
    @get:PropertyName(MENTIONS)
    val mentions: List<Map<String, Any>> = emptyList(),
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Message.COLLECTION_NAME
        const val SENDER_ID = Message.KEY_SENDER_ID
        const val SEND_MESSAGE = Message.KEY_SEND_MESSAGE
        const val REPLY_TO_MESSAGE_ID = Message.KEY_REPLY_TO_MESSAGE_ID
        const val IS_DELETED = Message.KEY_IS_DELETED
        const val MENTIONS = Message.KEY_MENTIONS
    }

    /**
     * DTO를 도메인 모델로 변환
     * @return Message 도메인 모델
     */
    override fun toDomain(): Message {
        val domainMentions = this.mentions.mapNotNull { map ->
            try {
                val type = MentionType.valueOf(map[MentionInfo.KEY_TYPE] as String)
                val id = map[MentionInfo.KEY_ID] as String
                val displayName = map[MentionInfo.KEY_DISPLAY_NAME] as String
                MentionInfo(type, id, displayName)
            } catch (e: Exception) {
                null
            }
        }

        return Message.fromDataSource(
            id = VODocumentId(id),
            senderId = UserId(senderId),
            content = MessageContent(content),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant(),
            replyToMessageId = replyToMessageId?.let{VODocumentId(it)},
            isDeleted = MessageIsDeleted(isDeleted),
            mentions = domainMentions
        )
    }
}

/**
 * Message 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MessageDTO 객체
 */
fun Message.toDto(): MessageDTO {
    val dtoMentions = this.mentions.map { mention ->
        mapOf(
            MentionInfo.KEY_TYPE to mention.type.name,
            MentionInfo.KEY_ID to mention.id,
            MentionInfo.KEY_DISPLAY_NAME to mention.displayName
        )
    }

    return MessageDTO(
        id = id.value,
        senderId = senderId.value,
        content = content.value,
        createdAt = null,
        updatedAt = null,
        replyToMessageId = replyToMessageId?.value,
        isDeleted = isDeleted.value,
        mentions = dtoMentions
    )
}
