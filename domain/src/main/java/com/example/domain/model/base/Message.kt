package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
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
    initialIsDeleted: MessageIsDeleted,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    val senderId: UserId = initialSenderId
    val replyToMessageId: DocumentId? = initialReplyToMessageId

    var content: MessageContent = initialContent
        private set
    var isDeleted: MessageIsDeleted = initialIsDeleted
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_SENDER_ID to this.senderId.value,
            KEY_SEND_MESSAGE to this.content.value,
            KEY_REPLY_TO_MESSAGE_ID to this.replyToMessageId?.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_IS_DELETED to this.isDeleted.value
        )
    }

    /**
     * Updates the content of the message.
     */
    fun updateContent(newContent: MessageContent) {
        if (this.content == newContent || isDeleted.value) return

        this.content = newContent
        pushDomainEvent(MessageContentUpdatedEvent(this.id, this.content, DateTimeUtil.nowInstant()))
    }

    /**
     * Marks the message as deleted.
     */
    fun delete() {
        if (isDeleted.value) return

        this.isDeleted = MessageIsDeleted.TRUE
    }

    companion object {
        const val COLLECTION_NAME = "messages"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_SEND_MESSAGE = "content"
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
            val message = Message(
                initialSenderId = senderId,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialIsDeleted = MessageIsDeleted.FALSE,
                id = id,
                isNew = true
            )
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
            createdAt: Instant?,
            updatedAt: Instant?,
            isDeleted: MessageIsDeleted
        ): Message {
            return Message(
                initialSenderId = senderId,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialIsDeleted = isDeleted,
                id = id,
                isNew = false
            )
        }
    }
}

