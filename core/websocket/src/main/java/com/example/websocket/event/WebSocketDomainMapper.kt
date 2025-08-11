package com.example.websocket.event

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessageIsDeleted
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.websocket.constant.WebSocketEventTypes
import com.example.websocket.constant.WebSocketFieldConstants
import com.example.websocket.core.WebSocketMessage
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 이벤트와 도메인 모델 간의 변환을 담당하는 매퍼
 *
 * MessageConverter에서 일반화하여 core 모듈에서 WebSocket 이벤트와
 * 도메인 모델 간의 변환을 중앙 집중적으로 처리
 */
@Singleton
class WebSocketDomainMapper @Inject constructor() {

    // ================================
    // WebSocket 메시지 → 도메인 모델 변환
    // ================================

    /**
     * WebSocketMessage를 WebSocketDomainEvent로 변환
     */
    fun webSocketMessageToDomainEvent(
        message: WebSocketMessage,
        roomId: String? = null
    ): WebSocketDomainEvent {
        return when (message.type) {
            WebSocketMessage.TYPE_MESSAGE -> {
                WebSocketDomainEvent.MessageReceived(
                    messageId = message.messageId ?: "",
                    senderId = message.senderId ?: "",
                    content = message.getTextContent() ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString(),
                    replyToMessageId = message.replyToMessageId,
                    roomId = roomId ?: message.roomId,
                    projectId = message.projectId,
                    channelType = message.channelType
                )
            }

            WebSocketMessage.TYPE_EDIT_MESSAGE -> {
                WebSocketDomainEvent.MessageEdited(
                    messageId = message.messageId ?: "",
                    senderId = message.senderId ?: "",
                    newContent = message.getTextContent() ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString(),
                    roomId = roomId ?: message.roomId
                )
            }

            WebSocketMessage.TYPE_DELETE_MESSAGE -> {
                WebSocketDomainEvent.MessageDeleted(
                    messageId = message.messageId ?: "",
                    senderId = message.senderId ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString(),
                    roomId = roomId ?: message.roomId
                )
            }

            WebSocketMessage.TYPE_ACK -> {
                // 서버에서 보내는 일반 ACK - replyToMessageId로 메시지 ACK임을 판단
                WebSocketDomainEvent.MessageAck(
                    messageId = message.replyToMessageId
                        ?: "", // ACK의 경우 replyToMessageId가 원본 메시지 ID
                    ackType = WebSocketMessage.TYPE_MESSAGE_ACK, // 일반 메시지 ACK로 처리
                    roomId = roomId ?: message.roomId
                )
            }
            
            WebSocketMessage.TYPE_MESSAGE_ACK,
            WebSocketMessage.TYPE_EDIT_MESSAGE_ACK,
            WebSocketMessage.TYPE_DELETE_MESSAGE_ACK -> {
                WebSocketDomainEvent.MessageAck(
                    messageId = message.messageId ?: "",
                    ackType = message.type,
                    roomId = roomId ?: message.roomId
                )
            }

            WebSocketEventTypes.MESSAGE_FAILED,
            WebSocketEventTypes.EDIT_MESSAGE_FAILED,
            WebSocketEventTypes.DELETE_MESSAGE_FAILED -> {
                WebSocketDomainEvent.MessageFailed(
                    messageId = message.messageId ?: "",
                    failureType = message.type,
                    errorMessage = message.getTextContent(),
                    roomId = roomId ?: message.roomId
                )
            }

            WebSocketEventTypes.JOINED_ROOM -> {
                WebSocketDomainEvent.RoomJoined(
                    roomId = roomId ?: message.roomId ?: "",
                    userId = message.senderId ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString()
                )
            }

            WebSocketEventTypes.LEFT_ROOM -> {
                WebSocketDomainEvent.RoomLeft(
                    roomId = roomId ?: message.roomId ?: "",
                    userId = message.senderId ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString()
                )
            }

            WebSocketMessage.TYPE_SYSTEM -> {
                WebSocketDomainEvent.SystemMessage(
                    content = message.getTextContent() ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString(),
                    roomId = roomId ?: message.roomId
                )
            }

            WebSocketMessage.TYPE_ERROR -> {
                WebSocketDomainEvent.Error(
                    message = message.getTextContent() ?: "Unknown error",
                    roomId = roomId ?: message.roomId
                )
            }

            WebSocketMessage.TYPE_AUTH_SUCCESS -> {
                WebSocketDomainEvent.AuthenticationSucceeded(
                    userId = message.senderId ?: "",
                    timestamp = message.timestamp?.let {
                        Instant.ofEpochSecond(it.toLong()).toString()
                    } ?: Instant.now().toString()
                )
            }

            else -> {
                WebSocketDomainEvent.Unknown(
                    type = message.type,
                    rawData = message.getTextContent()
                )
            }
        }
    }

    // ================================
    // WebSocket 이벤트 → 도메인 Message 변환
    // ================================

