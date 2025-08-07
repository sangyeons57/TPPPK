package com.example.websocket.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.data.UserSession
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessageIsDeleted
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.MessageRepository
import com.example.websocket.constant.OperationStatus
import com.example.websocket.constant.WebSocketEventTypes
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.core.WebSocketManager
import com.example.websocket.core.WebSocketMessage
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.event.WebSocketEventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 중앙 집중식 WebSocket 메시지 서비스
 *
 * ChatWebSocketClient에서 일반화하여 모든 feature에서 사용할 수 있는
 * WebSocket 메시징 기능을 제공한다.
 *
 * 책임:
 * - WebSocket 연결 관리 (GlobalWebSocketService 위임)
 * - 메시지 전송 (텍스트, 수정, 삭제)
 * - 방 관리 (입장, 퇴장)
 * - 이벤트 스트림 제공 (WebSocketEventFlow 통합)
 * - 인증 및 연결 상태 관리
 */
@Singleton
class WebSocketMessageService @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService,
    private val webSocketEventFlow: WebSocketEventFlow,
    private val messageRepository: MessageRepository
) {

    // GlobalWebSocketService에서 연결 상태와 WebSocketManager 위임
    val connectionState = globalWebSocketService.globalConnectionState
    private val webSocketManager: WebSocketManager = globalWebSocketService.getWebSocketManager()
    val isAuthenticated = webSocketManager.isAuthenticated

    // 방 입장 상태 추적
    private val joiningRooms = mutableSetOf<String>()
    private val joinedRooms = mutableSetOf<String>()

    // 코루틴 스코프 (자동 저장을 위한)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Paging3 새로고침 이벤트 스트림
    private val _messageRefreshEvents = MutableSharedFlow<String>() // channelId를 전달
    val messageRefreshEvents: Flow<String> = _messageRefreshEvents.asSharedFlow()

    init {
        // WebSocketEventFlow 초기화 - GlobalWebSocketService의 메시지 스트림 연결
        webSocketEventFlow.initialize(webSocketManager.incomingMessages)

        // SSOT 패턴: WebSocket 메시지 이벤트를 Room DB에 자동 저장
        initializeAutoSaveToRoomDB()
    }

    /**
     * WebSocket 메시지 이벤트를 Room DB에 자동으로 저장하는 SSOT 패턴 구현
     */
    private fun initializeAutoSaveToRoomDB() {
        Log.i(TAG, "SSOT 패턴 초기화: WebSocket → Room DB 자동 저장 시작")

        // 새 메시지 이벤트 → Room DB 저장 + Paging3 새로고침
        webSocketEventFlow.domainEvents
            .filterIsInstance<WebSocketDomainEvent.MessageReceived>()
            .onEach { event ->
                try {
                    // 입장하지 않은 방의 메시지는 저장하지 않음
                    val roomId = event.roomId
                    if (roomId.isNullOrBlank()) {
                        Log.e(TAG, "roomId 가 비어있어 메시지를 저장하지 않음: ${event.messageId}")
                        return@onEach
                    }

                    if (!isRoomJoined(roomId)) {
                        Log.d(
                            TAG,
                            "입장하지 않은 방의 메시지 스킵: roomId=$roomId, messageId=${event.messageId}"
                        )
                        return@onEach
                    }

                    // 중복 저장 방지: 이미 존재하는 메시지인지 확인
                    val existingMessage = messageRepository.findById(DocumentId(event.messageId))

                    if (existingMessage is CustomResult.Success) {
                        Log.d(TAG, "메시지 이미 존재함, 저장 스킵: ${event.messageId}")
                        return@onEach
                    }
                    
                    val message = convertWebSocketEventToDomainMessage(event)
                    val result = messageRepository.save(message)

                    when (result) {
                        is CustomResult.Success -> {
                            Log.d(TAG, "메시지 자동 저장 성공: ${event.messageId}")

                            // Paging3 새로고침 이벤트 발송 (해당 채널만)
                            _messageRefreshEvents.emit(roomId)
                            Log.d(TAG, "Paging3 새로고침 이벤트 발송: $roomId")
                        }

                        is CustomResult.Failure -> {
                            Log.e(TAG, "메시지 자동 저장 실패: ${event.messageId}", result.error)
                        }

                        else -> {
                            Log.d(TAG, "메시지 자동 저장 상태: ${event.messageId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 자동 저장 중 예외: ${event.messageId}", e)
                }
            }
            .launchIn(serviceScope)

        // 메시지 수정 이벤트 → Room DB 업데이트
        webSocketEventFlow.domainEvents
            .filterIsInstance<WebSocketDomainEvent.MessageEdited>()
            .onEach { event ->
                try {
                    // Room DB에서 메시지 업데이트 (update 메서드가 있다고 가정)
                    Log.d(TAG, "메시지 수정 이벤트 처리: ${event.messageId}")
                    // TODO: Repository에 update 메서드 구현 후 활성화
                    // messageRepository.updatePayload(DocumentId(event.messageId), MessagePayload.forText(event.newContent))
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 수정 자동 저장 중 예외: ${event.messageId}", e)
                }
            }
            .launchIn(serviceScope)

        // 메시지 삭제 이벤트 → Room DB 업데이트
        webSocketEventFlow.domainEvents
            .filterIsInstance<WebSocketDomainEvent.MessageDeleted>()
            .onEach { event ->
                try {
                    Log.d(TAG, "메시지 삭제 이벤트 처리: ${event.messageId}")
                    val existingMessage =
                        messageRepository.findById(DocumentId(event.messageId))
                    if (existingMessage is CustomResult.Success) {
                        existingMessage.data.delete()
                        messageRepository.save(existingMessage.data)
                        Log.d(TAG, "메시지 삭제 처리 완료: ${event.messageId}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 삭제 자동 저장 중 예외: ${event.messageId}", e)
                }
            }
            .launchIn(serviceScope)

        // 3️⃣ NEW: ACK 이벤트 → OutBox 상태 업데이트
        webSocketEventFlow.domainEvents
            .filterIsInstance<WebSocketDomainEvent.MessageAck>()
            .onEach { event ->
                try {
                    Log.d(TAG, "메시지 ACK 이벤트 처리: ${event.messageId}, ackType: ${event.ackType}")

                    when (event.ackType) {
                        WebSocketMessage.TYPE_MESSAGE_ACK -> {
                            // OutBox에서 메시지 상태를 ACKED로 업데이트
                            val result = messageRepository.handleMessageAck(event.messageId)

                            when (result) {
                                is CustomResult.Success -> {
                                    Log.d(
                                        TAG,
                                        "메시지 ACK 처리 완료 (OutBox DISPATCHED): ${event.messageId}"
                                    )

                                    // Paging3 새로고침 이벤트 발송 (해당 채널만)
                                    event.roomId?.let { channelId ->
                                        _messageRefreshEvents.emit(channelId)
                                        Log.d(TAG, "Paging3 새로고침 이벤트 발송: $channelId")
                                    }
                                }

                                is CustomResult.Failure -> {
                                    Log.e(TAG, "메시지 ACK 처리 실패: ${event.messageId}", result.error)
                                }

                                else -> {
                                    Log.w(TAG, "메시지 ACK 처리 결과 알 수 없음: ${event.messageId}")
                                }
                            }
                        }

                        WebSocketMessage.TYPE_EDIT_MESSAGE_ACK -> {
                            Log.d(TAG, "메시지 수정 ACK 처리 완료: ${event.messageId}")
                        }

                        WebSocketMessage.TYPE_DELETE_MESSAGE_ACK -> {
                            Log.d(TAG, "메시지 삭제 ACK 처리 완료: ${event.messageId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 ACK 처리 중 예외: ${event.messageId}", e)
                }
            }
            .launchIn(serviceScope)

        // 4️⃣ NEW: FAILED 이벤트 → Room DB 메시지 상태 업데이트
        webSocketEventFlow.domainEvents
            .filterIsInstance<WebSocketDomainEvent.MessageFailed>()
            .onEach { event ->
                try {
                    Log.e(
                        TAG,
                        "메시지 FAILED 이벤트 처리: ${event.messageId}, error: ${event.errorMessage}"
                    )

                    when (event.failureType) {
                        WebSocketMessage.TYPE_MESSAGE_FAILED -> {
                            // 메시지 전송 실패 - OutBox에서 FAILED로 업데이트
                            val result = messageRepository.handleMessageFailure(event.messageId)
                            when (result) {
                                is CustomResult.Success -> {
                                    Log.e(TAG, "메시지 전송 실패 처리 완료 (OutBox): ${event.messageId}")
                                }

                                is CustomResult.Failure -> {
                                    Log.e(TAG, "메시지 실패 처리 실패: ${event.messageId}", result.error)
                                }

                                else -> {
                                    Log.w(TAG, "메시지 실패 처리 결과 알 수 없음: ${event.messageId}")
                                }
                            }
                        }

                        WebSocketMessage.TYPE_EDIT_MESSAGE_FAILED -> {
                            Log.e(TAG, "메시지 수정 실패 처리 완료: ${event.messageId}")
                        }

                        WebSocketMessage.TYPE_DELETE_MESSAGE_FAILED -> {
                            Log.e(TAG, "메시지 삭제 실패 처리 완료: ${event.messageId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 FAILED 처리 중 예외: ${event.messageId}", e)
                }
            }
            .launchIn(serviceScope)
    }

    /**
     * WebSocket 도메인 이벤트를 도메인 메시지 모델로 변환
     */
    private fun convertWebSocketEventToDomainMessage(event: WebSocketDomainEvent.MessageReceived): Message {
        if (event.roomId == null) throw Exception("roomId is null")
        return Message.fromDataSource(
            id = DocumentId(event.messageId),
            senderId = UserId(event.senderId),
            messageType = MessageType.TEXT,
            payload = MessagePayload.forText(event.content),
            replyToMessageId = event.replyToMessageId?.let { DocumentId(it) },
            createdAt = Instant.parse(event.timestamp),
            updatedAt = Instant.parse(event.timestamp),
            isDeleted = MessageIsDeleted.FALSE,
            mentions = emptyList(), // TODO: WebSocketDomainEvent에 mentions 추가 후 활성화
            channelId = ChannelId(event.roomId)
        )
    }

    // ================================
    // 이벤트 스트림 제공
    // ================================

    /**
     * 모든 WebSocket 도메인 이벤트 스트림
     */
    fun getAllEvents(): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.domainEvents
    }

    /**
     * 특정 방의 이벤트만 필터링된 스트림
     */
    fun getEventsForRoom(roomId: String): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.getEventsForRoom(roomId)
    }

    /**
     * 메시지 관련 이벤트만 필터링된 스트림
     */
    fun getMessageEvents(): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.getMessageEvents()
    }

    /**
     * 특정 방의 메시지 이벤트만 필터링된 스트림
     */
    fun getMessageEventsForRoom(roomId: String): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.getMessageEventsForRoom(roomId)
    }

    /**
     * 연결 상태 이벤트 스트림
     */
    fun getConnectionEvents(): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.getConnectionEvents()
    }

    /**
     * ACK/실패 이벤트 스트림
     */
    fun getAckEvents(): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.getAckEvents()
    }

    /**
     * 특정 메시지의 ACK/실패 이벤트 스트림
     */
    fun getAckEventsForMessage(messageId: String): Flow<WebSocketDomainEvent> {
        return webSocketEventFlow.getAckEventsForMessage(messageId)
    }

    // ================================
    // 연결 관리
    // ================================

    /**
     * WebSocket 연결 (GlobalWebSocketService 위임)
     */
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        Log.d(TAG, "WebSocket 연결 시도 (GlobalWebSocketService 사용)")

        return try {
            globalWebSocketService.configure(serverUrl)

            if (connectionState.value !is WebSocketConnectionState.Connected) {
                globalWebSocketService.forceReconnect()
            }

            Log.d(TAG, "GlobalWebSocketService 연결 위임 완료")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "GlobalWebSocketService 연결 위임 실패: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * UserSession을 사용한 WebSocket 연결
     */
    suspend fun connectWithSession(serverUrl: String, userSession: UserSession): Result<Unit> {
        if (userSession.idToken == null) {
            val error = Exception("Invalid or expired token in user session")
            Log.e(TAG, "WebSocket 연결 실패: 유효하지 않은 토큰")
            return Result.failure(error)
        }

        val authToken = userSession.idToken!!.value
        Log.i(TAG, "UserSession으로 WebSocket 연결 시도")

        return connect(serverUrl, authToken)
    }

    /**
     * WebSocket 인증 대기
     */
    suspend fun waitForAuthentication(userId: UserId, timeoutMs: Long = 15000): Result<Unit> {
        Log.i(TAG, "WebSocket 인증 확인 대기 (타임아웃: ${timeoutMs}ms)")

        // 이미 인증된 경우
        if (webSocketManager.isAuthenticated.value) {
            Log.i(TAG, "이미 인증된 상태")
            return Result.success(Unit)
        }

        return try {
            val authResult = withTimeoutOrNull(timeoutMs) {
                webSocketManager.incomingMessages
                    .filter { message ->
                        message.type == WebSocketMessage.TYPE_AUTH_SUCCESS ||
                                message.type == WebSocketMessage.TYPE_ERROR
                    }
                    .first()
            }

            when {
                authResult == null -> {
                    Log.e(TAG, "WebSocket 인증 시간 초과 (${timeoutMs}ms)")
                    Result.failure(Exception("Authentication timeout - no AUTH_SUCCESS received within ${timeoutMs}ms"))
                }

                authResult.type == WebSocketMessage.TYPE_AUTH_SUCCESS -> {
                    Log.i(TAG, "WebSocket 인증 성공")
                    Result.success(Unit)
                }

                authResult.type == WebSocketMessage.TYPE_ERROR -> {
                    Log.e(TAG, "WebSocket 인증 실패: ${authResult.content}")
                    Result.failure(Exception("Authentication failed: ${authResult.content}"))
                }

                else -> {
                    Log.e(TAG, "WebSocket 인증 처리 중 예상치 못한 메시지 타입: ${authResult.type}")
                    Result.failure(Exception("Unexpected message type during authentication: ${authResult.type}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket 인증 대기 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * WebSocket 연결 해제 (개별 기능용 - GlobalWebSocketService는 유지)
     */
    suspend fun disconnect() {
        Log.i(TAG, "개별 기능 WebSocket 사용 종료 - GlobalWebSocketService는 계속 활성 상태")
        // 개별 기능은 GlobalWebSocketService를 해제하지 않음
        // 단지 자신의 방 상태만 정리
        clearAllRooms()
    }

    // ================================
    // 방 관리
    // ================================

    /**
     * 방 입장
     */
    suspend fun joinRoom(roomId: String, userId: UserId? = null): Result<Unit> {
        Log.i(TAG, "채팅방 입장 시도: $roomId")

        // 중복 입장 방지
        synchronized(joiningRooms) {
            if (joiningRooms.contains(roomId)) {
                Log.i(TAG, "이미 채팅방 입장 중: $roomId")
                return Result.success(Unit)
            }
            if (joinedRooms.contains(roomId)) {
                Log.i(TAG, "이미 채팅방에 입장함: $roomId")
                return Result.success(Unit)
            }
            joiningRooms.add(roomId)
        }

        try {
            // GlobalWebSocketService에서 이미 연결을 관리하므로 연결 확인 불필요
            // 연결이 안 되어 있으면 방 입장 메시지 전송이 실패할 것이므로 자연스럽게 처리됨

            // 방 입장 메시지 전송
            val sendResult = if (userId != null) {
                val joinMessage = WebSocketMessage(
                    type = WebSocketMessage.TYPE_JOIN_ROOM,
                    roomId = roomId,
                    senderId = userId.value,
                    timestamp = Instant.now().epochSecond.toDouble()
                )
                webSocketManager.sendMessage(joinMessage)
            } else {
                webSocketManager.joinRoom(roomId)
            }

            return if (sendResult.isSuccess) {
                // 방 입장 확인 대기
                try {
                    val confirmationResult = withTimeoutOrNull(10000) {
                        webSocketManager.incomingMessages
                            .filter { message ->
                                (message.type == WebSocketEventTypes.JOINED_ROOM && message.roomId == roomId) ||
                                        (message.type == WebSocketMessage.TYPE_ERROR &&
                                                (message.content?.contains("Room") == true || message.roomId == roomId))
                            }
                            .first()
                    }

                    when {
                        confirmationResult?.type == WebSocketEventTypes.JOINED_ROOM -> {
                            synchronized(joiningRooms) {
                                joiningRooms.remove(roomId)
                                joinedRooms.add(roomId)
                            }
                            Log.i(TAG, "채팅방 입장 확인됨: $roomId")
                            Result.success(Unit)
                        }

                        confirmationResult?.type == WebSocketMessage.TYPE_ERROR -> {
                            synchronized(joiningRooms) { joiningRooms.remove(roomId) }
                            Log.e(TAG, "채팅방 입장 실패: ${confirmationResult.content}")
                            Result.failure(Exception("Room join failed: ${confirmationResult.content}"))
                        }

                        else -> {
                            // 타임아웃이지만 메시지는 전송됨 - 낙관적으로 성공 처리
                            synchronized(joiningRooms) {
                                joiningRooms.remove(roomId)
                                joinedRooms.add(roomId)
                            }
                            Log.w(TAG, "채팅방 입장 확인 시간 초과 (낙관적 성공): $roomId")
                            Result.success(Unit)
                        }
                    }
                } catch (e: Exception) {
                    synchronized(joiningRooms) { joiningRooms.remove(roomId) }
                    Log.e(TAG, "채팅방 입장 확인 중 오류: ${e.message}", e)
                    // 메시지는 전송되었으므로 성공으로 처리
                    Result.success(Unit)
                }
            } else {
                synchronized(joiningRooms) { joiningRooms.remove(roomId) }
                Log.e(TAG, "채팅방 입장 메시지 전송 실패: ${sendResult.exceptionOrNull()?.message}")
                sendResult
            }
        } catch (e: Exception) {
            synchronized(joiningRooms) { joiningRooms.remove(roomId) }
            Log.e(TAG, "채팅방 입장 중 예외 발생: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * 방 퇴장
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        Log.i(TAG, "채팅방 퇴장 시도: $roomId")

        // 방 상태 정리
        synchronized(joiningRooms) {
            joiningRooms.remove(roomId)
            joinedRooms.remove(roomId)
        }

        return webSocketManager.leaveRoom(roomId).also { result ->
            if (result.isSuccess) {
                Log.i(TAG, "채팅방 퇴장 성공: $roomId")
            } else {
                Log.e(TAG, "채팅방 퇴장 실패: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * 방 입장 상태 확인
     */
    fun isRoomJoined(roomId: String): Boolean {
        synchronized(joiningRooms) {
            return joinedRooms.contains(roomId)
        }
    }

    /**
     * 방 입장 진행 중 상태 확인
     */
    fun isRoomJoining(roomId: String): Boolean {
        synchronized(joiningRooms) {
            return joiningRooms.contains(roomId)
        }
    }

    /**
     * 모든 방에서 퇴장 (연결 해제 시 호출)
     */
    fun clearAllRooms() {
        synchronized(joiningRooms) {
            joiningRooms.clear()
            joinedRooms.clear()
        }
    }

    // ================================
    // 메시지 전송
    // ================================

    /**
     * 메시지 전송 - Room DB에 먼저 저장 후 WebSocket 전송
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
        Log.i(TAG, "메시지 전송 시도: messageId=${messageId.value}, roomId=$roomId")

        try {
            // 1️⃣ Room DB 저장은 ViewModel에서 이미 처리되므로 여기서는 스킵
            // ViewModel이 SENDING 상태로 이미 저장했음
            Log.d(TAG, "Room DB 저장은 ViewModel에서 처리됨, WebSocket 전송만 진행: ${messageId.value}")

            // 2️⃣ WebSocket으로 메시지 전송
            val message = WebSocketMessage(
                type = WebSocketMessage.TYPE_MESSAGE,
                roomId = roomId,
                senderId = senderId.value,
                content = content, // Backward compatibility
                messageType = MessageType.TEXT.name,
                payload = mapOf("content" to content), // New payload format
                messageId = messageId.value,
                replyToMessageId = replyToMessageId?.value,
                timestamp = Instant.now().epochSecond.toDouble(),
                projectId = projectId,
                channelType = channelType
            )

            return webSocketManager.sendMessage(message).also { result ->
                val status =
                    if (result.isSuccess) OperationStatus.SUCCESS else OperationStatus.FAILED
                Log.i(TAG, "WebSocket 메시지 SEND $status: messageId=${messageId.value}")

                if (result.isFailure) {
                    Log.e(TAG, "메시지 전송 실패: ${result.exceptionOrNull()?.message}")
                    // TODO: 실패 시 Room DB의 메시지 상태를 FAILED로 업데이트
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 과정 중 예외: ${messageId.value}", e)
            return Result.failure(e)
        }
    }

    /**
     * 메시지 수정
     */
    suspend fun editMessage(
        roomId: String,
        messageId: DocumentId,
        newContent: String,
        projectId: String? = null,
        channelType: String? = null
    ): Result<Unit> {
        Log.i(TAG, "메시지 수정 시도: messageId=${messageId.value}, roomId=$roomId")

        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_EDIT_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            content = newContent, // Backward compatibility
            messageType = MessageType.TEXT.name,
            payload = mapOf("content" to newContent), // New payload format
            timestamp = Instant.now().epochSecond.toDouble(),
            projectId = projectId,
            channelType = channelType
        )

        return webSocketManager.sendMessage(message).also { result ->
            val status = if (result.isSuccess) OperationStatus.SUCCESS else OperationStatus.FAILED
            Log.i(TAG, "WebSocket 메시지 EDIT $status: messageId=${messageId.value}")

            if (result.isFailure) {
                Log.e(TAG, "메시지 수정 실패: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * 메시지 삭제
     */
    suspend fun deleteMessage(
        roomId: String,
        messageId: DocumentId,
        projectId: String? = null,
        channelType: String? = null
    ): Result<Unit> {
        Log.i(TAG, "메시지 삭제 시도: messageId=${messageId.value}, roomId=$roomId")

        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_DELETE_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            messageType = MessageType.TEXT.name,
            payload = emptyMap(), // Empty payload for delete operation
            timestamp = Instant.now().epochSecond.toDouble(),
            projectId = projectId,
            channelType = channelType
        )

        return webSocketManager.sendMessage(message).also { result ->
            val status = if (result.isSuccess) OperationStatus.SUCCESS else OperationStatus.FAILED
            Log.i(TAG, "WebSocket 메시지 DELETE $status: messageId=${messageId.value}")

            if (result.isFailure) {
                Log.e(TAG, "메시지 삭제 실패: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    companion object {
        private const val TAG = "WebSocketMessageService"
    }
}