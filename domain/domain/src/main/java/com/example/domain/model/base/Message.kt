package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.event.message.MessageContentUpdatedEvent
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import java.time.Instant

class Message private constructor(
    initialSenderId: UserId,
    initialContent: MessageContent,
    initialReplyToMessageId: DocumentId?,
    initialIsDeleted: MessageIsDeleted,
    initialMentions: List<MentionInfo>,
    initialDeliveryStatus: OutBoxStatus,
    initialFailureReason: String?,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    val senderId: UserId = initialSenderId
    val replyToMessageId: DocumentId? = initialReplyToMessageId
    val mentions: List<MentionInfo> = initialMentions

    var content: MessageContent = initialContent
        private set
    var isDeleted: MessageIsDeleted = initialIsDeleted
        private set
    var deliveryStatus: OutBoxStatus = initialDeliveryStatus
        private set
    var failureReason: String? = initialFailureReason
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
            KEY_IS_DELETED to this.isDeleted.value,
            KEY_DELIVERY_STATUS to this.deliveryStatus.name,
            KEY_FAILURE_REASON to this.failureReason,
            KEY_MENTIONS to this.mentions.map { 
                mapOf(
                    "type" to it.type.name,
                    "id" to it.id,
                    "displayName" to it.displayName
                )
            }
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

    /**
     * Marks the message as successfully delivered (ACK received).
     */
    fun markAsDelivered() {
        if (deliveryStatus.canTransitionTo(OutBoxStatus.COMPLETED)) {
            this.deliveryStatus = OutBoxStatus.COMPLETED
            this.failureReason = null
        }
    }

    /**
     * Marks the message as failed with an optional error reason.
     */
    fun markAsFailed(errorReason: String? = null) {
        if (deliveryStatus.canTransitionTo(OutBoxStatus.FAILED)) {
            this.deliveryStatus = OutBoxStatus.FAILED
            this.failureReason = errorReason
        }
    }

    /**
     * Marks the message as pending (for retry scenarios).
     */
    fun markAsPending() {
        if (deliveryStatus.canTransitionTo(OutBoxStatus.PENDING)) {
            this.deliveryStatus = OutBoxStatus.PENDING
            this.failureReason = null
        }
    }

    /**
     * Checks if the message delivery is still in progress.
     */
    fun isDeliveryPending(): Boolean = deliveryStatus.isActive()

    /**
     * Checks if the message was successfully delivered.
     */
    fun isDelivered(): Boolean = deliveryStatus == OutBoxStatus.COMPLETED

    /**
     * Checks if the message delivery failed.
     */
    fun isDeliveryFailed(): Boolean = deliveryStatus == OutBoxStatus.FAILED

    companion object {
        const val COLLECTION_NAME = "messages"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_SEND_MESSAGE = "content"
        const val KEY_REPLY_TO_MESSAGE_ID = "replyToMessageId"
        const val KEY_IS_DELETED = "isDeleted"
        const val KEY_DELIVERY_STATUS = "deliveryStatus"
        const val KEY_FAILURE_REASON = "failureReason"
        const val KEY_MENTIONS = "mentions"

        /**
         * Factory method for sending a new message.
         */
        fun create(
            id: DocumentId,
            senderId: UserId,
            content: MessageContent,
            replyToMessageId: DocumentId?,
            mentions: List<MentionInfo>
        ): Message {
            val message = Message(
                initialSenderId = senderId,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialIsDeleted = MessageIsDeleted.FALSE,
                initialMentions = mentions,
                initialDeliveryStatus = OutBoxStatus.PENDING, // 새 메시지는 PENDING 상태
                initialFailureReason = null,
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
            isDeleted: MessageIsDeleted,
            mentions: List<MentionInfo>,
            deliveryStatus: OutBoxStatus = OutBoxStatus.COMPLETED, // 데이터소스에서 온 메시지는 기본적으로 COMPLETED
            failureReason: String? = null
        ): Message {
            return Message(
                initialSenderId = senderId,
                initialContent = content,
                initialReplyToMessageId = replyToMessageId,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialIsDeleted = isDeleted,
                initialMentions = mentions,
                initialDeliveryStatus = deliveryStatus,
                initialFailureReason = failureReason,
                id = id,
                isNew = false
            )
        }
    }
}

