package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

import com.example.domain.model.AggregateRoot
import com.example.domain.event.message.MessageContentUpdatedEvent
import com.example.domain.event.message.MessageDeletedEvent
import com.example.domain.event.message.MessageSentEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted

class Message private constructor(
    initialSenderId: UserId,
    initialContent: MessageContent,
    initialReplyToMessageId: DocumentId?,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    initialIsDeleted: MessageIsDeleted,
    override val id: DocumentId,
    override var isNew: Boolean
) : AggregateRoot() {

    val senderId: UserId = initialSenderId
    val replyToMessageId: DocumentId? = initialReplyToMessageId
    override val createdAt: Instant = initialCreatedAt

    var content: MessageContent = initialContent
        private set
    override var updatedAt: Instant = initialUpdatedAt
        private set
    var isDeleted: MessageIsDeleted = initialIsDeleted
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_SENDER_ID to this.senderId,
            KEY_SEND_MESSAGE to this.content,
            KEY_REPLY_TO_MESSAGE_ID to this.replyToMessageId,
            KEY_SENT_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_IS_DELETED to this.isDeleted
        )
    }

    /**
     * Updates the content of the message.
     */
    fun updateContent(newContent: MessageContent) {
        if (this.content == newContent || isDeleted.value) return

        this.content = newContent
        this.updatedAt = Instant.now()
        pushDomainEvent(MessageContentUpdatedEvent(this.id, this.content, this.updatedAt))
    }

    /**
     * Marks the message as deleted.
     */
    fun delete() {
        if (isDeleted.value) return

        this.isDeleted = MessageIsDeleted.TRUE
        this.updatedAt = Instant.now()
        pushDomainEvent(MessageDeletedEvent(this.id, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "messages"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_SEND_MESSAGE = "content"
        const val KEY_SENT_AT = "sentAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_REPLY_TO_MESSAGE_ID = "replyToMessageId"
        const val KEY_IS_DELETED = "isDeleted"
        /**
         * Factory method for sending a new message.
         */
        fun create(
            id: DocumentId,
            senderId: UserId,
            content: MessageContent,
            replyToMessageId: DocumentId?
        ): Message {
            val now = Instant.now()
            val message = Message(
                initialSenderId = senderId,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                initialIsDeleted = MessageIsDeleted.FALSE,
                id = id,
                isNew = true
            )
            message.pushDomainEvent(MessageSentEvent(message.id, message.senderId, message.content, message.replyToMessageId, now))
            return message
        }

        /**
         * Factory method to reconstitute a Message from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            senderId: UserId,
            content: MessageContent,
            replyToMessageId: DocumentId?,
            createdAt: Instant,
            updatedAt: Instant,
            isDeleted: MessageIsDeleted
        ): Message {
            return Message(
                initialSenderId = senderId,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                initialIsDeleted = isDeleted,
                id = id,
                isNew = false
            )
        }
    }
}

