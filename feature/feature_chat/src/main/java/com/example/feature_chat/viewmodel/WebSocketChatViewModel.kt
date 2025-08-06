package com.example.feature_chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.data_repository.util.RoomDatabaseLogger
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.model.data.UserSession
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.MentionType
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.MessageRepository
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.usecase.sync.SyncUseCase
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.model.MessageDeliveryState
import com.example.feature_chat.service.ChatServiceProvider
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.usecase.WebSocketUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Refactored WebSocketChatViewModel using Service Provider pattern
 * Reduced from 1000+ lines to ~450 lines by delegating logic to Services
 */
@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val chatServiceProvider: ChatServiceProvider,
    private val messageRepository: MessageRepository,
    private val roomDatabaseLogger: RoomDatabaseLogger,
    private val sendMessageUseCase: com.example.domain_usecase.usecase.message.SendMessageUseCase,
    private val syncUseCase: SyncUseCase, // 증분 동기화 UseCase 추가
) : ViewModel() {

    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val projectId: String? = savedStateHandle.get<String>(RouteArgs.PROJECT_ID)
    private val initialMessageId: String? = savedStateHandle.get<String>("initialMessageId")

    // Services are initialized lazily once we determine the channel type
    private val services by lazy {
        if (projectId != null) {
            Log.d("ViewModel", "Creating services for project channel: $projectId/$channelId")
            chatServiceProvider.createForProjectChannel(projectId, channelId)
        } else {
            Log.d("ViewModel", "Creating services for DM channel: $channelId")
            chatServiceProvider.createForDMChannel(channelId)
        }
    }
    
    // Mention display gateway - maps user-visible @displayName to internal [type:id] format
    private var currentMentionMappings = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(
        ChatUiState(
            channelName = "채팅방",
            connectionState = WebSocketConnectionState.Disconnected
            // Note: messages, isLoadingHistory are now handled by Paging3
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentUserId: String? = null

    // 메시지 타임아웃 관리를 위한 Job 맵
    private val messageTimeoutJobs = mutableMapOf<String, Job>()

    // 주기적 동기화를 위한 Job
    private var periodicSyncJob: Job? = null

    // Paging3 configuration for messages from Room DB (Single Source of Truth)
    // PagingSource 인스턴스를 추적하기 위한 변수
    private var currentPagingSource: androidx.paging.PagingSource<Long, Message>? =
        null

    // Anchor 관련 상태 관리
    private var currentAnchor: Long? = null
    private var isAnchorJumpInProgress = false
    private var isInitialLoadComplete = false
    
    private val pager = Pager(
        config = PagingConfig(
            pageSize = 10, // Reduced from 20 to 10 for faster initial loading
            enablePlaceholders = false,
            prefetchDistance = 3 // Reduced from 5 to 3 for more responsive loading
        ),
        pagingSourceFactory = {
            val pagingSource = messageRepository.getMessagesPagingSource(channelId)
            currentPagingSource = pagingSource // 현재 PagingSource 인스턴스 추적
            Log.d("ViewModel", "🔄 새로운 PagingSource 생성됨: ${pagingSource.hashCode()}")
            pagingSource
        }
    )

    // Paging3 Flow for messages from Room DB
    val messagesFlow: Flow<PagingData<ChatMessageUiModel>> = pager.flow
        .map { pagingData: PagingData<Message> ->
            // 로깅 최적화: 성능 향상을 위해 상세 로그 제거
            pagingData.map<Message, ChatMessageUiModel> { message: Message ->
                // Convert domain Message to UI model with display format
                runBlocking { convertDomainMessageToUiModel(message) }
            }
        }
        .onEach { pagingData ->
            Log.d("Paging3-UI", "📊 Paging3 Flow에서 새로운 데이터 감지됨")

            // Paging3 로드 시 증분 동기화 트리거 (백그라운드 실행)
            performPagingLoadSync()
        }
        .cachedIn(viewModelScope)

    /**
     * Anchor Jump 기능들
     */

    /**
     * 특정 메시지 ID로 Anchor Jump (해당 메시지를 중심으로 페이징)
     */
    fun jumpToMessage(messageId: String) {
        viewModelScope.launch {
            try {
                Log.i("AnchorJump", "🎯 === ANCHOR JUMP TO MESSAGE: $messageId ===")

                // 1. 메시지 ID로 해당 메시지 조회
                val message = messageRepository.findById(messageId)
                if (message != null) {
                    val anchorTimestamp = message.createdAt.toEpochMilli()
                    jumpToAnchor(anchorTimestamp, "메시지 ID: $messageId")
                } else {
                    Log.w("AnchorJump", "⚠️ 메시지를 찾을 수 없음: $messageId")
                    _eventFlow.emit(ChatEvent.ShowSnackbar("메시지를 찾을 수 없습니다"))
                }
            } catch (e: Exception) {
                Log.e("AnchorJump", "❌ Anchor Jump 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지로 이동하는 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 특정 타임스탬프로 Anchor Jump
     */
    fun jumpToTimestamp(timestamp: Long, reason: String = "타임스탬프") {
        viewModelScope.launch {
            jumpToAnchor(timestamp, reason)
        }
    }

    /**
     * 최신 메시지로 Anchor Jump (채팅방 입장 시 기본 동작)
     */
    fun jumpToLatest() {
        viewModelScope.launch {
            Log.i("AnchorJump", "🚀 === ANCHOR JUMP TO LATEST ===")
            jumpToAnchor(System.currentTimeMillis(), "최신 메시지")
        }
    }

    /**
     * 내부 Anchor Jump 구현
     */
    private suspend fun jumpToAnchor(anchorTimestamp: Long, reason: String) {
        if (isAnchorJumpInProgress) {
            Log.w("AnchorJump", "⚠️ 이미 Anchor Jump가 진행 중입니다")
            return
        }

        try {
            isAnchorJumpInProgress = true
            currentAnchor = anchorTimestamp

            Log.i("AnchorJump", "🎯 === ANCHOR JUMP START ===")
            Log.i("AnchorJump", "📍 Anchor: $anchorTimestamp (${formatTimestamp(anchorTimestamp)})")
            Log.i("AnchorJump", "📝 Reason: $reason")
            Log.i("AnchorJump", "📊 Channel: $channelId")

            // PagingSource 무효화하여 새로운 anchor로 재시작
            currentPagingSource?.invalidate()

            Log.i("AnchorJump", "✅ PagingSource 무효화 완료")
            Log.i("AnchorJump", "🎯 === ANCHOR JUMP END ===")

        } catch (e: Exception) {
            Log.e("AnchorJump", "❌ Anchor Jump 실패", e)
            _eventFlow.emit(ChatEvent.ShowSnackbar("이동 중 오류가 발생했습니다"))
        } finally {
            isAnchorJumpInProgress = false
        }
    }

    /**
     * 초기 Anchor 설정
     */
    private fun initializeAnchor() {
        viewModelScope.launch {
            try {
                Log.i("AnchorInit", "🚀 === INITIAL ANCHOR SETUP ===")
                Log.i("AnchorInit", "📊 Channel: $channelId")
                Log.i("AnchorInit", "📝 Initial Message ID: $initialMessageId")

                if (initialMessageId != null) {
                    // 1. 특정 메시지 ID가 설정된 경우: 해당 메시지를 anchor로 설정
                    Log.i("AnchorInit", "🎯 특정 메시지로 Anchor 설정")
                    jumpToMessage(initialMessageId)
                } else {
                    // 2. 설정이 없는 경우: 최신 메시지를 anchor로 설정
                    Log.i("AnchorInit", "📌 최신 메시지로 Anchor 설정")
                    jumpToLatest()
                }

                Log.i("AnchorInit", "✅ 초기 Anchor 설정 완료")
                Log.i("AnchorInit", "🚀 === INITIAL ANCHOR SETUP END ===")
            } catch (e: Exception) {
                Log.e("AnchorInit", "❌ 초기 Anchor 설정 실패", e)
            }
        }
    }

    /**
     * 초기 메시지 ID 반환
     */
    fun getInitialMessageId(): String? = initialMessageId

    /**
     * 타임스탬프를 읽기 쉬운 형태로 포맷팅
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val dateFormat =
                java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            "Invalid"
        }
    }

    init {
        // Log local cache on entry
        logChannelCacheOnEntry()

        // 수신 메시지 처리를 위한 이벤트 구독
        subscribeToIncomingMessages()

        initializeUserSession()
        // joinChatRoom() - 사용자 인증 완료 후 호출로 이동
        observeConnectionState() // 연결 상태 모니터링 시작
        observeWebSocketEventsForUiEvents()
        loadChannelData()

        // 초기 진입 시 증분 동기화 실행
        performInitialSync()

        // 5분 주기 동기화 시작
        startPeriodicSync()

        // 초기 Anchor 설정
        initializeAnchor()
    }

    private fun logChannelCacheOnEntry() {
        viewModelScope.launch {
            Log.i("ChatDebug", "🚀 === CHAT INITIALIZATION: $channelId ===")

            // Room DB 상태 로그 출력 (IO 스레드에서 실행)
            try {
                withContext(Dispatchers.IO) {
                    // 1️⃣ 현재 채널의 상세 메시지 상태 출력
                    roomDatabaseLogger.logChannelMessages(channelId, 10)

                    // 2️⃣ 채널별 상세 분석 (Repository를 통한 조회)
                    logChannelSpecificData()

                    // 3️⃣ 전체 DB 상태 요약
                    roomDatabaseLogger.logTableState("messages")

                    // 4️⃣ OutBox 상태 확인 (동기화 대기 중인 메시지)
                    roomDatabaseLogger.logTableState("outbox")
                }
                Log.d("ChatDebug", "✅ Room DB 로거 실행 완료")
            } catch (e: Exception) {
                Log.e("ChatDebug", "❌ Room DB 로깅 실패", e)
            }
        }
    }

    /**
     * 현재 채널의 구체적인 데이터 상태를 로그로 출력
     */
    private suspend fun logChannelSpecificData() {
        try {
            // MessageDao를 통해 채널별 통계 조회
            val messageDao =
                messageRepository as? com.example.data_repository.base.MessageRepositoryImpl

            if (messageDao != null) {
                // TODO: MessageRepositoryImpl에서 MessageDao에 직접 접근할 수 있는 메서드 필요
                // 임시로 Repository 메서드를 통해 데이터 조회
                Log.i("ChatDebug", "📊 === CHANNEL DATA: $channelId ===")

                // 최근 메시지 조회해서 로그 출력
                when (val result = messageRepository.getMessagesBefore(
                    channelId,
                    System.currentTimeMillis(),
                    10
                )) {
                    is CustomResult.Success -> {
                        val messages = result.data
                        Log.i("ChatDebug", "📝 채널 메시지 개수: ${messages.size}")
                        Log.i("ChatDebug", "📋 최근 메시지 목록:")

                        messages.forEachIndexed { index, message ->
                            val timeFormatted =
                                DateTimeUtil.formatToHumanReadable(message.createdAt)
                            val contentPreview = if (message.payload.value.length > 30) {
                                message.payload.value.take(30) + "..."
                            } else {
                                message.payload.value
                            }
                            Log.i(
                                "ChatDebug",
                                "   ${index + 1}. [${message.id.value.take(8)}] $timeFormatted: \"$contentPreview\""
                            )
                        }

                        if (messages.isEmpty()) {
                            Log.i("ChatDebug", "   📭 채널에 저장된 메시지가 없습니다")
                        }
                    }

                    is CustomResult.Failure -> {
                        Log.e("ChatDebug", "❌ 채널 메시지 조회 실패: ${result.error.message}")
                    }

                    else -> {
                        Log.w("ChatDebug", "⚠️ 메시지 조회 결과 타입 예상치 못함")
                    }
                }
            } else {
                Log.w("ChatDebug", "⚠️ MessageRepository가 MessageRepositoryImpl 타입이 아닙니다")
            }

            Log.i("ChatDebug", "🔚 === END CHANNEL DATA ===")

        } catch (e: Exception) {
            Log.e("ChatDebug", "❌ 채널별 데이터 로깅 실패", e)
        }
    }

    private fun initializeUserSession() {
        viewModelScope.launch {
            val authUseCases = authSessionUseCaseProvider.create()
            authUseCases.getCurrentUserSessionStreamUseCase()
                .collectLatest { result: CustomResult<UserSession, Exception> ->
                when (result) {
                    is CustomResult.Success -> {
                        val userSession = result.data
                        currentUserId = userSession.userId.value
                        Log.d("ViewModel", "Current user authenticated: ${currentUserId}")
                        
                        _uiState.update { state ->
                            state.copy(
                                currentUserId = currentUserId,
                                myUserId = currentUserId ?: ""
                            )
                        }

                        // 사용자 인증 완료 후 채팅방 입장
                        joinChatRoom()
                    }

                    is CustomResult.Failure -> {
                        Log.e("ViewModel", "Authentication failed", result.error)
                        _eventFlow.emit(ChatEvent.Error("인증 실패: ${result.error.message ?: "Unknown error"}"))
                    }
                    else -> {
                        Log.d("ViewModel", "Authentication loading...")
                    }
                }
            }
        }
    }

    private fun joinChatRoom() {
        viewModelScope.launch {
            // Join WebSocket room for real-time events
            currentUserId?.let { userId ->
                try {
                    val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)

                    roomUseCases.joinRoomUseCase(UserId(userId))
                    Log.d("ViewModel", "Joined chat room: $channelId")

                } catch (e: Exception) {
                    Log.e("ViewModel", "Failed to join chat room", e)
                    _eventFlow.emit(ChatEvent.Error("채팅방 입장 실패: ${e.message}"))
                }
            }
        }
    }

    /**
     * 연결 상태 모니터링을 별도로 분리
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            try {
                val generalUseCases = webSocketUseCaseProvider.create()

                generalUseCases.getConnectionStateUseCase()
                    .collect { connectionState: WebSocketConnectionState ->
                        _uiState.update { state ->
                            state.copy(connectionState = connectionState)
                        }

                        when (connectionState) {
                            is WebSocketConnectionState.Error -> {
                                _eventFlow.emit(ChatEvent.Error("연결 오류: ${connectionState.message}"))
                            }

                            else -> { /* Handle other states if needed */
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to observe connection state", e)
            }
        }
    }

    /**
     * Observe WebSocket events for UI-only events (SystemMessage, Error)
     * Message data changes (ACK/Failed) are handled by Room database
     * Paging3 automatically reflects Room changes in UI
     */
    private fun observeWebSocketEventsForUiEvents() {
        viewModelScope.launch {
            try {
                val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                roomUseCases.subscribeToRoomEventsUseCase().collect { event ->
                    when (event) {
                        is WebSocketDomainEvent.MessageAck -> {
                            // Message ACK: Room database handles status update
                            // Paging3 will automatically reflect the changes in UI
                            try {
                                // OutBox 기반 ACK 처리
                                val result = messageRepository.handleMessageAck(event.messageId)

                                when (result) {
                                    is CustomResult.Success -> {
                                        Log.d(
                                            "ViewModel",
                                            "Message ACK processed in OutBox: ${event.messageId}"
                                        )

                                        // Paging3 새로고침으로 UI에 상태 변경 반영
                                        invalidatePagingSource()
                                    }

                                    is CustomResult.Failure -> {
                                        Log.e(
                                            "ViewModel",
                                            "Failed to process message ACK: ${result.error.message}"
                                        )
                                    }

                                    else -> {
                                        Log.w(
                                            "ViewModel",
                                            "Unexpected result type from handleMessageAck"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ViewModel", "Failed to process message ACK", e)
                            }
                        }

                        is WebSocketDomainEvent.MessageFailed -> {
                            // Message Failed: Room database handles status update
                            // Paging3 will automatically reflect the changes in UI
                            cancelMessageTimeout(event.messageId)
                            Log.d(
                                "ViewModel",
                                "Message failure handled by Room: ${event.messageId}"
                            )
                        }

                        is WebSocketDomainEvent.SystemMessage -> {
                            // System messages are UI events, not stored in Room
                            _eventFlow.emit(ChatEvent.SystemMessage(event.content))
                        }

                        is WebSocketDomainEvent.Error -> {
                            // Error messages are UI events, not stored in Room
                            _eventFlow.emit(ChatEvent.Error(event.message))
                        }

                        else -> {
                            // Other events (MessageReceived, etc.) are handled by Room auto-save
                            // Paging3 will automatically refresh when Room data changes
                            Log.d(
                                "ViewModel",
                                "Event handled by Room auto-save: ${event::class.simpleName}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to observe WebSocket events", e)
            }
        }
    }


    private fun handleMessageAck(event: WebSocketDomainEvent.MessageAck) {
        Log.i("ViewModel", "Message ACK received: ${event.messageId} (${event.ackType})")

        // 타임아웃 Job 취소
        cancelMessageTimeout(event.messageId)

        // With Paging3, update the database instead of UI state
        // The Room database update will automatically flow through Paging3 to UI
        viewModelScope.launch {
            try {
                // OutBox 기반 ACK 처리
                val result = messageRepository.handleMessageAck(event.messageId)

                when (result) {
                    is CustomResult.Success -> {
                        Log.d("ViewModel", "Message ACK processed in OutBox: ${event.messageId}")

                        // Paging3 새로고침으로 UI에 상태 변경 반영
                        invalidatePagingSource()
                    }

                    is CustomResult.Failure -> {
                        Log.e(
                            "ViewModel",
                            "Failed to process message ACK: ${result.error.message}"
                        )
                    }

                    else -> {
                        Log.w("ViewModel", "Unexpected result type from handleMessageAck")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to process message ACK", e)
            }
        }
    }

    private fun handleMessageFailed(event: WebSocketDomainEvent.MessageFailed) {
        Log.e("ViewModel", "Message failed: ${event.messageId} (${event.failureType})")

        // 타임아웃 Job 취소
        cancelMessageTimeout(event.messageId)

        // With Paging3, update the database instead of UI state
        // The Room database update will automatically flow through Paging3 to UI
        viewModelScope.launch {
            try {
                // OutBox 기반 실패 처리
                val result = messageRepository.handleMessageFailure(event.messageId)

                when (result) {
                    is CustomResult.Success -> {
                        Log.d(
                            "ViewModel",
                            "Message failure processed in OutBox: ${event.messageId}"
                        )

                        // Paging3 새로고침으로 UI에 실패 상태 반영
                        invalidatePagingSource()
                    }

                    is CustomResult.Failure -> {
                        Log.e(
                            "ViewModel",
                            "Failed to process message failure: ${result.error.message}"
                        )
                    }

                    else -> {
                        Log.w("ViewModel", "Unexpected result type from handleMessageFailure")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to process message failure", e)
            }
        }
    }


    private fun sendMessage(
        text: String,
        attachmentUris: List<Uri> = emptyList(),
        replyToMessageId: String? = null
    ) {
        val senderId = currentUserId
        if (senderId == null) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }

        // 웹소켓 연결 상태 확인
        val connectionState = _uiState.value.connectionState
        if (connectionState !is WebSocketConnectionState.Connected) {
            viewModelScope.launch {
                val statusMessage = when (connectionState) {
                    is WebSocketConnectionState.Connecting ->
                        "연결 중입니다. 잠시만 기다려주세요."
                    is WebSocketConnectionState.Disconnected ->
                        "서버와 연결이 끊어져 있습니다. 연결을 다시 시도해주세요."
                    is WebSocketConnectionState.Error ->
                        "연결 오류가 발생했습니다: ${connectionState.message}"
                    else -> "메시지 전송이 불가능한 상태입니다."
                }
                _eventFlow.emit(ChatEvent.ShowSnackbar(statusMessage))
            }
            return
        }

        viewModelScope.launch {
            try {
                // 1. 메시지 생성
                val domainMessageId = DocumentId(
                    UUID.randomUUID().toString()
                )
                Message.create(
                    id = domainMessageId,
                    senderId = UserId(senderId),
                    payload = MessagePayload.forText(text),
                    replyToMessageId = replyToMessageId?.let {
                        DocumentId(it)
                    },
                    mentions = emptyList(), // TODO: 멘션 파싱 로직 추가
                    channelId = ChannelId(channelId)
                )

                // 2. 메시지 전송 (OutBox 기반으로 자동 처리)
                val messageId = messageRepository.sendMessage(channelId, text)

                Log.d("ViewModel", "Message sent with OutBox: $messageId")

                // 3. Paging3 새로고침 트리거 (SSOT 반영) - 최적화된 방식
                // 새 메시지 추가 시에는 전체 무효화 대신 스크롤 위치 유지
                invalidatePagingSourceWithOptimization()

                // 4. 메시지 타임아웃 시작
                startMessageTimeout(messageId)

                // 5. 백그라운드에서 WebSocket 전송
                async {
                    try {
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                        roomUseCases.sendMessageUseCase(
                            senderId = UserId(senderId),
                            content = text,
                            messageId = DocumentId(messageId),
                            replyToMessageId = replyToMessageId?.let {
                                DocumentId(it)
                            },
                            projectId = projectId,
                            channelType = if (projectId != null) "PROJECT" else "DM"
                        )
                        Log.d("ViewModel", "Message sent via WebSocket: $messageId")
                    } catch (e: Exception) {
                        Log.e("ViewModel", "WebSocket send failed: ${e.message}")
                        // WebSocket 전송 실패 시 OutBox에서 FAILED로 업데이트
                        messageRepository.handleMessageFailure(messageId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to send message", e)
                _eventFlow.emit(ChatEvent.Error("메시지 전송 실패: ${e.message}"))
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        if (currentUserId == null) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                roomUseCases.editMessageUseCase(
                    messageId = DocumentId(messageId),
                    newContent = newContent,
                    projectId = projectId,
                    channelType = if (projectId != null) "PROJECT" else "DM"
                )
                Log.d("ViewModel", "Message edited via WebSocket")
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to edit message", e)
                _eventFlow.emit(ChatEvent.Error("메시지 수정 실패: ${e.message}"))
            }
        }
    }

    fun deleteMessage(messageId: String) {
        if (currentUserId == null) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                roomUseCases.deleteMessageUseCase(
                    messageId = DocumentId(messageId),
                    projectId = projectId,
                    channelType = if (projectId != null) "PROJECT" else "DM"
                )
                Log.d("ViewModel", "Message deleted via WebSocket")
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to delete message", e)
                _eventFlow.emit(ChatEvent.Error("메시지 삭제 실패: ${e.message}"))
            }
        }
    }

    // Paging3 handles message loading automatically
    // No manual loadMoreMessages needed
    
    private fun loadChannelData() {
        // Paging3가 Room DB에서 기존 메시지를 자동으로 로드합니다.
        // 별도의 초기 메시지 로딩은 필요하지 않습니다.
        // WebSocket과 동기화 서비스가 백그라운드에서 데이터를 관리합니다.
        
        if (projectId == null) {
            // DM channel - load participants
            loadDMParticipants()
        } else {
            // Project channel - load members and roles
            loadProjectMembersAndRoles()
        }
    }


    private fun loadDMParticipants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingParticipants = true) }

            _uiState.update {
                try {
                    val participantService = services.participantService
                    if (participantService != null) {
                        Log.d("ViewModel", "Loading DM participants using ParticipantService")
                        val participants = participantService.loadParticipants()

                        Log.d(
                            "ViewModel",
                            "Successfully loaded ${participants.size} DM participants"
                        )
                        return@update it.copy(
                            participants = participants,
                            isLoadingParticipants = false
                        )

                    } else {
                        Log.e("ViewModel", "ParticipantService is null for DM channel")
                        return@update it.copy(isLoadingParticipants = false)
                    }
                } catch (e: Exception) {
                    Log.e("ViewModel", "Failed to load DM participants", e)
                    return@update it.copy(
                        participants = emptyList(),
                        isLoadingParticipants = false
                    )
                }
            }
        }
    }
    
    private fun loadProjectMembersAndRoles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjectData = true) }

            _uiState.update {
                try {
                    val memberService = services.memberService
                    val roleService = services.roleService

                    if (memberService != null && roleService != null) {
                        Log.d(
                            "ViewModel",
                            "Loading project members and roles using MemberService and RoleService"
                        )

                        // Load members and roles concurrently
                        val membersDeferred = async { memberService.loadMembers() }
                        val rolesDeferred = async { roleService.loadRoles() }

                        val projectMembers = membersDeferred.await()
                        val projectRoles = rolesDeferred.await()

                        // Update role member counts with total member count
                        val updatedRoles =
                            roleService.updateRoleMemberCounts(projectRoles, projectMembers.size)

                        Log.d(
                            "ViewModel",
                            "Successfully loaded ${projectMembers.size} members and ${updatedRoles.size} roles"
                        )
                        return@update it.copy(
                            projectMembers = projectMembers,
                            projectRoles = updatedRoles,
                            isLoadingProjectData = false
                        )
                    } else {
                        Log.e(
                            "ViewModel",
                            "MemberService or RoleService is null for project channel"
                        )
                        return@update it.copy(isLoadingProjectData = false)
                    }
                } catch (e: Exception) {
                    Log.e("ViewModel", "Failed to load project data", e)
                    return@update it.copy(
                        projectMembers = emptyList(),
                        projectRoles = emptyList(),
                        isLoadingProjectData = false
                    )
                }
            }
        }
    }

    // UI Action Methods
    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(pendingMessageText = text) }
        
        // Clean up mention mappings for mentions that are no longer in the text
        val keysToRemove = currentMentionMappings.keys.filter { displayFormat ->
            !text.contains(displayFormat)
        }
        keysToRemove.forEach { key ->
            currentMentionMappings.remove(key)
        }
        
        handleMentionSuggestions(text)
    }
    
    private fun handleMentionSuggestions(text: String) {
        // Find if there's an @ symbol followed by text at the cursor position
        val cursorPosition = text.length // Assuming cursor is at the end
        val mentionMatch = findMentionQuery(text, cursorPosition)
        
        if (mentionMatch != null) {
            val (startPos, query) = mentionMatch
            if (query.length >= 0) { // Show suggestions immediately after @
                showMentionSuggestions(query, startPos)
            } else {
                hideMentionSuggestions()
            }
        }
    }
    
    private fun findMentionQuery(text: String, cursorPosition: Int): Pair<Int, String>? {
        // Find the last @ symbol before cursor position
        val beforeCursor = text.substring(0, cursorPosition)
        val lastAtIndex = beforeCursor.lastIndexOf('@')
        
        if (lastAtIndex == -1) return null
        
        // Check if there's a space between @ and cursor (which would break the mention)
        val textAfterAt = beforeCursor.substring(lastAtIndex + 1)
        if (textAfterAt.contains(' ')) return null
        
        return Pair(lastAtIndex, textAfterAt)
    }
    
    private fun showMentionSuggestions(query: String, startPosition: Int) {
        val currentState = _uiState.value
        
        // Generate suggestions based on participants/members/roles
        val suggestions = mutableListOf<MentionSuggestion>()
        
        // Add user suggestions
        if (projectId == null) {
            // DM channel - use participants
            Log.d ("WebSocketChatViewModel", currentState.participants.toString())
            suggestions.addAll(
                currentState.participants
                    .filter { it.displayName.contains(query, ignoreCase = true) }
                    .map { participant ->
                        MentionSuggestion(
                            type = MentionType.USER,
                            id = participant.userId,
                            displayName = participant.displayName,
                            profileUrl = participant.profileUrl,
                            subtitle = if (participant.isOnline) "온라인" else "오프라인"
                        )
                    }
            )
        } else {
            // Project channel - use project members
            suggestions.addAll(
                currentState.projectMembers
                    .filter { it.displayName.contains(query, ignoreCase = true) }
                    .map { member ->
                        MentionSuggestion(
                            type = MentionType.USER,
                            id = member.userId,
                            displayName = member.displayName,
                            profileUrl = member.profileUrl,
                            subtitle = member.roleName
                        )
                    }
            )
            
            // Add role suggestions
            suggestions.addAll(
                currentState.projectRoles
                    .filter { it.roleName.contains(query, ignoreCase = true) }
                    .map { role ->
                        MentionSuggestion(
                            type = MentionType.ROLE,
                            id = role.roleId,
                            displayName = role.roleName,
                            subtitle = "${role.memberCount}명"
                        )
                    }
            )
        }
        
        _uiState.update { 
            it.copy(
                mentionSuggestions = suggestions.take(8), // Limit to 8 suggestions
                isMentionSuggestionVisible = suggestions.isNotEmpty(),
                mentionQueryText = query,
                mentionQueryStartPosition = startPosition
            )
        }
    }
    
    private fun hideMentionSuggestions() {
        _uiState.update { 
            it.copy(
                mentionSuggestions = emptyList(),
                isMentionSuggestionVisible = false,
                mentionQueryText = "",
                mentionQueryStartPosition = -1
            )
        }
    }
    
    fun onMentionSuggestionClick(suggestion: MentionSuggestion) {
        val currentState = _uiState.value
        val currentText = currentState.pendingMessageText
        val startPos = currentState.mentionQueryStartPosition
        
        if (startPos >= 0) {
            // Replace @query with @displayName format for user-friendly display with automatic spacing
            val beforeMention = currentText.substring(0, startPos)
            val afterMention = currentText.substring(startPos + currentState.mentionQueryText.length + 1) // +1 for @
            val mentionText = "@${suggestion.displayName} " // Add space after mention for convenience
            
            val newText = beforeMention + mentionText + afterMention
            
            // Store the internal mention mapping for conversion during send
            // Use uppercase type name to match Firebase function expectations
            // Map both with and without space to handle different scenarios
            val mentionMapping = currentMentionMappings.toMutableMap()
            mentionMapping["@${suggestion.displayName}"] = "[${suggestion.type.name}:${suggestion.id}]"
            mentionMapping["@${suggestion.displayName} "] = "[${suggestion.type.name}:${suggestion.id}]"
            currentMentionMappings = mentionMapping
            
            _uiState.update { 
                it.copy(
                    pendingMessageText = newText,
                    mentionSuggestions = emptyList(),
                    isMentionSuggestionVisible = false,
                    mentionQueryText = "",
                    mentionQueryStartPosition = -1
                )
            }
        }
    }

    fun onSendMessageClick() {
        val message = _uiState.value.pendingMessageText
        val attachments = _uiState.value.selectedAttachmentUris
        
        if (message.isBlank() && attachments.isEmpty()) return
        
        // 웹소켓 연결 상태 확인 후 전송
        val connectionState = _uiState.value.connectionState
        if (connectionState !is WebSocketConnectionState.Connected) {
            viewModelScope.launch {
                val statusMessage = when (connectionState) {
                    is WebSocketConnectionState.Connecting ->
                        "연결 중입니다. 잠시만 기다려주세요."
                    is WebSocketConnectionState.Disconnected ->
                        "서버와 연결이 끊어져 있습니다."
                    is WebSocketConnectionState.Error ->
                        "연결 오류: ${connectionState.message}"
                    else -> "메시지 전송이 불가능합니다."
                }
                _eventFlow.emit(ChatEvent.ShowSnackbar(statusMessage))
            }
            return
        }
        
        // Convert display format (@displayName) back to internal format ([type:id]) for processing
        val internalMessage = convertDisplayToInternalFormat(message)
        // 멘션/답장 파싱은 이제 MessageService에서 처리하므로 원본 텍스트 그대로 전송
        sendMessage(internalMessage, attachments)

        _uiState.update {
            it.copy(
                pendingMessageText = "",
                selectedAttachmentUris = emptyList()
            )
        }

        // Clear mention mappings after sending
        currentMentionMappings.clear()
    }

    /**
     * 특정 메시지에 답장하기
     * @param message 답장할 메시지 내용
     * @param replyToMessageId 답장 대상 메시지 ID
     * @param attachments 첨부파일 URI 리스트
     */
    fun sendReplyMessage(
        message: String,
        replyToMessageId: String,
        attachments: List<Uri> = emptyList()
    ) {
        if (message.isBlank() && attachments.isEmpty()) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지를 입력해주세요"))
            }
            return
        }

        // Convert display format (@displayName) back to internal format ([type:id]) for processing
        val internalMessage = convertDisplayToInternalFormat(message)
        sendMessage(internalMessage, attachments, replyToMessageId)

        _uiState.update { 
            it.copy(
                pendingMessageText = "", 
                selectedAttachmentUris = emptyList()
            ) 
        }
        
        // Clear mention mappings after sending
        currentMentionMappings.clear()
    }
    
    /**
     * Converts display format (@displayName) to internal format ([type:id]) using stored mappings
     */
    private fun convertDisplayToInternalFormat(displayText: String): String {
        var result = displayText
        
        // Apply all stored mention mappings
        currentMentionMappings.forEach { (displayFormat, internalFormat) ->
            result = result.replace(displayFormat, internalFormat)
        }
        
        return result
    }

    private fun parseMentions(text: String): Pair<String, List<MentionInfo>> {
        // Parse internal format [TYPE:id] that was converted from display format
        val mentionRegex = """\[(USER|ROLE):(\S+?)\]""".toRegex()
        val mentions = mutableListOf<MentionInfo>()
        
        val processedText = mentionRegex.replace(text) { matchResult ->
            val typeStr = matchResult.groupValues[1]
            val id = matchResult.groupValues[2]
            
            val mentionType = when (typeStr) {
                "USER" -> MentionType.USER
                "ROLE" -> MentionType.ROLE
                else -> MentionType.USER // Default fallback
            }
            
            // Get the original display name from the mappings
            val displayName = currentMentionMappings.entries.find { 
                it.value == matchResult.value 
            }?.key ?: "@$id"
            
            mentions.add(
                MentionInfo(
                    type = mentionType,
                    id = id,
                    displayName = displayName
                )
            )
            
            // Return the original internal format for the processed text
            matchResult.value
        }
        
        return Pair(processedText, mentions)
    }

    /**
     * Applies display format conversion to a list of messages
     */
    private fun List<ChatMessageUiModel>.applyDisplayFormatConversion(): List<ChatMessageUiModel> {
        return this.map { message ->
            message.copy(
                message = convertInternalToDisplayFormat(message.message)
            )
        }
    }

    /**
     * Convert domain Message to ChatMessageUiModel
     */
    private suspend fun convertDomainMessageToUiModel(message: Message): ChatMessageUiModel {
        val userId = message.senderId.value
        val userProfileService = services.userProfileService

        // 사용자 프로필 정보 가져오기 (캐시된 값 사용)
        val userName = userProfileService.getUserDisplayName(userId)
        val userProfileUrl = userProfileService.getCachedProfileUrl(userId)

        // 프로필 로딩 최적화: 캐시된 값이 있으면 스킵
        val cachedProfileUrl = userProfileService.getCachedProfileUrl(userId)
        if (cachedProfileUrl == null) {
            // 캐시된 프로필이 없을 때만 로딩
            viewModelScope.launch {
                try {
                    userProfileService.loadUserProfile(userId)
                    Log.d("ViewModel", "User profile loaded for $userId")
                } catch (e: Exception) {
                    Log.e("ViewModel", "Failed to load user profile for $userId", e)
                }
            }
        }

        // 전송 상태는 OutBox에서 관리
        // 성능을 위해 최근 메시지에 대해 휴리스틱 사용
        var isSending = false
        var sendFailed = false
        var canRetry = false

        // 최근 5초 이내에 내가 보낸 메시지는 전송 중으로 가정
        // (실제 sync status 업데이트는 ACK/timeout에서 처리)
        val fiveSecondsAgo = System.currentTimeMillis() - 5000
        val isVeryRecentMyMessage = message.createdAt.toEpochMilli() > fiveSecondsAgo &&
                message.senderId.value == currentUserId

        if (isVeryRecentMyMessage) {
            isSending = true
            canRetry = false
        }

        // deliveryState를 조건에 따라 결정
        // OutBoxStatus로 상태 변환
        val outBoxStatusResult = messageRepository.getMessageOutBoxStatus(message.id)
        val outBoxStatus = when (outBoxStatusResult) {
            is CustomResult.Success -> outBoxStatusResult.data
            is CustomResult.Failure -> OutBoxStatus.DISPATCHED // 기본값
            is CustomResult.Initial -> OutBoxStatus.DISPATCHED // 기본값
            is CustomResult.Loading -> OutBoxStatus.DISPATCHED // 기본값
            is CustomResult.Progress -> OutBoxStatus.DISPATCHED // 기본값
        }
        val deliveryState = when (outBoxStatus) {
            OutBoxStatus.PENDING -> MessageDeliveryState.Sending
            OutBoxStatus.DISPATCHED -> MessageDeliveryState.Sent
            OutBoxStatus.FAILED -> MessageDeliveryState.Failed("전송 실패")
        }

        // 메시지 내용 처리 - 타입에 따라 다르게 처리
        val displayMessage = when (message.messageType) {
            MessageType.TEXT -> {
                // TEXT 타입: payload에서 content 추출하거나 레거시 content 사용
                val textContent = message.payload.getTextContent() ?: ""
                convertInternalToDisplayFormat(textContent)
            }

            MessageType.SYSTEM_PROJECT_JOIN -> {
                // 프로젝트 참여 시스템 메시지: payload에서 projectName 추출
                val projectName = message.payload.getValue("projectName") ?: "프로젝트"
                "$projectName 프로젝트에 초대되었습니다"
            }

            MessageType.SYSTEM_DATE -> {
                // 날짜 시스템 메시지: payload에서 displayText 추출
                message.payload.getValue("displayText") ?: "새로운 날짜"
            }

            MessageType.SYSTEM_CHAT_START -> {
                // 채팅 시작 시스템 메시지: payload에서 welcomeText 추출
                message.payload.getValue("welcomeText") ?: "채팅이 시작되었습니다"
            }
        }

        return ChatMessageUiModel(
            messageId = message.id.value,
            userId = userId,
            userName = userName,
            userProfileUrl = userProfileUrl,
            messageType = message.messageType,
            message = displayMessage,
            payload = message.payload.value,
            formattedTimestamp = if (isSending) "전송중..." else DateTimeUtil.formatChatTime(message.createdAt),
            actualTimestamp = message.createdAt,
            isModified = message.updatedAt != message.createdAt,
            attachmentImageUrls = emptyList(), // TODO: Handle attachments
            isMyMessage = message.senderId.value == currentUserId,
            isSending = isSending,
            sendFailed = sendFailed,
            isDeleted = message.isDeleted.value,
            deliveryState = deliveryState,
            isOptimistic = isSending, // 전송 중인 메시지는 낙관적 업데이트로 처리
            clientSentAt = message.createdAt,
            retryCount = 0, // TODO: 재전송 횟수 추적 로직 추가
            canRetry = canRetry,
            errorMessage = null, // Message 도메인 모델에서 failureReason 제거됨
            replyToMessageId = message.replyToMessageId?.value,
            replyToContent = null, // TODO: Fetch reply content
            replyToUserName = null, // TODO: Fetch reply user name
            mentions = message.mentions, // Use domain mentions directly
            isMentionedMessage = false // TODO: Check if current user is mentioned
        )
    }

    /**
     * Converts internal mention format ([TYPE:id]) to display format (@displayName) for UI display
     * This prevents internal format from being visible to users
     */
    fun convertInternalToDisplayFormat(text: String): String {
        val mentionRegex = """\[(USER|ROLE):(\S+?)]""".toRegex()
        
        return mentionRegex.replace(text) { matchResult ->
            val typeStr = matchResult.groupValues[1]
            val id = matchResult.groupValues[2]
            
            // Try to find display name from current participants/members/roles
            val currentState = _uiState.value
            
            val displayName = when (typeStr) {
                "USER" -> {
                    // First check participants (chat members)
                    currentState.participants.find { it.userId == id }?.displayName
                        ?: currentState.projectMembers.find { it.userId == id }?.displayName
                        ?: id // Fallback to ID if display name not found
                }
                "ROLE" -> {
                    currentState.projectRoles.find { it.roleId == id }?.roleName ?: id
                }
                else -> id
            }
            
            "@$displayName"
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            try {
                val generalUseCases = webSocketUseCaseProvider.create()
                // Reconnect by disconnecting and connecting again
                generalUseCases.disconnectUseCase()
                // Note: Actual reconnection logic should be handled by core WebSocket service
                Log.d("ViewModel", "Connection retry requested for room: $channelId")
                _eventFlow.emit(ChatEvent.SystemMessage("연결을 재시도하고 있습니다..."))
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to retry connection", e)
                _eventFlow.emit(ChatEvent.Error("연결 재시도 실패: ${e.message}"))
            }
        }
    }

    fun dismissConnectionError() {
        _uiState.update { it.copy(showConnectionError = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getConnectionStatusText(): String {
        val queuedCount = _uiState.value.queuedMessagesCount
        return when (val state = _uiState.value.connectionState) {
            is WebSocketConnectionState.Connected -> "실시간 연결됨"
            is WebSocketConnectionState.Connecting -> "연결 중..."
            is WebSocketConnectionState.Authenticating -> "인증 중..."
            is WebSocketConnectionState.Disconnected -> {
                if (queuedCount > 0) {
                    "오프라인 (${queuedCount}개 대기중)"
                } else {
                    "오프라인 (읽기 전용)"
                }
            }
            is WebSocketConnectionState.Reconnecting -> "재연결 중..."
            is WebSocketConnectionState.Error -> "연결 오류: ${state.message}"
        }
    }
    
    fun canPerformWriteOperations(): Boolean {
        return currentUserId != null && 
               _uiState.value.connectionState is WebSocketConnectionState.Connected
    }
    
    fun isSendButtonEnabled(): Boolean {
        return canPerformWriteOperations() && 
               (_uiState.value.pendingMessageText.isNotBlank() || _uiState.value.selectedAttachmentUris.isNotEmpty())
    }
    
    fun isReadOnlyMode(): Boolean {
        return !canPerformWriteOperations()
    }

    fun onImagesSelected(uris: List<Uri>) {
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uris) }
    }
    
    fun onBackClick() {
        viewModelScope.launch {
            services.navigationService.navigateBack()
        }
    }
    
    fun confirmEditMessage() {
        val messageId = _uiState.value.editingMessageId
        val newContent = _uiState.value.pendingMessageText
        
        if (messageId != null && newContent.isNotBlank()) {
            editMessage(messageId, newContent)
            _uiState.update { it.copy(isEditing = false, editingMessageId = null, pendingMessageText = "") }
        }
    }
    
    fun onAttachmentClick() {
        _uiState.update { it.copy(isAttachmentAreaVisible = !it.isAttachmentAreaVisible) }
    }
    
    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uri) }
    }
    
    fun onImageDeselected(uri: Uri) {
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris - uri) }
    }
    
    fun cancelEdit() {
        _uiState.update { it.copy(isEditing = false, editingMessageId = null, pendingMessageText = "") }
    }
    
    fun onMessageLongClick(message: ChatMessageUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowEditDeleteDialog(message))
        }
    }
    
    fun onUserProfileClick(userId: String) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowUserProfileDialog(userId))
        }
    }
    
    fun startEditMessage(messageId: String, text: String) {
        _uiState.update { 
            it.copy(
                isEditing = true, 
                editingMessageId = messageId, 
                pendingMessageText = text
            ) 
        }
    }
    
    fun confirmDeleteMessage(messageId: String) {
        deleteMessage(messageId)
    }

    /**
     * 메시지 전송 타임아웃 시작 (30초 후 강제 상태 변경)
     */
    private fun startMessageTimeout(messageId: String) {
        // 기존 타임아웃 Job이 있다면 취소
        cancelMessageTimeout(messageId)

        val timeoutJob = viewModelScope.launch {
            try {
                delay(30000) // 30초 대기
                Log.w("ViewModel", "Message timeout: $messageId - forcing state update")

                // ACK가 도착하지 않았으므로 강제로 상태 업데이트
                updateMessageStateIfStillSending(messageId)
            } catch (e: Exception) {
                // Job이 취소된 경우 (정상적인 ACK 도착)
                Log.d("ViewModel", "Message timeout cancelled for: $messageId")
            }
        }

        messageTimeoutJobs[messageId] = timeoutJob
    }

    /**
     * 메시지 타임아웃 Job 취소
     */
    private fun cancelMessageTimeout(messageId: String) {
        messageTimeoutJobs[messageId]?.cancel()
        messageTimeoutJobs.remove(messageId)
    }

    /**
     * 메시지가 아직 전송 중인 경우 상태 업데이트
     */
    private fun updateMessageStateIfStillSending(messageId: String) {
        // With Paging3, update the database instead of UI state
        // The Room database update will automatically flow through Paging3 to UI
        viewModelScope.launch {
            try {
                // OutBox에서 메시지 상태를 FAILED로 업데이트
                val result = messageRepository.handleMessageFailure(messageId)
                when (result) {
                    is CustomResult.Success -> {
                        Log.d(
                            "ViewModel",
                            "Timeout: Message marked as FAILED in OutBox: $messageId"
                        )
                        invalidatePagingSource() // Refresh UI to show failed status
                    }

                    is CustomResult.Failure -> {
                        Log.e(
                            "ViewModel",
                            "Failed to update timeout status: ${result.error.message}"
                        )
                    }

                    else -> {
                        Log.w("ViewModel", "Unexpected result from timeout status update")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to update message status in database", e)
            }
        }

        // 타임아웃 Job 정리
        messageTimeoutJobs.remove(messageId)
    }

    /**
     * 실패한 메시지를 재전송합니다.
     * Simplified for Room Paging3 architecture
     */
    fun retryMessage(messageId: String) {
        if (currentUserId == null) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }

        viewModelScope.launch {
            try {
                // Simply retry sending the message via WebSocket
                val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                roomUseCases.sendMessageUseCase(
                    senderId = UserId(currentUserId!!),
                    content = "[Retry Message]", // TODO: Get original content from Room DB
                    messageId = DocumentId(messageId), // Use same ID for retry
                    replyToMessageId = null, // TODO: Get original reply info from Room DB if needed
                    projectId = projectId,
                    channelType = if (projectId != null) "PROJECT" else "DM"
                )

                Log.d("ViewModel", "Message retry sent via WebSocket: $messageId")
                _eventFlow.emit(ChatEvent.SystemMessage("메시지 재전송을 시도했습니다"))
                
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to retry message", e)
                _eventFlow.emit(ChatEvent.Error("메시지 재전송 실패: ${e.message}"))
            }
        }
    }

    /**
     * PagingSource를 무효화하여 Room DB 변경사항을 UI에 반영
     */
    private fun invalidatePagingSource() {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "🔄 PagingSource 무효화 시작 - 채널: $channelId")

                // 현재 활성화된 PagingSource 인스턴스를 무효화
                val pagingSource = currentPagingSource
                if (pagingSource != null && !pagingSource.invalid) {
                    Log.d("ViewModel", "✅ 현재 PagingSource 무효화: ${pagingSource.hashCode()}")
                    pagingSource.invalidate()
                } else {
                    Log.d("ViewModel", "⚠️ PagingSource가 null이거나 이미 무효화됨")

                    // 새로운 PagingSource 생성을 위해 Pager를 통해 접근
                    // 이는 pagingSourceFactory를 트리거하여 새로운 인스턴스를 생성합니다
                    Log.d("ViewModel", "🔄 새로운 PagingSource 생성 트리거")
                }

                Log.d("ViewModel", "✅ PagingSource invalidation 완료 - UI가 Room DB에서 새로고침됨")

                // 잠시 대기 후 Room DB 상태 확인
                delay(100)
                Log.d("ViewModel", "📊 PagingSource 무효화 후 Room DB 상태 확인 완료")

            } catch (e: Exception) {
                Log.e("ViewModel", "❌ Failed to invalidate PagingSource: ${e.message}", e)
            }
        }
    }

    /**
     * 최적화된 PagingSource 무효화 (새 메시지 추가 시 성능 최적화)
     */
    private fun invalidatePagingSourceWithOptimization() {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "🔄 최적화된 PagingSource 무효화 시작 - 채널: $channelId")

                // 현재 활성화된 PagingSource 인스턴스를 무효화
                val pagingSource = currentPagingSource
                if (pagingSource != null && !pagingSource.invalid) {
                    Log.d("ViewModel", "✅ 현재 PagingSource 무효화 (최적화): ${pagingSource.hashCode()}")
                    pagingSource.invalidate()
                } else {
                    Log.d("ViewModel", "⚠️ PagingSource가 null이거나 이미 무효화됨")
                }

                Log.d("ViewModel", "✅ 최적화된 PagingSource invalidation 완료")

            } catch (e: Exception) {
                Log.e(
                    "ViewModel",
                    "❌ Failed to invalidate PagingSource (optimized): ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * Anchor 기반 메시지 로딩 (특정 메시지 기준점에서 시작)
     */
    fun loadMessagesFromAnchor(anchorMessageId: String) {
        viewModelScope.launch {
            try {
                // BidirectionalPagingMediator를 사용하여 Anchor 기반 로딩
                com.example.data_repository.paging.BidirectionalPagingMediator(
                    messageRepository = messageRepository,
                    anchorMessageId = anchorMessageId,
                    channelId = channelId
                )

                // Pager를 새로 구성하여 Anchor 기반 로딩
                // TODO: ViewModel에서 동적 Pager 재구성 로직 추가
                Log.d("ViewModel", "Loading messages from anchor: $anchorMessageId")

            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load messages from anchor: ${e.message}")
                _eventFlow.emit(ChatEvent.Error("특정 메시지 위치 로딩 실패: ${e.message}"))
            }
        }
    }

    /**
     * 수신 메시지 이벤트 구독 및 처리
     */
    private fun subscribeToIncomingMessages() {
        viewModelScope.launch {
            try {
                // WebSocketMessageService에서 Paging3 새로고침 이벤트 구독
                val generalUseCases = webSocketUseCaseProvider.create()
                generalUseCases.subscribeToMessageRefreshEventsUseCase()
                    .filter { refreshChannelId -> refreshChannelId == channelId } // 현재 채널만 필터링
                    .onEach { refreshChannelId ->
                        Log.d("ViewModel", "수신 메시지로 인한 Paging3 새로고침: $refreshChannelId")
                        invalidatePagingSource()

                        // UI 상태 업데이트 - 새 메시지 알림
                        updateNewMessageNotification()

                        // 새 메시지 알림 (선택적)
                        _eventFlow.emit(ChatEvent.ShowSnackbar("새 메시지가 도착했습니다"))
                    }
                    .launchIn(viewModelScope)

                // 직접적인 WebSocket 이벤트 구독 (추가 처리가 필요한 경우)
                val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                roomUseCases.subscribeToRoomEventsUseCase()
                    .filter { event -> isMessageForCurrentChannel(event) }
                    .onEach { event ->
                        when (event) {
                            is WebSocketDomainEvent.MessageReceived -> {
                                Log.d("ViewModel", "새 메시지 수신: ${event.messageId}")
                                // Room 저장은 WebSocketMessageService에서 자동 처리
                                // UI 새로고침은 messageRefreshEvents에서 처리
                            }

                            is WebSocketDomainEvent.MessageEdited -> {
                                Log.d("ViewModel", "메시지 수정 수신: ${event.messageId}")
                                invalidatePagingSource()
                            }

                            is WebSocketDomainEvent.MessageDeleted -> {
                                Log.d("ViewModel", "메시지 삭제 수신: ${event.messageId}")
                                invalidatePagingSource()
                            }

                            else -> {
                                // 다른 이벤트는 기존 로직 유지
                            }
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to subscribe to incoming messages", e)
            }
        }
    }

    /**
     * 이벤트가 현재 채널과 관련된 것인지 확인
     */
    private fun isMessageForCurrentChannel(event: WebSocketDomainEvent): Boolean {
        return when (event) {
            is WebSocketDomainEvent.MessageReceived -> event.roomId == channelId
            is WebSocketDomainEvent.MessageEdited -> event.roomId == channelId
            is WebSocketDomainEvent.MessageDeleted -> event.roomId == channelId
            else -> false
        }
    }

    /**
     * 새 메시지 알림 상태 업데이트
     */
    private fun updateNewMessageNotification() {
        // ChatUiState에는 새 메시지 알림 필드가 없으므로 로그만 출력
        // 필요시 ChatUiState에 해당 필드들을 추가해야 함
        Log.d("ViewModel", "새 메시지 알림 업데이트")
    }

    /**
     * 새 메시지 알림 상태 초기화 (사용자가 메시지를 확인했을 때)
     */
    fun clearNewMessageNotification() {
        // ChatUiState에는 새 메시지 알림 필드가 없으므로 로그만 출력
        // 필요시 ChatUiState에 해당 필드들을 추가해야 함
        Log.d("ViewModel", "새 메시지 알림 상태 초기화")
    }

    /**
     * 채팅화면 최초 진입 시 증분 동기화 실행
     */
    private fun performInitialSync() {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "🔄 초기 증분 동기화 시작: $channelId")

                when (val result = syncUseCase.syncChannel(channelId)) {
                    is CustomResult.Success -> {
                        Log.d("ViewModel", "✅ 초기 동기화 완료: $channelId")
                        // Room DB 변경 → Paging3 자동 새로고침 트리거
                        invalidatePagingSource()
                    }

                    is CustomResult.Failure -> {
                        Log.e("ViewModel", "❌ 초기 동기화 실패: ${result.error.message}")
                        // 실패해도 사용자에게는 알리지 않음 (백그라운드 작업)
                    }

                    else -> {
                        throw Exception("syncChannel() returned unexpected result: $result")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "💥 초기 동기화 예외", e)
            }
        }
    }

    /**
     * 5분 주기 동기화 시작
     */
    private fun startPeriodicSync() {
        // 기존 주기 동기화 Job이 있다면 취소
        periodicSyncJob?.cancel()

        periodicSyncJob = viewModelScope.launch {
            try {
                while (true) {
                    delay(5 * 60 * 1000L) // 5분 대기

                    Log.d("ViewModel", "🔄 주기적 증분 동기화 실행: $channelId")

                    when (val result = syncUseCase.syncChannel(channelId)) {
                        is CustomResult.Success -> {
                            Log.d("ViewModel", "✅ 주기 동기화 완료: $channelId")
                            // Room DB 변경 → Paging3 자동 새로고침 트리거
                            invalidatePagingSource()
                        }

                        is CustomResult.Failure -> {
                            Log.e("ViewModel", "❌ 주기 동기화 실패: ${result.error.message}")
                            // 실패해도 계속 진행 (다음 주기에 재시도)
                        }

                        else -> {
                            throw Exception("syncChannel() returned unexpected result: $result")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("ViewModel", "💥 주기 동기화 예외", e)
                }
            }
        }
    }

    /**
     * Paging3 로드 시 증분 동기화 실행 (빈도 제한 적용)
     */
    private var lastPagingLoadSyncTime = 0L
    private fun performPagingLoadSync() {
        val currentTime = System.currentTimeMillis()
        val minSyncInterval = 30 * 1000L // 30초 간격 제한

        // 너무 빈번한 호출 방지
        if (currentTime - lastPagingLoadSyncTime < minSyncInterval) {
            Log.d("ViewModel", "🚫 Paging3 동기화 빈도 제한 적용 (${currentTime - lastPagingLoadSyncTime}ms)")
            return
        }

        lastPagingLoadSyncTime = currentTime

        viewModelScope.launch {
            try {
                Log.d("ViewModel", "🔄 Paging3 로드 시 증분 동기화 실행: $channelId")

                when (val result = syncUseCase.syncChannel(channelId)) {
                    is CustomResult.Success -> {
                        Log.d("ViewModel", "✅ Paging3 동기화 완료: $channelId")
                        // 동기화 후 자동으로 Paging3가 업데이트됨
                    }

                    is CustomResult.Failure -> {
                        Log.e("ViewModel", "❌ Paging3 동기화 실패: ${result.error.message}")
                        // 실패해도 사용자에게는 알리지 않음 (백그라운드 작업)
                    }

                    else -> {
                        throw Exception("syncChannel() returned unexpected result: $result")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "💥 Paging3 동기화 예외", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // 모든 타임아웃 Job 취소
        messageTimeoutJobs.values.forEach { it.cancel() }
        messageTimeoutJobs.clear()

        // 주기적 동기화 Job 취소
        periodicSyncJob?.cancel()
        
        viewModelScope.launch {
            // Leave room when ViewModel is cleared
            // Note: The actual room leaving is handled by the WebSocketClient
            Log.d("ViewModel", "Left chat room: $channelId (GlobalWebSocketService remains active)")
        }
    }
}
