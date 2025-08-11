package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.AggregateRoot
import com.example.domain.event.message.MessageContentUpdatedEvent
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessageIsDeleted
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import java.time.Instant

class Message private constructor(
    initialSenderId: UserId,
    initialMessageType: MessageType,
    initialPayload: MessagePayload,
    initialReplyToMessageId: DocumentId?,
    initialIsDeleted: MessageIsDeleted,
    initialMentions: List<MentionInfo>,
    initialChannelId: ChannelId,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant
) : AggregateRoot() {

    val senderId: UserId = initialSenderId
    val channelId: ChannelId = initialChannelId
    val messageType: MessageType = initialMessageType
    val replyToMessageId: DocumentId? = initialReplyToMessageId
    val mentions: List<MentionInfo> = initialMentions

    var payload: MessagePayload = initialPayload
        private set
    var isDeleted: MessageIsDeleted = initialIsDeleted
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_SENDER_ID to this.senderId.value,
            KEY_CHANNEL_ID to this.channelId.value,
            KEY_MESSAGE_TYPE to this.messageType.name,
            KEY_PAYLOAD to this.payload.value,
            KEY_REPLY_TO_MESSAGE_ID to this.replyToMessageId?.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_IS_DELETED to this.isDeleted.value,
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
     * Updates the payload of the message.
     */
    fun updatePayload(newPayload: MessagePayload) {
        if (this.payload == newPayload || isDeleted.value) return

        this.payload = newPayload
        pushDomainEvent(
            MessageContentUpdatedEvent(
                this.id,
                this.payload,
                DateTimeUtil.nowInstant()
            )
        )
    }

    /**
     * Updates the content of the message (backward compatibility).
     * @deprecated Use updatePayload instead
     */
    @Deprecated(
        "Use updatePayload instead",
        ReplaceWith("updatePayload(MessagePayload.forText(newContent.value))")
    )
    fun updateContent(newContent: MessagePayload) {
        updatePayload(newContent)
    }

    /**
     * Marks the message as deleted.
     */
    fun delete() {
        if (isDeleted.value) return

        this.isDeleted = MessageIsDeleted.TRUE
    }

    // ================================
    // Rich Domain Model Methods
    // ================================

    /**
     * 메시지가 텍스트 메시지인지 확인
     */
    fun isTextMessage(): Boolean {
        return messageType == MessageType.TEXT
    }

    /**
     * 메시지가 이미지 메시지인지 확인
     */
    fun isImageMessage(): Boolean {
        return messageType == MessageType.IMAGE || hasImages()
    }

    /**
     * 메시지가 시스템 메시지인지 확인
     */
    fun isSystemMessage(): Boolean {
        return messageType == MessageType.SYSTEM ||
                messageType == MessageType.SYSTEM_DATE ||
                messageType == MessageType.SYSTEM_PROJECT_JOIN ||
                messageType == MessageType.SYSTEM_PROJECT_LEAVE ||
                messageType == MessageType.SYSTEM_USER_INVITE
    }

    /**
     * 메시지가 특정 사용자의 것인지 확인
     */
    fun isSentBy(userId: UserId): Boolean {
        return senderId == userId
    }

    /**
     * 메시지가 특정 채널에 속하는지 확인
     */
    fun belongsToChannel(channelId: ChannelId): Boolean {
        return this.channelId == channelId
    }

    /**
     * 메시지가 답장인지 확인
     */
    fun isReply(): Boolean {
        return replyToMessageId != null
    }

    /**
     * 메시지에 멘션이 있는지 확인
     */
    fun hasMentions(): Boolean {
        return mentions.isNotEmpty()
    }

    /**
     * 특정 사용자가 멘션되었는지 확인
     */
    fun isMentioned(userId: UserId): Boolean {
        return mentions.any { it.id == userId.value }
    }

    /**
     * 페이로드에서 텍스트 콘텐츠 추출
     */
    fun getTextContent(): String? {
        return payload.getTextContent()
    }

    /**
     * 페이로드에서 이미지 URL들 추출 (간단한 방식)
     */
    fun getImageUrls(): List<String> {
        return try {
            val payloadValue = payload.value
            val imageUrls = mutableListOf<String>()

            // JSON 문자열에서 imageUrl 패턴 찾기
            val singleImageRegex = "\"imageUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            singleImageRegex.find(payloadValue)?.let { match ->
                imageUrls.add(match.groupValues[1])
            }

            // 다중 이미지 패턴 찾기
            val multiImageRegex = "\"url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            multiImageRegex.findAll(payloadValue).forEach { match ->
                imageUrls.add(match.groupValues[1])
            }

            imageUrls.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 메시지에 이미지가 포함되어 있는지 확인
     */
    fun hasImages(): Boolean {
        return getImageUrls().isNotEmpty()
    }

    /**
     * 이미지 개수 반환
     */
    fun getImageCount(): Int {
        return getImageUrls().size
    }

    /**
     * 메시지 요약 정보 생성 (UI나 알림에서 사용)
     */
    fun getSummary(maxLength: Int = 100): String {
        val textContent = getTextContent()
        val imageCount = getImageCount()

        return when {
            isSystemMessage() -> textContent ?: "시스템 메시지"
            hasImages() && !textContent.isNullOrBlank() -> {
                val truncatedText = if (textContent.length > maxLength) {
                    textContent.take(maxLength) + "..."
                } else {
                    textContent
                }
                "$truncatedText (이미지 ${imageCount}개)"
            }

            hasImages() -> "이미지 ${imageCount}개"
            !textContent.isNullOrBlank() -> {
                if (textContent.length > maxLength) {
                    textContent.take(maxLength) + "..."
                } else {
                    textContent
                }
            }

            else -> "빈 메시지"
        }
    }

    /**
     * 메시지가 수정 가능한지 확인
     */
    fun canBeEdited(currentUserId: UserId): Boolean {
        return !isDeleted.value &&
                isSentBy(currentUserId) &&
                !isSystemMessage() &&
                isCreatedRecently()
    }

    /**
     * 메시지가 삭제 가능한지 확인
     */
    fun canBeDeleted(currentUserId: UserId): Boolean {
        return !isDeleted.value &&
                isSentBy(currentUserId) &&
                !isSystemMessage()
    }

    /**
     * 메시지가 최근에 생성되었는지 확인 (30분 이내)
     */
    private fun isCreatedRecently(): Boolean {
        val thirtyMinutesAgo = Instant.now().minusSeconds(30 * 60)
        return createdAt.isAfter(thirtyMinutesAgo)
    }

    /**
     * 메시지 디버그 정보 출력
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== Message Debug Info ===")
            appendLine("ID: ${id.value}")
            appendLine("Sender: ${senderId.value}")
            appendLine("Channel: ${channelId.value}")
            appendLine("Type: ${messageType.name}")
            appendLine("Created: $createdAt")
            appendLine("Updated: $updatedAt")
            appendLine("Deleted: ${isDeleted.value}")
            appendLine("Reply to: ${replyToMessageId?.value ?: "None"}")
            appendLine("Text content: ${getTextContent() ?: "None"}")
            appendLine("Has images: ${hasImages()}")
            if (hasImages()) {
                appendLine("Image URLs: ${getImageUrls().joinToString()}")
            }
            appendLine("Mentions: ${mentions.size}")
            appendLine("Summary: ${getSummary()}")
        }
    }

    companion object {
        const val COLLECTION_NAME = "messages"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_CHANNEL_ID = "channelId"
        const val KEY_MESSAGE_TYPE = "messageType"
        const val KEY_PAYLOAD = "payload"
        const val KEY_REPLY_TO_MESSAGE_ID = "replyToMessageId"
        const val KEY_IS_DELETED = "isDeleted"
        const val KEY_MENTIONS = "mentions"

        // Backward compatibility
        @Deprecated("Use KEY_PAYLOAD instead")
        const val KEY_SEND_MESSAGE = "content"

        /**
         * Factory method for sending a new message.
         */
        fun create(
            id: DocumentId,
            senderId: UserId,
            messageType: MessageType = MessageType.TEXT,
            payload: MessagePayload,
            replyToMessageId: DocumentId?,
            mentions: List<MentionInfo>,
            channelId: ChannelId
        ): Message {
            val message = Message(
                initialSenderId = senderId,
                initialMessageType = messageType,
                initialPayload = payload,
                initialReplyToMessageId = replyToMessageId,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialIsDeleted = MessageIsDeleted.FALSE,
                initialMentions = mentions,
                initialChannelId = channelId,
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
            messageType: MessageType,
            payload: MessagePayload,
            replyToMessageId: DocumentId?,
            createdAt: Instant?,
            updatedAt: Instant?,
            isDeleted: MessageIsDeleted,
            mentions: List<MentionInfo>,
            channelId: ChannelId
        ): Message {
            return Message(
                initialSenderId = senderId,
                initialMessageType = messageType,
                initialPayload = payload,
                initialReplyToMessageId = replyToMessageId,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialIsDeleted = isDeleted,
                initialMentions = mentions,
                initialChannelId = channelId,
                id = id,
                isNew = false
            )
        }

        /**
         * Backward compatibility factory method
         * @deprecated Use the new create method with messageType and payload
         */
        @Deprecated("Use create with messageType and payload parameters")
        fun create(
            id: DocumentId,
            senderId: UserId,
            content: MessagePayload,
            replyToMessageId: DocumentId?,
            mentions: List<MentionInfo>,
            channelId: ChannelId
        ): Message {
            return create(
                id = id,
                senderId = senderId,
                messageType = MessageType.TEXT,
                payload = content,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = channelId
            )
        }

        /**
         * Backward compatibility factory method
         * @deprecated Use the new fromDataSource method with messageType and payload
         */
        @Deprecated("Use fromDataSource with messageType and payload parameters")
        fun fromDataSource(
            id: DocumentId,
            senderId: UserId,
            content: MessagePayload,
            replyToMessageId: DocumentId?,
            createdAt: Instant?,
            updatedAt: Instant?,
            isDeleted: MessageIsDeleted,
            mentions: List<MentionInfo>,
            channelId: ChannelId
        ): Message {
            return fromDataSource(
                id = id,
                senderId = senderId,
                messageType = MessageType.TEXT,
                payload = content,
                replyToMessageId = replyToMessageId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                mentions = mentions,
                channelId = channelId
            )
        }

        /**
         * Factory method for text messages (convenience)
         */
        fun createTextMessage(
            id: DocumentId,
            senderId: UserId,
            textContent: String,
            replyToMessageId: DocumentId? = null,
            mentions: List<MentionInfo> = emptyList(),
            channelId: ChannelId
        ): Message {
            return create(
                id = id,
                senderId = senderId,
                messageType = MessageType.TEXT,
                payload = MessagePayload.forText(textContent),
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = channelId
            )
        }

        /**
         * Factory method for system messages (convenience)
         */
        fun createSystemMessage(
            id: DocumentId,
            senderId: UserId,
            messageType: MessageType,
            payload: MessagePayload,
            channelId: ChannelId
        ): Message {
            return create(
                id = id,
                senderId = senderId,
                messageType = messageType,
                payload = payload,
                replyToMessageId = null,
                mentions = emptyList(),
                channelId = channelId
            )
        }
    }
}

