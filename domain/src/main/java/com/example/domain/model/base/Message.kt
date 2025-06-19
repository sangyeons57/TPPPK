package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

import com.example.domain.event.AggregateRoot
import com.example.domain.event.message.MessageContentUpdatedEvent
import com.example.domain.event.message.MessageDeletedEvent
import com.example.domain.event.message.MessageSentEvent
import com.example.domain.model.vo.DocumentId

class Message private constructor(
    initialSenderId: DocumentId,
    initialSenderName: String,
    initialSenderProfileImageUrl: String?,
    initialContent: String,
    initialReplyToMessageId: DocumentId?,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    initialIsDeleted: Boolean,
    override val id: DocumentId,
    override var isNew: Boolean
) : AggregateRoot() {

    val senderId: DocumentId = initialSenderId
    val senderName: String = initialSenderName
    val senderProfileImageUrl: String? = initialSenderProfileImageUrl
    val replyToMessageId: DocumentId? = initialReplyToMessageId
    val createdAt: Instant = initialCreatedAt

    var content: String = initialContent
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set
    var isDeleted: Boolean = initialIsDeleted
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_SENDER_ID to this.senderId,
            KEY_SENDER_NAME to this.senderName,
            KEY_SENDER_PROFILE_IMAGE_URL to this.senderProfileImageUrl,
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
    fun updateContent(newContent: String) {
        if (this.content == newContent || isDeleted) return

        this.content = newContent
        this.updatedAt = Instant.now()
        pushDomainEvent(MessageContentUpdatedEvent(this.id, this.content, this.updatedAt))
    }

    /**
     * Marks the message as deleted.
     */
    fun delete() {
        if (isDeleted) return

        this.isDeleted = true
        this.updatedAt = Instant.now()
        pushDomainEvent(MessageDeletedEvent(this.id, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "messages"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_SENDER_NAME = "senderName"
        const val KEY_SENDER_PROFILE_IMAGE_URL = "senderProfileImageUrl"
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
            senderId: DocumentId,
            senderName: String,
            senderProfileImageUrl: String?,
            content: String,
            replyToMessageId: DocumentId?
        ): Message {
            val now = Instant.now()
            val message = Message(
                initialSenderId = senderId,
                initialSenderName = senderName,
                initialSenderProfileImageUrl = senderProfileImageUrl,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                initialIsDeleted = false,
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
            senderId: DocumentId,
            senderName: String,
            senderProfileImageUrl: String?,
            content: String,
            replyToMessageId: DocumentId?,
            createdAt: Instant,
            updatedAt: Instant,
            isDeleted: Boolean
        ): Message {
            return Message(
                initialSenderId = senderId,
                initialSenderName = senderName,
                initialSenderProfileImageUrl = senderProfileImageUrl,
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