    /**
     * WebSocketDomainEvent.MessageReceived를 도메인 Message로 변환
     */
    fun messageReceivedToDomainMessage(event: WebSocketDomainEvent.MessageReceived): Message {
        if (event.roomId == null) throw Exception("roomId is null")
        return Message.fromDataSource(
            id = DocumentId(event.messageId),
            senderId = UserId(event.senderId),
            messageType = MessageType.TEXT,
            payload = MessagePayload.forText(event.content),
            replyToMessageId = event.replyToMessageId?.let { DocumentId(it) },
            createdAt = parseTimestamp(event.timestamp),
            updatedAt = parseTimestamp(event.timestamp),
            isDeleted = MessageIsDeleted.FALSE,
            mentions = emptyList(), // TODO: WebSocket에서 mentions 파싱 지원 시 추가
            channelId = ChannelId(event.roomId)
        )
    }

    /**
     * 기존 Message와 MessageEdited 이벤트를 결합하여 수정된 Message 생성
     */
    fun applyMessageEdit(
        existingMessage: Message,
        event: WebSocketDomainEvent.MessageEdited
    ): Message {
        if (event.roomId == null) throw Exception("roomId is null")
        return Message.fromDataSource(
            id = existingMessage.id,
            senderId = existingMessage.senderId,
            messageType = existingMessage.messageType,
            payload = MessagePayload.forText(event.newContent),
            replyToMessageId = existingMessage.replyToMessageId,
            createdAt = existingMessage.createdAt,
            updatedAt = parseTimestamp(event.timestamp),
            isDeleted = existingMessage.isDeleted,
            mentions = existingMessage.mentions, // 기존 mentions 유지
            channelId = ChannelId(event.roomId)
        )
    }

    /**
     * 기존 Message에 삭제 표시 적용
     */
    fun applyMessageDeletion(
        existingMessage: Message,
        event: WebSocketDomainEvent.MessageDeleted
    ): Message {
        if (event.roomId == null) throw Exception("roomId is null")
        return Message.fromDataSource(
            id = existingMessage.id,
            senderId = existingMessage.senderId,
            messageType = existingMessage.messageType,
            payload = existingMessage.payload,
            replyToMessageId = existingMessage.replyToMessageId,
            createdAt = existingMessage.createdAt,
            updatedAt = parseTimestamp(event.timestamp),
            isDeleted = MessageIsDeleted.TRUE,
            mentions = existingMessage.mentions,
            channelId = ChannelId(event.roomId)
        )
    }

    // ================================
    // 도메인 → WebSocket 메시지 변환
    // ================================

    /**
     * 도메인 Message를 WebSocketMessage로 변환 (전송용)
     */
    fun domainMessageToWebSocketMessage(
        message: Message,
        roomId: String,
        messageType: String = WebSocketMessage.TYPE_MESSAGE,
        projectId: String? = null,
        channelType: String? = null
    ): WebSocketMessage {
        val payload = mapOf(WebSocketFieldConstants.PAYLOAD_CONTENT to (message.payload.getTextContent() ?: ""))
        return WebSocketMessage(
            type = messageType,
            roomId = roomId,
            senderId = message.senderId.value,
            payload = payload,
            messageId = message.id.value,
            replyToMessageId = message.replyToMessageId?.value,
            timestamp = message.createdAt.epochSecond.toDouble(),
            projectId = projectId,
            channelType = channelType
        )
    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 타임스탬프 문자열을 Instant로 파싱
     * 파싱 실패 시 현재 시간 반환
     */
    private fun parseTimestamp(timestampString: String): Instant {
        return try {
            Instant.parse(timestampString)
        } catch (e: Exception) {
            DateTimeUtil.nowInstant()
        }
    }

    /**
     * WebSocketDomainEvent가 특정 방과 관련된 이벤트인지 확인
     */
    fun isRoomRelatedEvent(event: WebSocketDomainEvent, roomId: String): Boolean {
        return when (event) {
            is WebSocketDomainEvent.MessageReceived -> event.roomId == roomId
            is WebSocketDomainEvent.MessageEdited -> event.roomId == roomId
            is WebSocketDomainEvent.MessageDeleted -> event.roomId == roomId
            is WebSocketDomainEvent.MessageAck -> event.roomId == roomId
            is WebSocketDomainEvent.MessageFailed -> event.roomId == roomId
            is WebSocketDomainEvent.RoomJoined -> event.roomId == roomId
            is WebSocketDomainEvent.RoomLeft -> event.roomId == roomId
            is WebSocketDomainEvent.UserJoinedRoom -> event.roomId == roomId
            is WebSocketDomainEvent.UserLeftRoom -> event.roomId == roomId
            is WebSocketDomainEvent.SystemMessage -> event.roomId == roomId
            is WebSocketDomainEvent.Error -> event.roomId == roomId
            else -> false // 연결 관련 이벤트는 특정 방과 무관
        }
    }

    /**
     * 메시지 관련 이벤트인지 확인
     */
    fun isMessageEvent(event: WebSocketDomainEvent): Boolean {
        return when (event) {
            is WebSocketDomainEvent.MessageReceived,
            is WebSocketDomainEvent.MessageEdited,
            is WebSocketDomainEvent.MessageDeleted,
            is WebSocketDomainEvent.MessageAck,
            is WebSocketDomainEvent.MessageFailed -> true

            else -> false
        }
    }
}