package com.example.feature_chat.websocket

import android.util.Log
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.feature_chat.util.ChatLogUtil
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.usecase.RoomWebSocketUseCases
import com.example.websocket.usecase.WebSocketUseCaseProvider
import com.example.websocket.usecase.WebSocketUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 기능을 위한 WebSocket 클라이언트 (리팩토링된 버전)
 *
 * 기존 ChatWebSocketClient의 인터페이스를 유지하면서
 * 내부적으로는 새로운 WebSocketUseCaseProvider를 사용하여
 * Clean Architecture 원칙을 따르고 중앙 집중식 WebSocket 관리를 활용한다.
 *
 * 변경사항:
 * - GlobalWebSocketService 직접 사용 → WebSocketUseCaseProvider 사용
 * - 복잡한 WebSocket 로직을 core:websocket으로 이동
 * - 채팅 관련 로직만 유지하고 나머지는 use case에 위임
 * - 기존 인터페이스 호환성 유지로 기존 UI 코드 수정 최소화
 */
@Singleton
class ChatWebSocketClient @Inject constructor(
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider
) {

    // WebSocket 사용 사례들
    private val webSocketUseCases: WebSocketUseCases = webSocketUseCaseProvider.create()

    // 연결 상태 (UseCase를 통해 접근)
    val connectionState: Flow<WebSocketConnectionState> =
        webSocketUseCases.getConnectionStateUseCase()
    val isAuthenticated: Flow<Boolean> = webSocketUseCases.getAuthenticationStateUseCase()

    /**
     * 특정 방의 채팅 메시지 이벤트 스트림 반환
     *
     * 기존 인터페이스를 유지하면서 내부적으로는 새로운 WebSocket 아키텍처 사용
     */
    fun getChatMessages(roomId: String): Flow<ChatWebSocketEvent> {
        val correlationId = ChatLogUtil.generateCorrelationId()
        Log.d(
            ChatLogUtil.TAG_WEBSOCKET, ChatLogUtil.formatLogMessage(
            correlationId = correlationId,
                message = "getChatMessages 시작 (새 아키텍처)",
            roomId = roomId
        ))

        // 방별 WebSocket 사용 사례 생성
        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)

        return roomUseCases.subscribeToRoomEventsUseCase()
            .onEach { domainEvent ->
                val correlationId = ChatLogUtil.generateCorrelationId()
                Log.i(
                    ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                        correlationId = correlationId,
                        message = "WebSocket 도메인 이벤트 수신: ${domainEvent::class.simpleName}",
                        roomId = roomId
                    )
                )
            }
            .mapNotNull { domainEvent ->
                // WebSocketDomainEvent를 ChatWebSocketEvent로 변환
                ChatWebSocketEvent.fromDomainEvent(domainEvent)
            }
            .onEach { chatEvent ->
                val correlationId = ChatLogUtil.generateCorrelationId()
                Log.i(
                    ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                        correlationId = correlationId,
                        message = "채팅 이벤트 변환 완료: ${chatEvent::class.simpleName}",
                        roomId = roomId
                    )
                )
            }
    }

    /**
     * WebSocket 연결 (UseCase 위임)
     */
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        val correlationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = correlationId,
                message = "WebSocket 연결 시도 (UseCase 사용)",
            metadata = mapOf("serverUrl" to serverUrl)
        ))
        
        return try {
            val result = webSocketUseCases.connectUseCase(serverUrl, authToken)
            
            val connectionCorrelationId = ChatLogUtil.generateCorrelationId()
            if (result.isSuccess) {
                Log.i(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                        correlationId = connectionCorrelationId,
                        message = "WebSocket 연결 성공 (UseCase 사용)",
                        metadata = mapOf("serverUrl" to serverUrl)
                    )
                )
            } else {
                Log.e(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                        correlationId = connectionCorrelationId,
                        message = "WebSocket 연결 실패: ${result.exceptionOrNull()?.message}",
                        metadata = mapOf("serverUrl" to serverUrl)
                    )
                )
            }
            
            result
        } catch (e: Exception) {
            val errorCorrelationId = ChatLogUtil.generateCorrelationId()
            Log.e(
                ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                correlationId = errorCorrelationId,
                    message = "WebSocket 연결 중 예외 발생: ${e.message}",
                metadata = mapOf("serverUrl" to serverUrl)
                ), e
            )
            Result.failure(e)
        }
    }
    
    /**
     * UserSession을 사용한 WebSocket 연결 (UseCase 위임)
     */
    suspend fun connectWithSession(serverUrl: String, userSession: UserSession): Result<Unit> {
        val correlationId = ChatLogUtil.generateCorrelationId()
        
        // 토큰 유효성 검증
        if (userSession.idToken == null) {
            val error = Exception("Invalid or expired token in user session")
            Log.e(
                ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                correlationId = correlationId,
                message = "WebSocket 연결 실패: 유효하지 않은 토큰",
                userId = userSession.userId.value,
            ))
            return Result.failure(error)
        }

        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = correlationId,
                message = "UserSession으로 WebSocket 연결 시도 (UseCase 사용)",
            userId = userSession.userId.value,
        ))

        return webSocketUseCases.connectWithSessionUseCase(serverUrl, userSession)
    }

    /**
     * WebSocket 인증 대기 (UseCase 위임)
     */
    suspend fun waitForAuthentication(userId: UserId, timeoutMs: Long = 15000): Result<Unit> {
        val authCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = authCorrelationId,
                message = "WebSocket 인증 확인 대기 (UseCase 사용, 타임아웃: ${timeoutMs}ms)",
            userId = userId.value
        ))
        
        return try {
            val result = webSocketUseCases.waitForAuthenticationUseCase(userId, timeoutMs)

            if (result.isSuccess) {
                Log.i(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                        correlationId = authCorrelationId,
                        message = "WebSocket 인증 성공",
                        userId = userId.value
                    )
                )
            } else {
                Log.e(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                        correlationId = authCorrelationId,
                        message = "WebSocket 인증 실패: ${result.exceptionOrNull()?.message}",
                        userId = userId.value
                    )
                )
            }

            result
        } catch (e: Exception) {
            Log.e(
                ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                correlationId = authCorrelationId,
                message = "WebSocket 인증 대기 중 오류 발생: ${e.message}",
                userId = userId.value
            ), e)
            Result.failure(e)
        }
    }

    /**
     * WebSocket 연결 해제 (UseCase 위임)
     */
    suspend fun disconnect() {
        val disconnectCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = disconnectCorrelationId,
                message = "WebSocket 연결 해제 시도 (UseCase 사용)"
        ))

        webSocketUseCases.disconnectUseCase()

        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = disconnectCorrelationId,
                message = "개별 채팅 기능 종료 완료 (GlobalWebSocketService는 계속 활성 상태)"
        ))
    }

    /**
     * 방 입장 (UseCase 위임)
     */
    suspend fun joinRoom(roomId: String, userId: UserId? = null): Result<Unit> {
        val joinCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = joinCorrelationId,
                message = "채팅방 입장 시도 (UseCase 사용)",
            roomId = roomId,
            userId = userId?.value
        ))

        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return try {
            val result = roomUseCases.joinRoomUseCase(userId)

            if (result.isSuccess) {
                Log.i(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                    correlationId = joinCorrelationId,
                        message = "채팅방 입장 성공",
                    roomId = roomId,
                    userId = userId?.value
                ))
            } else {
                Log.e(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                        correlationId = joinCorrelationId,
                        message = "채팅방 입장 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId,
                    userId = userId?.value
                ))
            }

            result
        } catch (e: Exception) {
            Log.e(
                ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                correlationId = joinCorrelationId,
                message = "채팅방 입장 중 예외 발생: ${e.message}",
                roomId = roomId,
                userId = userId?.value
            ), e)
            Result.failure(e)
        }
    }

    /**
     * 방 퇴장 (UseCase 위임)
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        val leaveCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
            correlationId = leaveCorrelationId,
                message = "채팅방 퇴장 시도 (UseCase 사용)",
            roomId = roomId
        ))

        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return roomUseCases.leaveRoomUseCase().also { result ->
            if (result.isSuccess) {
                Log.i(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                    correlationId = leaveCorrelationId,
                    message = "채팅방 퇴장 성공",
                    roomId = roomId
                ))
            } else {
                Log.e(
                    ChatLogUtil.TAG_CONNECTION, ChatLogUtil.formatLogMessage(
                    correlationId = leaveCorrelationId,
                    message = "채팅방 퇴장 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId
                ))
            }
        }
    }
    
    /**
     * 방 입장 상태 확인 (UseCase 위임)
     */
    fun isRoomJoined(roomId: String): Boolean {
        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return roomUseCases.isRoomJoinedUseCase()
    }

    /**
     * 방 입장 진행 중 상태 확인 (UseCase 위임)
     */
    fun isRoomJoining(roomId: String): Boolean {
        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return roomUseCases.isRoomJoiningUseCase()
    }
    
    /**
     * 모든 방에서 퇴장 (UseCase 위임)
     * 호환성을 위해 유지하지만 실제로는 WebSocketMessageService에서 처리
     */
    fun clearAllRooms() {
        Log.i(ChatLogUtil.TAG_CONNECTION, "모든 방 상태 정리 (UseCase에서 처리됨)")
        // WebSocketMessageService에서 중앙 집중적으로 처리되므로 별도 작업 불필요
    }

    /**
     * 메시지 전송 (UseCase 위임)
     */
    suspend fun sendMessage(
        roomId: String,
        senderId: UserId,
        content: String,
        messageId: DocumentId,
        replyToMessageId: DocumentId? = null,
        projectId: String? = null,
        channelType: String? = null
    ): Result<Unit> {
        val sendCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
            correlationId = sendCorrelationId,
                message = "메시지 전송 시도 (UseCase 사용)",
            userId = senderId.value,
            roomId = roomId,
            messageId = messageId.value,
            metadata = mapOf("contentLength" to content.length.toString())
        ))

        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return roomUseCases.sendMessageUseCase(
            senderId = senderId,
            content = content,
            messageId = messageId,
            replyToMessageId = replyToMessageId,
            projectId = projectId,
            channelType = channelType
        ).also { result ->
            val status = if (result.isSuccess) "SUCCESS" else "FAILED"
            Log.i(
                ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                correlationId = sendCorrelationId,
                message = "WebSocket 메시지 SEND $status",
                userId = senderId.value,
                roomId = roomId,
                messageId = messageId.value,
                metadata = mapOf("action" to "SEND", "status" to status)
            ))
            
            if (result.isFailure) {
                Log.e(
                    ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                    correlationId = sendCorrelationId,
                    message = "메시지 전송 실패: ${result.exceptionOrNull()?.message}",
                    userId = senderId.value,
                    roomId = roomId,
                    messageId = messageId.value
                ))
            }
        }
    }

    /**
     * 메시지 수정 (UseCase 위임)
     */
    suspend fun editMessage(
        roomId: String,
        messageId: DocumentId,
        newContent: String,
        projectId: String? = null,
        channelType: String? = null
    ): Result<Unit> {
        val editCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
            correlationId = editCorrelationId,
                message = "메시지 수정 시도 (UseCase 사용)",
            roomId = roomId,
            messageId = messageId.value,
            metadata = mapOf("newContentLength" to newContent.length.toString())
        ))

        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return roomUseCases.editMessageUseCase(
            messageId = messageId,
            newContent = newContent,
            projectId = projectId,
            channelType = channelType
        ).also { result ->
            val status = if (result.isSuccess) "SUCCESS" else "FAILED"
            Log.i(
                ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                correlationId = editCorrelationId,
                message = "WebSocket 메시지 EDIT $status",
                roomId = roomId,
                messageId = messageId.value,
                metadata = mapOf("action" to "EDIT", "status" to status)
            ))
            
            if (result.isFailure) {
                Log.e(
                    ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                    correlationId = editCorrelationId,
                    message = "메시지 수정 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId,
                    messageId = messageId.value
                ))
            }
        }
    }

    /**
     * 메시지 삭제 (UseCase 위임)
     */
    suspend fun deleteMessage(
        roomId: String,
        messageId: DocumentId,
        projectId: String? = null,
        channelType: String? = null
    ): Result<Unit> {
        val deleteCorrelationId = ChatLogUtil.generateCorrelationId()
        Log.i(
            ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
            correlationId = deleteCorrelationId,
                message = "메시지 삭제 시도 (UseCase 사용)",
            roomId = roomId,
            messageId = messageId.value
        ))

        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
        return roomUseCases.deleteMessageUseCase(
            messageId = messageId,
            projectId = projectId,
            channelType = channelType
        ).also { result ->
            val status = if (result.isSuccess) "SUCCESS" else "FAILED"
            Log.i(
                ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                correlationId = deleteCorrelationId,
                message = "WebSocket 메시지 DELETE $status",
                roomId = roomId,
                messageId = messageId.value,
                metadata = mapOf("action" to "DELETE", "status" to status)
            ))
            
            if (result.isFailure) {
                Log.e(
                    ChatLogUtil.TAG_MESSAGE, ChatLogUtil.formatLogMessage(
                    correlationId = deleteCorrelationId,
                    message = "메시지 삭제 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId,
                    messageId = messageId.value
                ))
            }
        }
    }
}