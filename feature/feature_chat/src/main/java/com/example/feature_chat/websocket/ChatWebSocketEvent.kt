package com.example.feature_chat.websocket

import com.example.websocket.event.WebSocketDomainEvent

/**
 * 채팅 기능 전용 WebSocket 이벤트 (호환성 유지용)
 *
 * 기존 ChatWebSocketEvent 인터페이스를 유지하면서
 * 내부적으로는 새로운 WebSocketDomainEvent를 사용하도록 브릿지 역할
 *
 * 이를 통해 기존 채팅 UI 코드를 최소한으로 수정하면서도
 * 새로운 중앙 집중식 WebSocket 아키텍처를 사용할 수 있다.
 */
sealed class ChatWebSocketEvent {

    data class MessageReceived(
        val messageId: String,
        val senderId: String,
        val content: String,
        val timestamp: String,
        val replyToMessageId: String? = null
    ) : ChatWebSocketEvent()
    
    data class MessageEdited(
        val messageId: String,
        val senderId: String,
        val newContent: String,
        val timestamp: String
    ) : ChatWebSocketEvent()
    
    data class MessageDeleted(
        val messageId: String,
        val senderId: String,
        val timestamp: String
    ) : ChatWebSocketEvent()
    
    data class SystemMessage(
        val content: String,
        val timestamp: String
    ) : ChatWebSocketEvent()
    
    data class Error(
        val message: String
    ) : ChatWebSocketEvent()
    
    data class MessageAck(
        val messageId: String,
        val ackType: String // WebSocketEventTypes.MESSAGE_ACK, EDIT_MESSAGE_ACK, DELETE_MESSAGE_ACK
    ) : ChatWebSocketEvent()
    
    data class MessageFailed(
        val messageId: String,
        val failureType: String // WebSocketEventTypes.MESSAGE_FAILED, EDIT_MESSAGE_FAILED, DELETE_MESSAGE_FAILED
    ) : ChatWebSocketEvent()
    
    data class Unknown(
        val type: String
    ) : ChatWebSocketEvent()

    companion object {
        /**
         * WebSocketDomainEvent를 ChatWebSocketEvent로 변환
         * 기존 채팅 UI 코드와의 호환성 유지를 위한 변환 함수
         */
        fun fromDomainEvent(domainEvent: WebSocketDomainEvent): ChatWebSocketEvent? {
            return when (domainEvent) {
                is WebSocketDomainEvent.MessageReceived -> MessageReceived(
                    messageId = domainEvent.messageId,
                    senderId = domainEvent.senderId,
                    content = domainEvent.content,
                    timestamp = domainEvent.timestamp,
                    replyToMessageId = domainEvent.replyToMessageId
                )

                is WebSocketDomainEvent.MessageEdited -> MessageEdited(
                    messageId = domainEvent.messageId,
                    senderId = domainEvent.senderId,
                    newContent = domainEvent.newContent,
                    timestamp = domainEvent.timestamp
                )

                is WebSocketDomainEvent.MessageDeleted -> MessageDeleted(
                    messageId = domainEvent.messageId,
                    senderId = domainEvent.senderId,
                    timestamp = domainEvent.timestamp
                )

                is WebSocketDomainEvent.SystemMessage -> SystemMessage(
                    content = domainEvent.content,
                    timestamp = domainEvent.timestamp
                )

                is WebSocketDomainEvent.Error -> Error(
                    message = domainEvent.message
                )

                is WebSocketDomainEvent.MessageAck -> MessageAck(
                    messageId = domainEvent.messageId,
                    ackType = domainEvent.ackType
                )

                is WebSocketDomainEvent.MessageFailed -> MessageFailed(
                    messageId = domainEvent.messageId,
                    failureType = domainEvent.failureType
                )

                is WebSocketDomainEvent.Unknown -> Unknown(
                    type = domainEvent.type
                )

                // 채팅과 직접 관련 없는 이벤트들은 null 반환 (필터링됨)
                is WebSocketDomainEvent.RoomJoined,
                is WebSocketDomainEvent.RoomLeft,
                is WebSocketDomainEvent.UserJoinedRoom,
                is WebSocketDomainEvent.UserLeftRoom,
                is WebSocketDomainEvent.Connected,
                is WebSocketDomainEvent.Disconnected,
                is WebSocketDomainEvent.AuthenticationSucceeded,
                is WebSocketDomainEvent.AuthenticationFailed -> null
            }
        }
    }
}