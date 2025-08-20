package com.example.websocket.usecase

import com.example.domain.model.data.UserSession
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.event.WebSocketDomainMapper
import com.example.websocket.service.WebSocketMessageService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 관련 UseCase들을 제공하는 Provider
 *
 * Clean Architecture의 UseCase Provider 패턴을 따라
 * feature 모듈에서 WebSocket 기능을 사용할 수 있도록
 * 사용 사례별로 정리된 인터페이스를 제공한다.
 *
 * ViewModel은 이 Provider를 주입받아 WebSocket 기능을 사용하며,
 * Repository나 저수준 서비스에 직접 의존하지 않는다.
 */
@Singleton
class WebSocketUseCaseProvider @Inject constructor(
    private val webSocketMessageService: WebSocketMessageService,
    private val webSocketDomainMapper: WebSocketDomainMapper
) {

    /**
     * 일반적인 WebSocket 사용 사례들을 제공
     */
    fun create(): WebSocketUseCases {
        return WebSocketUseCases(
            webSocketMessageService = webSocketMessageService,
            webSocketDomainMapper = webSocketDomainMapper
        )
    }

    /**
     * 특정 방에 대한 WebSocket 사용 사례들을 제공
     */
    fun createForRoom(roomId: String): RoomWebSocketUseCases {
        return RoomWebSocketUseCases(
            roomId = roomId,
            webSocketMessageService = webSocketMessageService,
            webSocketDomainMapper = webSocketDomainMapper
        )
    }
}

/**
 * 일반적인 WebSocket 사용 사례들
 */
class WebSocketUseCases(
    private val webSocketMessageService: WebSocketMessageService,
    private val webSocketDomainMapper: WebSocketDomainMapper
) {

    // ================================
    // 연결 관리 Use Cases
    // ================================

    /**
     * WebSocket 연결
     */
    suspend fun connectUseCase(serverUrl: String, authToken: String): Result<Unit> {
        return webSocketMessageService.connect(serverUrl, authToken)
    }

    /**
     * UserSession을 사용한 WebSocket 연결
     */
    suspend fun connectWithSessionUseCase(
        serverUrl: String,
        userSession: UserSession
    ): Result<Unit> {
        return webSocketMessageService.connectWithSession(serverUrl, userSession)
    }

    /**
     * WebSocket 인증 대기
     */
    suspend fun waitForAuthenticationUseCase(
        userId: UserId,
        timeoutMs: Long = 15000
    ): Result<Unit> {
        return webSocketMessageService.waitForAuthentication(userId, timeoutMs)
    }

    /**
     * WebSocket 연결 해제
     */
    suspend fun disconnectUseCase() {
        webSocketMessageService.disconnect()
    }

    // ================================
    // 이벤트 스트림 Use Cases
    // ================================

    /**
     * 모든 WebSocket 이벤트 구독
     */
    fun subscribeToAllEventsUseCase(): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getAllEvents()
    }

    /**
     * 메시지 관련 이벤트만 구독
     */
    fun subscribeToMessageEventsUseCase(): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getMessageEvents()
    }

    /**
     * 연결 상태 이벤트 구독
     */
    fun subscribeToConnectionEventsUseCase(): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getConnectionEvents()
    }

    /**
     * ACK/실패 이벤트 구독
     */
    fun subscribeToAckEventsUseCase(): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getAckEvents()
    }


    // ================================
    // 상태 확인 Use Cases
    // ================================

    /**
     * 연결 상태 확인
     */
    fun getConnectionStateUseCase(): Flow<WebSocketConnectionState> {
        return webSocketMessageService.connectionState
    }

    /**
     * 인증 상태 확인
     */
    fun getAuthenticationStateUseCase(): Flow<Boolean> {
        return webSocketMessageService.isAuthenticated
    }
}

/**
 * 특정 방에 대한 WebSocket 사용 사례들
 */
