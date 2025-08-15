package com.example.data_model.remote

import com.example.domain.AggregateRoot
import com.example.domain.DTO
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
    @get:PropertyName(CHANNEL_ID)
    val channelId: String = "",
    @get:PropertyName(SENDER_ID)
    val senderId: String = "",
    @get:PropertyName(MESSAGE_TYPE)
    val messageType: String = "TEXT", // 메시지 타입 (TEXT, SYSTEM_PROJECT_JOIN, etc.)
    @get:PropertyName(PAYLOAD)
    val payload: Any? = null, // String 또는 Map<String, Any?> 모두 호환 (기존 content 대체)
    @get:PropertyName(REPLY_TO_MESSAGE_ID)
    val replyToMessageId: String? = null,
    @get:PropertyName(IS_DELETED)
    val isDeleted: Boolean = false,
    @get:PropertyName(MENTIONS)
    val mentions: List<Map<String, Any>> = emptyList(),
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null,

    // Backward compatibility - 기존 content 필드 (읽기 전용)
    @get:PropertyName(SEND_MESSAGE)
    val content: String = "" // 하위 호환성을 위해 유지, 쓰기 시에는 사용하지 않음
) : DTO {

    companion object {
        const val COLLECTION_NAME = Message.COLLECTION_NAME
        const val CHANNEL_ID = Message.KEY_CHANNEL_ID
        const val SENDER_ID = Message.KEY_SENDER_ID
        const val MESSAGE_TYPE = Message.KEY_MESSAGE_TYPE
        const val PAYLOAD = Message.KEY_PAYLOAD
        const val REPLY_TO_MESSAGE_ID = Message.KEY_REPLY_TO_MESSAGE_ID
        const val IS_DELETED = Message.KEY_IS_DELETED
        const val MENTIONS = Message.KEY_MENTIONS

        // Backward compatibility
        @Deprecated("Use PAYLOAD instead")
        const val SEND_MESSAGE = Message.KEY_SEND_MESSAGE
    }

}
