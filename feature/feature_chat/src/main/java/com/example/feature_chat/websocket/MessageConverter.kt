package com.example.feature_chat.websocket

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import java.time.Instant

object MessageConverter {
    
    fun fromWebSocketEvent(event: ChatWebSocketEvent.MessageReceived): Message {
        return Message.fromDataSource(
            id = DocumentId(event.messageId),
            senderId = UserId(event.senderId),
            content = MessageContent(event.content),
            replyToMessageId = event.replyToMessageId?.let { DocumentId(it) },
            createdAt = parseTimestamp(event.timestamp),
            updatedAt = parseTimestamp(event.timestamp),
            isDeleted = MessageIsDeleted.FALSE
        )
    }
    
    fun fromWebSocketEdit(
        existingMessage: Message,
        event: ChatWebSocketEvent.MessageEdited
    ): Message {
        return Message.fromDataSource(
            id = existingMessage.id,
            senderId = existingMessage.senderId,
            content = MessageContent(event.newContent),
            replyToMessageId = existingMessage.replyToMessageId,
            createdAt = existingMessage.createdAt,
            updatedAt = parseTimestamp(event.timestamp),
            isDeleted = existingMessage.isDeleted
        )
    }
    
    private fun parseTimestamp(timestampString: String): Instant {
        return try {
            Instant.parse(timestampString)
        } catch (e: Exception) {
            DateTimeUtil.nowInstant()
        }
    }
}