class RoomWebSocketUseCases(
    private val roomId: String,
    private val webSocketMessageService: WebSocketMessageService,
    private val webSocketDomainMapper: WebSocketDomainMapper
) {

    // ================================
    // 방 관리 Use Cases
    // ================================

    /**
     * 방 입장
     */
    suspend fun joinRoomUseCase(userId: UserId? = null): Result<Unit> {
        return webSocketMessageService.joinRoom(roomId, userId)
    }

    /**
     * 방 퇴장
     */
    suspend fun leaveRoomUseCase(): Result<Unit> {
        return webSocketMessageService.leaveRoom(roomId)
    }

    /**
     * 방 입장 상태 확인
     */
    fun isRoomJoinedUseCase(): Boolean {
        return webSocketMessageService.isRoomJoined(roomId)
    }

    /**
     * 방 입장 진행 중 상태 확인
     */
    fun isRoomJoiningUseCase(): Boolean {
        return webSocketMessageService.isRoomJoining(roomId)
    }

    // ================================
    // 메시지 전송 Use Cases
    // ================================

    /**
     * 메시지 전송 (도메인 Message 기반 단일 API)
     */
    suspend fun sendMessageUseCase(
        message: com.example.domain.model.base.Message
    ): Result<Unit> {
        return webSocketMessageService.sendMessage(
            roomId = roomId,
            message = message
        )
    }

    // 도메인 기반 API 통합: payload 기반 API만 유지

    /**
     * 메시지 수정 (payload 기반)
     */
    suspend fun editMessageUseCase(
        messageId: DocumentId,
        newPayload: com.example.domain.vo.message.MessagePayload,
        channelType: String? = null
    ): Result<Unit> {
        return webSocketMessageService.editMessage(
            roomId = roomId,
            messageId = messageId,
            newPayload = newPayload,
            channelType = channelType
        )
    }

    /**
     * 메시지 수정 (텍스트 content - 하위 호환용)
     */
    suspend fun editMessageWithTextUseCase(
        messageId: DocumentId,
        newContent: String,
        channelType: String? = null
    ): Result<Unit> {
        val newPayload = com.example.domain.vo.message.MessagePayload.forText(newContent)
        return editMessageUseCase(messageId, newPayload, channelType)
    }

    /**
     * 메시지 삭제
     */
    suspend fun deleteMessageUseCase(
        messageId: DocumentId,
        channelType: String? = null
    ): Result<Unit> {
        return webSocketMessageService.deleteMessage(
            roomId = roomId,
            messageId = messageId,
            channelType = channelType
        )
    }

    // ================================
    // 이벤트 스트림 Use Cases (방 전용)
    // ================================

    /**
     * 이 방의 모든 이벤트 구독
     */
    fun subscribeToRoomEventsUseCase(): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getEventsForRoom(roomId)
    }

    /**
     * 이 방의 메시지 이벤트만 구독
     */
    fun subscribeToRoomMessageEventsUseCase(): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getMessageEventsForRoom(roomId)
    }

    /**
     * 특정 메시지의 ACK/실패 이벤트 구독
     */
    fun subscribeToMessageAckEventsUseCase(messageId: String): Flow<WebSocketDomainEvent> {
        return webSocketMessageService.getAckEventsForMessage(messageId)
    }

    // ================================
    // 도메인 변환 Use Cases
    // ================================

    /**
     * WebSocket 메시지 이벤트를 도메인 Message로 변환
     */
    fun convertMessageEventToDomainUseCase(event: WebSocketDomainEvent.MessageReceived) =
        webSocketDomainMapper.messageReceivedToDomainMessage(event)

    /**
     * 메시지 수정 이벤트를 기존 Message에 적용
     */
    fun applyMessageEditUseCase(
        existingMessage: com.example.domain.model.base.Message,
        event: WebSocketDomainEvent.MessageEdited
    ) = webSocketDomainMapper.applyMessageEdit(existingMessage, event)

    /**
     * 메시지 삭제 이벤트를 기존 Message에 적용
     */
    fun applyMessageDeletionUseCase(
        existingMessage: com.example.domain.model.base.Message,
        event: WebSocketDomainEvent.MessageDeleted
    ) = webSocketDomainMapper.applyMessageDeletion(existingMessage, event)
}
