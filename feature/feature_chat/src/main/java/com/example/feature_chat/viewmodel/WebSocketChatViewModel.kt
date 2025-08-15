package com.example.feature_chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.vo.DocumentId
import com.example.domain.vo.MentionType
import com.example.domain.vo.UserId
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.core_common.util.SyncThrottler
import com.example.domain_usecase.usecase.sync.SyncUseCase
import com.example.domain_usecase.usecase.project.AcceptMemberInvitationUseCase
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.service.ChatServiceProvider
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.usecase.WebSocketUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 단순화된 WebSocketChatViewModel - MessageService를 통한 중간 라우터
 *
 * 역할:
 * - MessageService의 기능을 UI에 전달
 * - UI 상태 관리 (ChatUiState)
 * - 사용자 인터페이스 이벤트 처리
 * - 초기화 및 생명주기 관리
 *
 * 모든 비즈니스 로직은 MessageService로 위임
 */
@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val chatServiceProvider: ChatServiceProvider,
    private val syncThrottler: SyncThrottler,
    private val syncUseCase: SyncUseCase,
    private val acceptMemberInvitationUseCase: AcceptMemberInvitationUseCase
) : ViewModel() {

    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val projectId: String? = savedStateHandle.get<String>(RouteArgs.PROJECT_ID)
    private val initialMessageId: String? = savedStateHandle.get<String>("initialMessageId")

    // Services are initialized lazily once we determine the channel type
    private val services by lazy {
        if (projectId != null) {
            Log.d(TAG, "Creating services for project channel: $projectId/$channelId")
            chatServiceProvider.createForProjectChannel(projectId, channelId)
        } else {
            Log.d(TAG, "Creating services for DM channel: $channelId")
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

    // 🎯 MessageService를 통한 모든 페이징 기능 위임
    // UI 모델 변환까지 포함된 완전한 페이징 플로우
    val messagesFlow: Flow<PagingData<ChatMessageUiModel>> by lazy {
        Log.d(TAG, "🚀 messagesFlow lazy 초기화 시작 (initialMessageId=$initialMessageId)")
        services.messageService.getUiMessagesPagingFlow(initialMessageId).cachedIn(viewModelScope)
    }

    // 🎯 MessageService의 상태들을 UI에 전달
    val isAnchorJumpInProgress: StateFlow<Boolean> = services.messageService.isAnchorJumpInProgress
    val anchorTargetMessageId: StateFlow<String?> = services.messageService.anchorTargetMessageId

    init {
        // 1. 🎯 Pager initialKey 방식을 사용하므로 초기 Anchor 설정은 생략

        // 2. 채팅방 즉시 입장 (해당 방 이벤트만 수신/저장하도록 보장)
        viewModelScope.launch {
            try {
                val joinResult = webSocketUseCaseProvider.createForRoom(channelId).joinRoomUseCase(
                    userId = AuthUtil.getCurrentUserId()?.let { UserId(it) }
                )
                if (joinResult.isSuccess) {
                    Log.d(TAG, "✅ 방 입장 성공: $channelId")

                    // 채널 입장 시 초기 동기화 실행 (SyncMetadata 기반)
                    if (syncThrottler.canSync(channelId, "Initial")) {
                        Log.d(TAG, "🚀 채널 입장시 초기 동기화 시작: $channelId")
                        try {
                            val syncResult = syncUseCase.syncChannel(channelId)
                            if (syncResult.isSuccess) {
                                syncThrottler.markSynced(channelId, "Initial")
                                Log.d(TAG, "✅ 초기 동기화 완료: $channelId")
                            } else {
                                when (syncResult) {
                                    is CustomResult.Failure -> Log.w(
                                        TAG,
                                        "⚠️ 초기 동기화 실패: $channelId - ${syncResult.error.message}"
                                    )

                                    else -> Log.w(
                                        TAG,
                                        "⚠️ 초기 동기화 실패: $channelId - Unknown error"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "💥 초기 동기화 예외: $channelId", e)
                        }
                    } else {
                        Log.d(TAG, "🔥 채널 입장시 초기 동기화 쿨다운 중: $channelId")
                    }

                    // 동기화 완료 후 추가 초기화 작업 (필요시)
                    withContext(Dispatchers.IO) {
                        // 향후 필요한 초기화 작업이 있으면 여기에 추가
                    }
                } else {
                    Log.e(
                        TAG,
                        "❌ 방 입장 실패: $channelId - ${joinResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 방 입장 중 예외: $channelId", e)
            }
        }

        // 3. 초기 채팅 아이템 로딩 관련: 캐시 확인/UX 최적화 로그만 유지
        //    실제 데이터 로딩은 Paging3 + Room 캐시로 자동 처리
        viewModelScope.launch { optimizeUserExperience("CACHE_AVAILABLE") }

        // 4. WebSocket 이벤트 구독 (UI 이벤트만)
        observeWebSocketEventsForUiEvents()
        // 프로필 업데이트 신호가 오면 Paging을 자동 재매핑하도록 트리거 (간단히 event만 발생)
        viewModelScope.launch {
            services.profileUpdates.collect { _ ->
                // UI는 Paging 아이템 recompose 시 최신 이름/이미지를 조회하여 반영
                _eventFlow.emit(ChatEvent.SystemMessage("프로필이 갱신되었습니다"))
            }
        }

        // Room DB 최신 내용 로그 + Paging 새로고침 트리거 (메시지 변경시)
        viewModelScope.launch {
            webSocketUseCaseProvider.create().subscribeToMessageRefreshEventsUseCase()
                .onEach { channelId ->
                    if (channelId == this@WebSocketChatViewModel.channelId) {
                        Log.d(TAG, "🗂️ Room DB 최신 내용 요청 (channel=$channelId)")
                        _eventFlow.emit(ChatEvent.RefreshMessages)
                    }
                }
                .launchIn(this)
        }

        // 5. 연결 상태 모니터링
        observeConnectionState()

        // 6. 오프라인 큐 모니터링
        observeOfflineQueue()

        // 7. 주기적인 캐시 정리 (1시간마다)
        startPeriodicCacheCleanup()

        // Note: messagesFlow는 lazy property로 설정되어 처음 접근 시점에 생성됨
    }

    /**
     * 초기 채팅 아이템 로딩 (SSOT 패턴 - Room DB만 관찰)
     */
    private fun loadInitialChatItems() {
        viewModelScope.launch {
            try {
                // 사용자 경험 최적화
                val cacheStatus = checkRoomDBCache()
                optimizeUserExperience(cacheStatus)

            } catch (e: Exception) {
                Log.e(TAG, "❌ 초기 채팅 아이템 로딩 실패", e)
            }
        }
    }

    // ================================
    // 🎯 MessageService 기능 위임 (단순 라우터)
    // ================================

    /**
     * 특정 메시지 ID로 Anchor Jump (MessageService 위임)
     */
    fun jumpToMessage(messageId: String) {
        viewModelScope.launch {
            try {
                services.messageService.jumpToMessage(messageId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Anchor Jump 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지로 이동하는 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 최신 메시지로 Anchor Jump (MessageService 위임)
     */
    fun jumpToLatest() {
        viewModelScope.launch {
            try {
                services.messageService.jumpToLatest()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 최신 메시지 조회 중 오류", e)
            }
        }
    }

    // sendMessage(content, ...) 래퍼 제거: ViewModel에서는 onSendMessageClick에서 service를 직접 호출

    // 이미지 메시지 전송 전용 API는 제거하고, 통합 send만 사용하도록 유도

    /**
     * 다중 이미지 메시지 전송 (MessageService 위임)
     */
    // 다중 이미지 전송 전용 API는 제거하고, 통합 send만 사용하도록 유도

    /**
     * 프로젝트 멤버 초대 메시지 전송 (MessageService 위임)
     */
    fun sendMemberInvitation(targetUserId: String, projectName: String, inviterName: String) {
        viewModelScope.launch {
            try {
                val senderId = AuthUtil.getCurrentUserId()
                val currentProjectId = projectId ?: return@launch

                val inviteContent = "${inviterName}님이 '${projectName}' 프로젝트로 초대했습니다."
                val metadata = mapOf(
                    "projectId" to currentProjectId,
                    "projectName" to projectName,
                    "inviterName" to inviterName,
                    "targetUserId" to targetUserId
                )
                val result = services.messageService.sendMessage(
                    senderId = UserId(senderId),
                    textContent = inviteContent,
                    isSystemMessage = true,
                    systemType = "USER_INVITE",
                    additionalMetadata = metadata
                )

                when (result) {
                    is CustomResult.Success<*> -> {
                        Log.d(TAG, "✅ 멤버 초대 메시지 전송 성공: ${result.data}")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("멤버 초대 메시지를 전송했습니다"))
                        _eventFlow.emit(ChatEvent.ScrollToBottom)
                    }

                    is CustomResult.Failure<*> -> {
                        Log.e(TAG, "❌ 멤버 초대 메시지 전송 실패: ${result.error}")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("멤버 초대 전송에 실패했습니다"))
                    }

                    else -> {
                        Log.w(TAG, "⚠️ 멤버 초대 메시지 전송 결과: 알 수 없는 상태")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 멤버 초대 메시지 전송 중 예외", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("멤버 초대 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 메시지 수정 (MessageService 위임)
     */
    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            try {
                val result = services.messageService.editMessage(
                    messageId = DocumentId(messageId),
                    newContent = newContent
                )
                
                when (result) {
                    is CustomResult.Success<*> -> {
                        Log.d(TAG, "✅ 메시지 수정 요청 전송(낙관적 적용됨)")
                    }

                    is CustomResult.Failure<*> -> {
                        Log.e(TAG, "❌ 메시지 수정 실패: ${result.error}")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 수정에 실패했습니다"))
                    }
                    else -> {
                        Log.w(TAG, "⚠️ 메시지 수정 결과: 알 수 없는 상태")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 수정 중 예외", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 수정 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 메시지 삭제 (MessageService 위임)
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val result = services.messageService.deleteMessage(DocumentId(messageId))
                
                when (result) {
                    is CustomResult.Success<*> -> {
                        Log.d(TAG, "✅ 메시지 삭제 요청 전송(낙관적 적용됨)")
                    }

                    is CustomResult.Failure<*> -> {
                        Log.e(TAG, "❌ 메시지 삭제 실패: ${result.error}")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 삭제에 실패했습니다"))
                    }

                    else -> {
                        Log.w(TAG, "⚠️ 메시지 삭제 결과: 알 수 없는 상태")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 삭제 중 예외", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 삭제 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 실패한 메시지 재전송
     */
    fun retryFailedMessage(messageId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 실패한 메시지 재전송 시작: $messageId")

                val result = services.messageService.retryFailedImageMessage(DocumentId(messageId))

                when (result) {
                    is CustomResult.Success<*> -> {
                        Log.d(TAG, "✅ 메시지 재전송 성공: $messageId")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 재전송을 시작했습니다"))
                    }

                    is CustomResult.Failure<*> -> {
                        Log.e(TAG, "❌ 메시지 재전송 실패: ${result.error}")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 재전송에 실패했습니다: ${result.error}"))
                    }

                    else -> {
                        Log.w(TAG, "⚠️ 메시지 재전송 결과: 알 수 없는 상태")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 재전송 상태를 확인할 수 없습니다"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 재전송 중 예외", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 재전송 중 오류가 발생했습니다"))
            }
        }
    }

    // ================================
    // 🎯 기존 UI 관련 기능들 (유지)
    // ================================


    /**
     * Room DB 캐시 상태 확인
     */
    private suspend fun checkRoomDBCache(): String = "CACHE_AVAILABLE"

    /**
     * 사용자 경험 최적화
     */
    private fun optimizeUserExperience(cacheStatus: String) {
        when (cacheStatus) {
            "CACHE_AVAILABLE" -> {
                Log.d(TAG, "✅ 캐시된 메시지가 있어 빠른 로딩 가능")
            }

            "CACHE_ERROR" -> {
                Log.w(TAG, "⚠️ 캐시 확인 실패, 기본 로딩 진행")
            }

            else -> {
                Log.d(TAG, "ℹ️ 캐시 상태: $cacheStatus")
            }
        }
    }

    // ================================
    // 🎯 WebSocket 이벤트 처리 (UI 이벤트만)
    // ================================

    /**
     * WebSocket 이벤트를 UI 이벤트로 변환하여 구독
     */
    private fun observeWebSocketEventsForUiEvents() {
        viewModelScope.launch {
            webSocketUseCaseProvider.createForRoom(channelId).subscribeToRoomEventsUseCase()
                .onEach { event ->
                    when (event) {
                        is WebSocketDomainEvent.Connected -> {
                            Log.d(TAG, "✅ WebSocket 연결됨")
                            _uiState.update {
                                it.copy(
                                    connectionState = WebSocketConnectionState.Connected(
                                        "ws://localhost"
                                    )
                                )
                            }
                        }

                        is WebSocketDomainEvent.Disconnected -> {
                            Log.d(TAG, "❌ WebSocket 연결 끊김")
                            _uiState.update { it.copy(connectionState = WebSocketConnectionState.Disconnected) }
                        }

                        is WebSocketDomainEvent.MessageReceived -> {
                            Log.d(TAG, "📨 메시지 수신: ${event.messageId}")
                            // ChatMessagesList에서 스크롤 위치를 자동 판단하여 처리
                            // 무조건 ScrollToBottom 하지 않고 사용자 스크롤 위치 존중
                        }

                        is WebSocketDomainEvent.MessageAck -> {
                            Log.d(
                                TAG,
                                "✅ 메시지 ACK 수신: ${event.messageId}, type: ${event.ackType}"
                            )
                            // MessageService의 ACK 처리 호출
                            viewModelScope.launch {
                                services.messageService.handleMessageAck(event.messageId)
                                // 낙관적 메타 정리 (편집/삭제 공통)
                                services.messageService.handleOptimisticAck(event.messageId)

                                // UI 갱신 이벤트 전송 (로딩 인디케이터 제거용)
                                _eventFlow.emit(ChatEvent.ScrollToBottom)

                                Log.d(TAG, "🔄 ACK 처리 후 UI 갱신 이벤트 전송: ${event.messageId}")
                            }
                        }

                        is WebSocketDomainEvent.MessageFailed -> {
                            Log.e(
                                TAG,
                                "❌ 메시지 전송 실패: ${event.messageId}, type: ${event.failureType}, error: ${event.errorMessage}"
                            )
                            // MessageService의 실패 처리 호출
                            viewModelScope.launch {
                                services.messageService.handleMessageFailure(event.messageId)
                                // 낙관적 롤백 적용
                                services.messageService.handleOptimisticFailure(event.messageId)
                            }
                            // 사용자에게 실패 알림
                            _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 전송에 실패했습니다"))
                        }

                        is WebSocketDomainEvent.Error -> {
                            Log.e(TAG, "❌ WebSocket 오류: ${event.message}")
                            _eventFlow.emit(ChatEvent.ShowSnackbar("연결 오류가 발생했습니다"))
                        }

                        else -> {
                            Log.d(TAG, "📡 WebSocket 이벤트: ${event::class.simpleName}")
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * 연결 상태 모니터링
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketUseCaseProvider.create().getConnectionStateUseCase()
                .onEach { state ->
                    Log.d(TAG, "🔗 연결 상태 변경: $state")
                    _uiState.update { it.copy(connectionState = state) }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * 오프라인 큐 상태 모니터링
     */
    private fun observeOfflineQueue() {
        viewModelScope.launch {
            // 주기적으로 큐 상태 업데이트 (5초마다)
            while (isActive) {
                try {
                    val (pendingCount, failedCount) = services.messageService.offlineMessageQueue.getQueueInfo()

                    _uiState.update { currentState ->
                        currentState.copy(
                            queuedMessagesCount = pendingCount,
                            failedMessagesCount = failedCount,
                            isRetryingMessages = pendingCount > 0 || failedCount > 0
                        )
                    }

                    if (pendingCount > 0 || failedCount > 0) {
                        Log.d(TAG, "📋 큐 상태 업데이트: 대기 ${pendingCount}개, 실패 ${failedCount}개")
                    }
                } catch (e: CancellationException) {
                    // 취소는 정상 종료로 간주
                    Log.d(TAG, "ℹ️ 큐 상태 모니터링 취소됨")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 큐 상태 모니터링 실패", e)
                }

                delay(5000) // 5초마다 확인
            }
        }
    }

    /**
     * 주기적인 캐시 정리 (메모리 및 디스크 사용량 최적화)
     */
    private fun startPeriodicCacheCleanup() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    delay(60 * 60 * 1000L) // 1시간 대기

                    Log.d(TAG, "🧹 주기적 캐시 정리 시작")
                    withContext(Dispatchers.IO) {
                        // cleanupCache 메서드 제거됨 - Room의 자동 무효화로 대체
                        Log.d(TAG, "캐시 정리는 Room 자동 무효화로 처리됨")
                    }
                    Log.d(TAG, "✅ 주기적 캐시 정리 완료")
                } catch (e: CancellationException) {
                    // 취소는 정상 종료로 간주
                    Log.d(TAG, "ℹ️ 주기적 캐시 정리 취소됨")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 주기적 캐시 정리 실패", e)
                    // 실패해도 계속 시도
                }
            }
        }
    }

    // ================================
    // 🎯 멘션 관련 기능 (UI 전용)
    // ================================

    /**
     * 멘션 제안 목록 생성
     */
    fun getMentionSuggestions(query: String): List<MentionSuggestion> {
        // TODO: 실제 사용자 목록에서 검색
        return listOf(
            MentionSuggestion(MentionType.USER, "user1", "사용자1"),
            MentionSuggestion(MentionType.USER, "user2", "사용자2")
        ).filter { it.displayName.contains(query, ignoreCase = true) }
    }

    /**
     * 멘션 매핑 업데이트
     */
    fun updateMentionMappings(mappings: Map<String, String>) {
        currentMentionMappings.clear()
        currentMentionMappings.putAll(mappings)
    }


    /**
     * 메시지 타임아웃 시작
     */
    private fun startMessageTimeout(messageId: String, timeoutMs: Long = 30000) {
        val job = viewModelScope.launch {
            delay(timeoutMs)
            // 타임아웃 시 처리 로직
            Log.w(TAG, "⏰ 메시지 타임아웃: $messageId")
        }
        messageTimeoutJobs[messageId] = job
    }

    /**
     * 메시지 타임아웃 취소
     */
    private fun cancelMessageTimeout(messageId: String) {
        messageTimeoutJobs[messageId]?.cancel()
        messageTimeoutJobs.remove(messageId)
    }

    /**
     * 모든 메시지 타임아웃 정리
     */
    override fun onCleared() {
        super.onCleared()
        messageTimeoutJobs.values.forEach { it.cancel() }
        messageTimeoutJobs.clear()

        // 방 퇴장 처리로 WebSocket 메시지 저장 범위를 정리
        viewModelScope.launch {
            try {
                webSocketUseCaseProvider.createForRoom(channelId).leaveRoomUseCase()
            } catch (e: Exception) {
                Log.w(TAG, "leaveRoom 실패 (무시 가능): ${e.message}")
            }
        }
    }

    // ================================
    // 🎯 ChatScreen에서 호출하는 UI 메서드들
    // ================================


    // ================================
    // 🎯 ChatScreen에서 호출하는 UI 메서드들
    // ================================

    /**
     * 이미지 선택 처리
     */
    fun onImagesSelected(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📸 이미지 선택됨: ${uris.size}개")

                // 선택 즉시 전송하지 않고, 임시 선택 목록에 추가
                _uiState.update { current ->
                    val merged =
                        current.selectedAttachmentUris.toMutableList().apply { addAll(uris) }
                    current.copy(
                        selectedAttachmentUris = merged,
                        isAttachmentAreaVisible = merged.isNotEmpty()
                    )
                }

                // UI 상태만 갱신하며 별도 이벤트 발행은 하지 않음
            } catch (e: Exception) {
                Log.e(TAG, "❌ 이미지 선택 처리 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("이미지 선택 처리에 실패했습니다: ${e.message}"))
            }
        }
    }

    /**
     * 뒤로가기 클릭 처리
     */
    fun onBackClick() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⬅️ 뒤로가기 클릭")
                services.navigationService.navigateBack()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 뒤로가기 처리 실패", e)
            }
        }
    }

    /**
     * 쓰기 권한 확인
     */
    fun canPerformWriteOperations(): Boolean {
        return true // TODO: 실제 권한 확인 로직 구현
    }

    /**
     * 메시지 입력 변경 처리
     */
    fun onMessageInputChange(text: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(pendingMessageText = text) }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 입력 변경 처리 실패", e)
            }
        }
    }

    /**
     * 메시지 수정 확인
     */
    fun confirmEditMessage() {
        Log.d(TAG, "confirmEditMessage")
        viewModelScope.launch {
            try {
                val messageId = uiState.value.editingMessageId
                val newContent = uiState.value.pendingMessageText

                if (messageId != null && newContent.isNotBlank()) {
                    editMessage(messageId, newContent)
                    _uiState.update {
                        it.copy(
                            isEditing = false,
                            editingMessageId = null,
                            pendingMessageText = ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 수정 확인 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 수정에 실패했습니다"))
            }
        }
    }

    fun onSendClick() {
        if (uiState.value.isEditing) {
            confirmEditMessage()
        } else {
            onSendMessageClick()
        }
    }
    
    /**
     * 메시지 전송 클릭 처리
     */
    fun onSendMessageClick() {
        viewModelScope.launch {
            try {
                val content = uiState.value.pendingMessageText.trim()
                val attachments = uiState.value.selectedAttachmentUris

                // 전송 가능 조건: 텍스트 있거나 첨부가 있거나
                if (content.isBlank() && attachments.isEmpty()) return@launch

                val senderId = AuthUtil.getCurrentUserId()

                val result = services.messageService.sendMessage(
                    senderId = UserId(senderId),
                    textContent = content,
                    imageUris = attachments,
                    replyToMessageId = null
                )

                when (result) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "✅ 메시지 전송 성공: ${result.data}")
                        _eventFlow.emit(ChatEvent.ScrollToBottom)
                        // 전송 성공 시 입력/첨부 초기화
                        _uiState.update {
                            it.copy(
                                pendingMessageText = "",
                                selectedAttachmentUris = emptyList(),
                                isAttachmentAreaVisible = false
                            )
                        }
                    }

                    is CustomResult.Failure -> {
                        Log.e(TAG, "❌ 메시지 전송 실패: ${result.error}")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 전송에 실패했습니다"))
                    }

                    else -> {
                        Log.w(TAG, "⚠️ 메시지 전송 결과: 알 수 없는 상태")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 전송 클릭 처리 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 전송에 실패했습니다"))
            }
        }
    }

    /**
     * 선택된 단일 첨부 제거
     */
    fun removeSelectedAttachment(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { current ->
                    val updated = current.selectedAttachmentUris.filterNot { it == uri }
                    current.copy(
                        selectedAttachmentUris = updated,
                        isAttachmentAreaVisible = updated.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 첨부 제거 실패", e)
            }
        }
    }

    /**
     * 선택된 모든 첨부 제거
     */
    fun clearSelectedAttachments() {
        viewModelScope.launch {
            try {
                _uiState.update { current ->
                    current.copy(
                        selectedAttachmentUris = emptyList(),
                        isAttachmentAreaVisible = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 첨부 전체 제거 실패", e)
            }
        }
    }

    /**
     * 첨부파일 클릭 처리
     */
    // onAttachmentClick 제거: ChatScreen에서 바로 이미지 피커 실행

    /**
     * 편집 취소
     */
    fun cancelEdit() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isEditing = false,
                        editingMessageId = null,
                        pendingMessageText = ""
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 편집 취소 처리 실패", e)
            }
        }
    }

    /**
     * 멘션 제안 클릭 처리
     */
    fun onMentionSuggestionClick(suggestion: MentionSuggestion) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👤 멘션 제안 클릭: ${suggestion.displayName}")
                // TODO: 멘션 처리 로직 구현
            } catch (e: Exception) {
                Log.e(TAG, "❌ 멘션 제안 클릭 처리 실패", e)
            }
        }
    }

    /**
     * 연결 재시도
     */
    fun retryConnection() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 연결 재시도")
                // TODO: WebSocket 재연결 로직 구현
            } catch (e: Exception) {
                Log.e(TAG, "❌ 연결 재시도 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("연결 재시도에 실패했습니다"))
            }
        }
    }

    /**
     * 메시지 길게 클릭 처리
     */
    fun onMessageLongClick(message: ChatMessageUiModel) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📱 메시지 길게 클릭: ${message.messageId}")
                _eventFlow.emit(ChatEvent.ShowEditDeleteDialog(message))
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 길게 클릭 처리 실패", e)
            }
        }
    }

    /**
     * 사용자 프로필 클릭 처리
     */
    fun onUserProfileClick(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👤 사용자 프로필 클릭: $userId")
                _eventFlow.emit(ChatEvent.ShowUserProfileDialog(userId))
            } catch (e: Exception) {
                Log.e(TAG, "❌ 사용자 프로필 클릭 처리 실패", e)
            }
        }
    }

    /**
     * 메시지 재전송
     */
    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 메시지 재전송: $messageId")

                val result = services.messageService.retryFailedMessage(messageId)

                when (result) {
                    is CustomResult.Success<*> -> {
                        Log.d(TAG, "✅ 메시지 재전송 성공: $messageId")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지를 다시 전송했습니다"))
                        _eventFlow.emit(ChatEvent.ScrollToBottom)
                    }

                    is CustomResult.Failure<*> -> {
                        Log.e(TAG, "❌ 메시지 재전송 실패: ${result.error}")
                        val errorMessage = when {
                            result.error.toString().contains("네트워크") == true -> "네트워크 연결을 확인해주세요"
                            result.error.toString().contains("이미지") == true -> "이미지 전송에 실패했습니다"
                            else -> "메시지 재전송에 실패했습니다"
                        }
                        _eventFlow.emit(ChatEvent.ShowSnackbar(errorMessage))
                    }

                    else -> {
                        Log.w(TAG, "⚠️ 메시지 재전송 알 수 없는 상태")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 재전송 중 문제가 발생했습니다"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 재전송 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 재전송에 실패했습니다"))
            }
        }
    }

    /**
     * 초기 메시지 ID 반환
     */
    fun getInitialMessageId(): String? {
        return initialMessageId
    }

    /**
     * 메시지 편집 시작
     */
    fun startEditMessage(message: ChatMessageUiModel) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "✏️ 메시지 편집 시작: ${message.messageId}")
                _uiState.update {
                    it.copy(
                        isEditing = true,
                        editingMessageId = message.messageId,
                        pendingMessageText = message.message
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 편집 시작 실패", e)
            }
        }
    }

    /**
     * 메시지 삭제 확인
     */
    fun confirmDeleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ 메시지 삭제 확인: $messageId")
                deleteMessage(messageId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 삭제 확인 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 삭제에 실패했습니다"))
            }
        }
    }

    /**
     * 멤버 초대 수락 처리
     */
    fun onAddMember(projectId: String, targetUserId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👥 멤버 초대 수락: projectId=$projectId, targetUserId=$targetUserId")

                acceptMemberInvitationUseCase(projectId = projectId, targetUserId = targetUserId)
                    .collect { result ->
                        when (result) {
                            is CustomResult.Success -> {
                                Log.d(TAG, "✅ 멤버 초대 수락 성공")
                                _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트에 성공적으로 참여했습니다!"))
                            }

                            is CustomResult.Failure -> {
                                Log.e(TAG, "❌ 멤버 초대 수락 실패", result.error)
                                _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트 참여에 실패했습니다: ${result.error.message}"))
                            }

                            is CustomResult.Loading -> {
                                Log.d(TAG, "⏳ 멤버 초대 수락 중...")
                            }

                            is CustomResult.Initial -> {
                                Log.d(TAG, "🔄 멤버 초대 수락 초기 상태")
                            }

                            is CustomResult.Progress -> {
                                Log.d(TAG, "📊 멤버 초대 수락 진행 중: ${result.progress}")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 멤버 초대 수락 중 예외 발생", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트 참여 중 오류가 발생했습니다"))
            }
        }
    }

    companion object {
        private const val TAG = "JEONJU_CHAT"
    }
}
