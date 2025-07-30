package com.example.data_model.remote

import com.example.domain.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Message
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

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

}

