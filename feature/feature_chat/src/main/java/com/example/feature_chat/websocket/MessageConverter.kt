package com.example.feature_chat.websocket

import com.example.domain.model.base.Message
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.event.WebSocketDomainMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메시지 변환기 (호환성 유지용)
 *
 * 기존 MessageConverter 인터페이스를 유지하면서
 * 내부적으로는 새로운 WebSocketDomainMapper를 사용하도록 위임
 *
 * 이를 통해 기존 채팅 코드를 최소한으로 수정하면서도
 * 새로운 중앙 집중식 WebSocket 아키텍처를 사용할 수 있다.
 */
@Singleton
class MessageConverter @Inject constructor(
    private val webSocketDomainMapper: WebSocketDomainMapper
) {

    /**
     * ChatWebSocketEvent.MessageReceived를 도메인 Message로 변환
     * 내부적으로 WebSocketDomainMapper 사용
     */
    fun fromWebSocketEvent(event: ChatWebSocketEvent.MessageReceived): Message {
        // ChatWebSocketEvent를 WebSocketDomainEvent로 변환한 후 도메인 매퍼 사용
        val domainEvent = WebSocketDomainEvent.MessageReceived(
            messageId = event.messageId,
            senderId = event.senderId,
            content = event.content,
            timestamp = event.timestamp,
            replyToMessageId = event.replyToMessageId
        )
        return webSocketDomainMapper.messageReceivedToDomainMessage(domainEvent)
    }

    /**
     * 기존 Message와 MessageEdited 이벤트를 결합하여 수정된 Message 생성
     * 내부적으로 WebSocketDomainMapper 사용
     */
    fun fromWebSocketEdit(
        existingMessage: Message,
        event: ChatWebSocketEvent.MessageEdited
    ): Message {
        // ChatWebSocketEvent를 WebSocketDomainEvent로 변환한 후 도메인 매퍼 사용
        val domainEvent = WebSocketDomainEvent.MessageEdited(
            messageId = event.messageId,
            senderId = event.senderId,
            newContent = event.newContent,
            timestamp = event.timestamp
        )
        return webSocketDomainMapper.applyMessageEdit(existingMessage, domainEvent)
    }
}