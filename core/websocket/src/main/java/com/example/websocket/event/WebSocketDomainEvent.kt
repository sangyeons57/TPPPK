package com.example.websocket.event

/**
 * 도메인 레벨의 WebSocket 이벤트들
 * feature 모듈에서 사용할 수 있는 일반화된 WebSocket 이벤트 정의
 *
 * ChatWebSocketEvent에서 일반화하여 다른 feature에서도 재사용 가능하도록 설계
 */
sealed class WebSocketDomainEvent {

    // ================================
    // 메시지 관련 이벤트
    // ================================

    /**
     * 새 메시지가 수신되었을 때
     */
    data class MessageReceived(
        val messageId: String,
        val senderId: String,
        val content: String,
        val timestamp: String,
        val replyToMessageId: String? = null,
        val roomId: String? = null,
        val projectId: String? = null,
        val channelType: String? = null
    ) : WebSocketDomainEvent()

    /**
     * 메시지가 수정되었을 때
     */
    data class MessageEdited(
        val messageId: String,
        val senderId: String,
        val newContent: String,
        val timestamp: String,
        val roomId: String? = null
    ) : WebSocketDomainEvent()

    /**
     * 메시지가 삭제되었을 때
     */
    data class MessageDeleted(
        val messageId: String,
        val senderId: String,
        val timestamp: String,
        val roomId: String? = null
    ) : WebSocketDomainEvent()

    // ================================
    // 메시지 상태 관련 이벤트
    // ================================

    /**
     * 메시지 처리 성공 확인 (ACK)
     */
    data class MessageAck(
        val messageId: String,
        val ackType: String, // WebSocketEventTypes.MESSAGE_ACK, EDIT_MESSAGE_ACK, DELETE_MESSAGE_ACK
        val roomId: String? = null
    ) : WebSocketDomainEvent()

    /**
     * 메시지 처리 실패
     */
    data class MessageFailed(
        val messageId: String,
        val failureType: String, // WebSocketEventTypes.MESSAGE_FAILED, EDIT_MESSAGE_FAILED, DELETE_MESSAGE_FAILED
        val errorMessage: String? = null,
        val roomId: String? = null
    ) : WebSocketDomainEvent()

    // ================================
    // 방 관련 이벤트
    // ================================

    /**
     * 방 입장 성공
     */
    data class RoomJoined(
        val roomId: String,
        val userId: String,
        val timestamp: String
    ) : WebSocketDomainEvent()

    /**
     * 방 퇴장
     */
    data class RoomLeft(
        val roomId: String,
        val userId: String,
        val timestamp: String
    ) : WebSocketDomainEvent()

    /**
     * 다른 사용자가 방에 입장했을 때
     */
    data class UserJoinedRoom(
        val roomId: String,
        val userId: String,
        val username: String? = null,
        val timestamp: String
    ) : WebSocketDomainEvent()

    /**
     * 다른 사용자가 방을 떠났을 때
     */
    data class UserLeftRoom(
        val roomId: String,
        val userId: String,
        val username: String? = null,
        val timestamp: String
    ) : WebSocketDomainEvent()

    // ================================
    // 시스템 이벤트
    // ================================

    /**
     * 시스템 메시지 (공지사항, 알림 등)
     */
    data class SystemMessage(
        val content: String,
        val timestamp: String,
        val roomId: String? = null,
        val messageType: String? = null
    ) : WebSocketDomainEvent()

    /**
     * WebSocket 에러
     */
    data class Error(
        val message: String,
        val errorCode: String? = null,
        val roomId: String? = null
    ) : WebSocketDomainEvent()

    // ================================
    // 연결 상태 이벤트
    // ================================

    /**
     * WebSocket 연결 성공
     */
    data class Connected(
        val timestamp: String
    ) : WebSocketDomainEvent()

    /**
     * WebSocket 연결 해제
     */
    data class Disconnected(
        val reason: String? = null,
        val timestamp: String
    ) : WebSocketDomainEvent()

    /**
     * 인증 성공
     */
    data class AuthenticationSucceeded(
        val userId: String,
        val timestamp: String
    ) : WebSocketDomainEvent()

    /**
     * 인증 실패
     */
    data class AuthenticationFailed(
        val reason: String,
        val timestamp: String
    ) : WebSocketDomainEvent()

    // ================================
    // 기타 이벤트
    // ================================

    /**
     * 알 수 없는 이벤트 타입
     */
    data class Unknown(
        val type: String,
        val rawData: String? = null
    ) : WebSocketDomainEvent()
}