package com.example.feature_chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.MentionType
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MentionInfo
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.dm.DMUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectAuthorizationUseCaseProvider
import com.example.domain_usecase.usecase.sync.SyncUseCase
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.service.ChatServiceProvider
import com.example.feature_chat.ui.components.mention.MentionConstants
import com.example.feature_chat.util.moveCursorTo
import com.example.feature_chat.util.replaceTextAndMoveCursor
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.usecase.WebSocketUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val syncUseCase: SyncUseCase,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val projectAuthorizationUseCaseProvider: ProjectAuthorizationUseCaseProvider
) : ViewModel() {

    // 멘션 제안 최대 표시 개수 (기본 7, 필요 시 변경 가능)
    private var mentionSuggestionLimit: Int = 7

    fun setMentionSuggestionLimit(limit: Int) {
        mentionSuggestionLimit = limit.coerceAtLeast(1)
    }


    private val compositeChannelId: ChannelId =
        ChannelId(savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID))

    private val _channelIdAndProjectId: Pair<ChannelId, String?> = run {
        if (compositeChannelId.isProject()) {
            Pair(compositeChannelId, compositeChannelId.firstOrNull())
        } else {
            Pair(compositeChannelId, null)
        }
    }
    private val channelId: ChannelId = _channelIdAndProjectId.first
    private val projectId: String? = _channelIdAndProjectId.second
    private val initialMessageId: String? = savedStateHandle.get<String>("initialMessageId")

    // Services are initialized lazily once we determine the channel type
    private val services by lazy {
        if (projectId != null) {
            Log.d(TAG, "Creating services for project channel: $projectId/$channelId")
            chatServiceProvider.createForProjectChannel(projectId, channelId.value)
        } else {
            Log.d(TAG, "Creating services for DM channel: $channelId")
            chatServiceProvider.createForDMChannel(channelId.value)
        }
    }

    // Authorization use cases for project channels
    private val authUseCases by lazy {
        projectId?.let { pid ->
            projectAuthorizationUseCaseProvider.createForProject(DocumentId(pid))
        }
    }
    
    // Mention display gateway - maps user-visible @displayName to internal [type:id] format
    private var currentMentionMappings = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(
        ChatUiState(
            channelName = "채팅방",
            connectionState = WebSocketConnectionState.Disconnected,
            isProjectChannel = projectId != null
            // Note: messages, isLoadingHistory are now handled by Paging3
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentUserId: String? = null

    // 프로젝트 참여 상태 관리 (projectId -> isJoined)
    private val _projectMembershipStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val projectMembershipStates: StateFlow<Map<String, Boolean>> =
        _projectMembershipStates.asStateFlow()

    // 메시지 타임아웃 관리를 위한 Job 맵
    private val messageTimeoutJobs = mutableMapOf<String, Job>()

    // 🎯 MessageService를 통한 모든 페이징 기능 위임
    // UI 모델 변환까지 포함된 완전한 페이징 플로우
    val messagesFlow: Flow<PagingData<ChatMessageUiModel>> by lazy {
        Log.d(TAG, "🚀 messagesFlow lazy 초기화 시작 (initialMessageId=$initialMessageId)")
        services.messageService.getUiMessagesPagingFlow(initialMessageId).cachedIn(viewModelScope)
    }

    // OutBox 상태 맵(옵션): UI 컴포저블이 메시지별 로딩/실패 인디케이터를 바인딩할 때 사용할 수 있도록 노출
    val outBoxStatuses: Flow<Map<String, com.example.domain.enum.OutBoxStatus>> =
        services.messageService.observeChannelOutBoxStatuses()

    // 🎯 MessageService의 상태들을 UI에 전달
    val isAnchorJumpInProgress: StateFlow<Boolean> = services.messageService.isAnchorJumpInProgress
    val anchorTargetMessageId: StateFlow<String?> = services.messageService.anchorTargetMessageId

    init {
        // 1. 🎯 Pager initialKey 방식을 사용하므로 초기 Anchor 설정은 생략

        // 2. 채팅방 즉시 입장 (해당 방 이벤트만 수신/저장하도록 보장)
        viewModelScope.launch {
            try {
                // WebSocket 연결을 위해서는 composite channelId를 그대로 사용 (서버에서 파싱)
                val joinResult =
                    webSocketUseCaseProvider.createForRoom(channelId.value).joinRoomUseCase(
                    userId = AuthUtil.getCurrentUserId()?.let { UserId(it) }
                )
                if (joinResult.isSuccess) {
                    Log.d(TAG, "✅ 방 입장 성공: $channelId")

                    // 채널 입장 시 초기 동기화 실행 (SyncMetadata 기반) - 커서 기준 멱등
                    Log.d(TAG, "🚀 채널 입장시 초기 동기화 시작: $channelId")
                    try {
                        val result = syncUseCase.syncChannel(channelId.value)
                        if (result.isSuccess) {
                            Log.d(TAG, "✅ 초기 동기화 완료: $channelId")
                        } else {
                            Log.w(TAG, "⚠️ 초기 동기화 실패: $channelId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 초기 동기화 예외: $channelId", e)
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

        // Room DB는 자동 invalidation으로 Paging3가 자동 갱신됨

        // 5. 연결 상태 모니터링
        observeConnectionState()

        // 6. 멘션용 데이터 로드
        loadMentionData()

        // 6.1 OutBox 기반 채널 전송 진행 상태를 UI에 연결
        services.messageService.observeChannelPendingCount()
            .onEach { pendingCount ->
                _uiState.update { it.copy(isSendingMessage = pendingCount > 0) }
            }
            .launchIn(viewModelScope)

        // 권한(쓰기/초대) 선가드: DM 제외, 프로젝트 채널에서 OWNER→권한 확인 후 캐시
        if (projectId != null) {
            viewModelScope.launch {
                val canWrite = when (val res = authUseCases?.ownerOrPermissionUseCase?.invoke(
                    DocumentId(projectId),
                    com.example.domain.model.data.project.RolePermission.CHANNEL_WRITE
                )) {
                    is com.example.core_common.result.CustomResult.Success -> res.data
                    else -> false
                }
                val canInvite = when (val res = authUseCases?.ownerOrPermissionUseCase?.invoke(
                    DocumentId(projectId),
                    com.example.domain.model.data.project.RolePermission.MEMBER_INVITE
                )) {
                    is com.example.core_common.result.CustomResult.Success -> res.data
                    else -> false
                }
                _uiState.update { it.copy(canWrite = canWrite, canInvite = canInvite) }
            }
        } else {
            _uiState.update { it.copy(canWrite = !it.isDMBlocked, canInvite = true) }
        }

    }

    /**
     * UI 게이트 유틸: OWNER 선검사 결과가 캐시된 상태(canWrite/canInvite)를 사용.
     */
    private suspend fun checkAllowed(
        required: com.example.domain.model.data.project.RolePermission,
        onDenied: (() -> Unit)? = null
    ): Boolean {
        // DM 채널은 프로젝트 권한 미적용
        if (projectId == null) return true

        val allowed = when (required) {
            com.example.domain.model.data.project.RolePermission.CHANNEL_WRITE -> uiState.value.canWrite
            com.example.domain.model.data.project.RolePermission.MEMBER_INVITE -> uiState.value.canInvite
            else -> true
        }

        if (!allowed) {
            val msg = authUseCases?.permissionDeniedMessageUseCase?.invoke(required)
                ?: "권한이 없습니다: ${required.name}"
            _eventFlow.emit(ChatEvent.ShowSnackbar(msg))
            onDenied?.invoke()
        }
        return allowed
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

                // 권한 게이트: 캐시된 권한 사용
                if (!checkAllowed(com.example.domain.model.data.project.RolePermission.MEMBER_INVITE)) return@launch

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
                if (!checkAllowed(com.example.domain.model.data.project.RolePermission.CHANNEL_WRITE)) return@launch
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
                if (!checkAllowed(com.example.domain.model.data.project.RolePermission.CHANNEL_WRITE)) return@launch
                val result = services.messageService.deleteMessage(DocumentId(messageId))
                
                when (result) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "✅ 메시지 삭제 요청 전송(낙관적 적용됨)")
                    }

                    is CustomResult.Failure -> {
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
            webSocketUseCaseProvider.createForRoom(channelId.value).subscribeToRoomEventsUseCase()
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
                            // ACK 처리는 WebSocketMessageService에서 OutBox를 직접 업데이트한다.
                            // UI 측에서는 OutBox 관찰을 통해 자동으로 로딩 인디케이터가 해제된다.
                            // 필요 시 부드러운 UX를 위해 하단으로 스크롤만 트리거.
                            _eventFlow.emit(ChatEvent.ScrollToBottom)
                        }

                        is WebSocketDomainEvent.MessageFailed -> {
                            Log.e(
                                TAG,
                                "❌ 메시지 전송 실패: ${event.messageId}, type: ${event.failureType}, error: ${event.errorMessage}"
                            )
                            // 실패 처리는 WebSocketMessageService에서 OutBox를 직접 업데이트한다.
                            // UI는 OutBox 관찰을 통해 실패 상태를 반영한다.
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

    // ================================
    // 🎯 멘션 관련 기능 (UI 전용)
    // ================================

    /**
     * 멘션 제안 목록 생성
     */
    fun getMentionSuggestions(query: String): List<MentionSuggestion> {
        val suggestions = mutableListOf<MentionSuggestion>()

        // 프로젝트 채널인지 DM 채널인지에 따라 다른 멘션 대상 제공
        if (projectId != null) {
            // 프로젝트 채널: 프로젝트 멤버와 역할 모두 포함
            val currentState = uiState.value

            Log.d(TAG, "🔍 getMentionSuggestions - query: '$query'")
            Log.d(TAG, "🔍 현재 프로젝트ID: $projectId")
            Log.d(TAG, "🔍 프로젝트 멤버 수: ${currentState.projectMembers.size}")
            Log.d(TAG, "🔍 프로젝트 역할 수: ${currentState.projectRoles.size}")

            // 프로젝트 멤버 검색
            currentState.projectMembers
                .filter { member ->
                    query.isEmpty() || member.displayName.contains(query, ignoreCase = true)
                }
                .forEach { member ->
                    Log.d(TAG, "🔍 멤버 추가: ${member.displayName} (ID: ${member.userId})")
                    suggestions.add(
                        MentionSuggestion(
                            type = MentionType.USER,
                            id = member.userId,
                            displayName = member.displayName,
                            profileUrl = member.profileUrl,
                            subtitle = member.roleName ?: "멤버"
                        )
                    )
                }

            // 프로젝트 역할 검색
            Log.d(TAG, "🔍 역할 필터링 시작 - 전체 역할 목록:")
            currentState.projectRoles.forEach { role ->
                Log.d(TAG, "🔍 역할: ${role.roleName} (ID: ${role.roleId}, 멤버수: ${role.memberCount})")
            }

            val filteredRoles = currentState.projectRoles
                .filter { role ->
                    val matches =
                        query.isEmpty() || role.roleName.contains(query, ignoreCase = true)
                    Log.d(TAG, "🔍 역할 '${role.roleName}' 쿼리 매치: $matches (query: '$query')")
                    matches
                }

            Log.d(TAG, "🔍 필터링된 역할 수: ${filteredRoles.size}")

            filteredRoles.forEach { role ->
                Log.d(TAG, "🔍 역할 추가: ${role.roleName} (ID: ${role.roleId})")
                suggestions.add(
                    MentionSuggestion(
                        type = MentionType.ROLE,
                        id = role.roleId,
                        displayName = role.roleName,
                        profileUrl = null,
                        subtitle = "${role.memberCount}명의 멤버"
                    )
                )
            }
        } else {
            // DM 채널: 채팅 참여자만 포함
            val currentState = uiState.value
            currentState.participants
                .filter { participant ->
                    query.isEmpty() || participant.displayName.contains(query, ignoreCase = true)
                }
                .forEach { participant ->
                    suggestions.add(
                        MentionSuggestion(
                            type = MentionType.USER,
                            id = participant.userId,
                            displayName = participant.displayName,
                            profileUrl = participant.profileUrl,
                            subtitle = if (participant.isOnline) "온라인" else "오프라인"
                        )
                    )
                }
        }

        // 결과 정렬: 접두사(prefix) 매치 우선, 그 다음 사전순
        val (prefix, containsOnly) = suggestions.partition {
            it.displayName.startsWith(
                query,
                ignoreCase = true
            )
        }
        val ordered =
            (prefix.sortedBy { it.displayName.lowercase() } + containsOnly.sortedBy { it.displayName.lowercase() })

        // @everyone 특수 멘션 추가 (프로젝트 채널에서만)
        val reserved = if (projectId != null) listOf(
            MentionSuggestion(
                type = MentionType.EVERYONE,
                id = "everyone",
                displayName = "everyone",
                subtitle = "모든 멤버에게 알림"
            )
        ) else emptyList()

        val remainingSlots = (mentionSuggestionLimit - reserved.size).coerceAtLeast(0)
        val finalSuggestions = (reserved + ordered.take(remainingSlots))

        Log.d(TAG, "🔍 최종 멘션 제안 결과:")
        Log.d(TAG, "🔍 전체 suggestions 수: ${suggestions.size}")
        Log.d(TAG, "🔍 ordered 수: ${ordered.size}")
        Log.d(TAG, "🔍 reserved 수: ${reserved.size}")
        Log.d(TAG, "🔍 final 수: ${finalSuggestions.size}")
        finalSuggestions.forEach { suggestion ->
            Log.d(
                TAG,
                "🔍 최종 제안: ${suggestion.type} - ${suggestion.displayName} (ID: ${suggestion.id})"
            )
        }

        return finalSuggestions
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
                webSocketUseCaseProvider.createForRoom(channelId.value).leaveRoomUseCase()
            } catch (e: Exception) {
                Log.w(TAG, "leaveRoom 실패 (무시 가능): ${e.message}")
            }
        }
    }

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
        return if (projectId == null) {
            !uiState.value.isDMBlocked
        } else {
            uiState.value.canWrite
        }
    }

    /**
     * 멘션 텍스트가 수정되었는지 확인하고 해당 멘션을 무효화
     */
    private fun checkMentionTextModification(
        previousText: String,
        newText: String,
        cursorPosition: Int
    ): Boolean {
        val currentMentions = uiState.value.pendingMessageMentions
        if (currentMentions.isEmpty()) return false

        // 현재 텍스트에서 멘션 패턴 찾기 (점 포함)
        val mentionPattern = MentionConstants.MENTION_REGEX_PATTERN.toRegex()
        val currentMentionTexts = mentionPattern.findAll(newText).map {
            it.value to (it.range.first..it.range.last)
        }.toList()

        // 각 등록된 멘션이 현재 텍스트에서 유효한지 확인
        val invalidMentions = mutableListOf<MentionInfo>()

        currentMentions.forEach { mention ->
            val mentionText = "@${mention.displayName}"
            val matchingTextMention = currentMentionTexts.find { it.first == mentionText }

            if (matchingTextMention == null) {
                // 멘션 텍스트가 완전히 사라졌거나 변경됨
                invalidMentions.add(mention)
                Log.d(TAG, "🗑️ 멘션 텍스트 완전 변경/삭제 감지: $mentionText")
            } else {
                // 멘션 텍스트는 존재하지만 커서가 멘션 내부에 있고 텍스트가 변경된 경우
                val mentionRange = matchingTextMention.second
                val wasCursorInMention = cursorPosition in mentionRange
                val textChanged = previousText != newText

                if (wasCursorInMention && textChanged) {
                    // 멘션 내부에서 텍스트 변경이 발생한 경우 (예: @jo|hn -> @joXhn)
                    invalidMentions.add(mention)
                    Log.d(TAG, "🗑️ 멘션 내부 수정 감지: $mentionText (커서 위치: $cursorPosition)")
                }
            }
        }

        // 무효화된 멘션들 제거
        if (invalidMentions.isNotEmpty()) {
            val remainingMentions = currentMentions.filter { mention ->
                !invalidMentions.contains(mention)
            }

            _uiState.update { state ->
                state.copy(pendingMessageMentions = remainingMentions)
            }

            // 멘션 매핑에서도 제거
            invalidMentions.forEach { mention ->
                val mentionKey = "@${mention.displayName}"
                currentMentionMappings.remove(mentionKey)
            }

            Log.d(
                TAG,
                "✅ 멘션 텍스트 수정으로 ${invalidMentions.size}개 멘션 제거, ${remainingMentions.size}개 남음"
            )
            return true
        }

        return false
    }

    /**
     * 멘션 경계 보호: @username 바로 뒤에 문자가 추가되었는지 감지하고 해당 멘션 무효화
     */
    private fun detectMentionBoundaryViolation(
        previousText: String,
        newText: String,
        cursorPosition: Int
    ): Boolean {
        // 텍스트가 늘어난 경우만 확인 (문자 추가)
        if (newText.length <= previousText.length) return false

        val currentMentions = uiState.value.pendingMessageMentions
        if (currentMentions.isEmpty()) return false

        // 이전 텍스트에서 멘션 위치 찾기 (점 포함)
        val mentionPattern = MentionConstants.MENTION_REGEX_PATTERN.toRegex()
        val previousMentions = mentionPattern.findAll(previousText).toList()

        val violatedMentions = mutableListOf<MentionInfo>()

        previousMentions.forEach { previousMatch ->
            val mentionText = previousMatch.value
            val mentionEnd = previousMatch.range.last + 1

            // 해당 멘션이 등록된 멘션인지 확인
            val registeredMention = currentMentions.find { "@${it.displayName}" == mentionText }

            if (registeredMention != null) {
                // 멘션 바로 뒤에 문자가 추가되었는지 확인
                val wasAtMentionEnd = cursorPosition == mentionEnd + 1 // +1은 추가된 문자
                val charAddedAfterMention = newText.length > previousText.length &&
                        mentionEnd < newText.length &&
                        newText[mentionEnd] != ' ' // 공백이 아닌 문자가 추가됨

                if (wasAtMentionEnd && charAddedAfterMention) {
                    violatedMentions.add(registeredMention)
                    Log.d(TAG, "🗑️ 멘션 경계 침해 감지: $mentionText 뒤에 '${newText[mentionEnd]}' 추가됨")
                }
            }
        }

        // 경계 침해된 멘션들 제거
        if (violatedMentions.isNotEmpty()) {
            val remainingMentions = currentMentions.filter { mention ->
                !violatedMentions.contains(mention)
            }

            _uiState.update { state ->
                state.copy(pendingMessageMentions = remainingMentions)
            }

            // 멘션 매핑에서도 제거
            violatedMentions.forEach { mention ->
                val mentionKey = "@${mention.displayName}"
                currentMentionMappings.remove(mentionKey)
            }

            Log.d(TAG, "✅ 멘션 경계 침해로 ${violatedMentions.size}개 멘션 제거, ${remainingMentions.size}개 남음")
            return true
        }

        return false
    }

    /**
     * 백스페이스로 멘션이 제거되었는지 확인하고 처리 (강화된 버전)
     */
    private fun checkAndHandleMentionRemoval(previousText: String, newText: String): Boolean {
        // 텍스트가 단축된 경우만 확인 (백스페이스 등)
        if (newText.length >= previousText.length) return false

        val currentMentions = uiState.value.pendingMessageMentions
        if (currentMentions.isEmpty()) return false

        // @username 패턴으로 기존 텍스트에서 멘션 위치 찾기 (점 포함)
        val mentionPattern = MentionConstants.MENTION_REGEX_PATTERN.toRegex()
        val previousMentions = mentionPattern.findAll(previousText).map { it.value }.toSet()
        val newMentions = mentionPattern.findAll(newText).map { it.value }.toSet()

        // 사라진 멘션 텍스트들 찾기
        val removedMentionTexts = previousMentions - newMentions

        if (removedMentionTexts.isNotEmpty()) {
            Log.d(TAG, "🗑️ 백스페이스로 멘션 제거 감지: $removedMentionTexts")

            // 사라진 멘션 텍스트에 해당하는 등록된 멘션들 찾기
            val removedMentions = currentMentions.filter { mention ->
                val mentionText = "@${mention.displayName}"
                removedMentionTexts.contains(mentionText)
            }

            if (removedMentions.isNotEmpty()) {
                // 남은 멘션들만 유지
                val remainingMentions = currentMentions - removedMentions.toSet()

                // 상태 업데이트
                _uiState.update { state ->
                    state.copy(pendingMessageMentions = remainingMentions)
                }

                // 멘션 매핑에서도 제거
                removedMentions.forEach { mention ->
                    val mentionKey = "@${mention.displayName}"
                    currentMentionMappings.remove(mentionKey)
                }

                Log.d(TAG, "✅ 백스페이스로 ${removedMentions.size}개 멘션 제거, ${remainingMentions.size}개 남음")
                return true
            }
        }

        return false
    }

    /**
     * 메시지 입력 변경 처리
     */
    fun onMessageInputChange(newTextFieldValue: TextFieldValue) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val previousText = currentState.pendingMessageTextFieldValue.text
                val newText = newTextFieldValue.text
                val cursorPosition = newTextFieldValue.selection.end

                // 1. 멘션 텍스트 수정 감지 (우선순위 1)
                val wasMentionTextModified =
                    checkMentionTextModification(previousText, newText, cursorPosition)

                // 2. 멘션 경계 침해 감지 (우선순위 2)
                val wasMentionBoundaryViolated =
                    detectMentionBoundaryViolation(previousText, newText, cursorPosition)

                // 3. 백스페이스로 멘션 제거 감지 (우선순위 3)
                val wasBackspaceOverMention = checkAndHandleMentionRemoval(previousText, newText)

                // 4. 멘션 감지 로직 (실제 커서 위치 사용)
                val mentionDetectionResult = detectMentionInput(newText, cursorPosition)

                Log.d(
                    "WebSocketChatViewModel",
                    "멘션 처리 결과 - 텍스트수정:$wasMentionTextModified, 경계침해:$wasMentionBoundaryViolated, 백스페이스:$wasBackspaceOverMention, 새감지:${mentionDetectionResult.shouldShowSuggestions}, 커서:$cursorPosition"
                )
                _uiState.update { state ->
                    state.copy(
                        pendingMessageTextFieldValue = newTextFieldValue,
                        isMentionSuggestionVisible = mentionDetectionResult.shouldShowSuggestions,
                        mentionQueryText = mentionDetectionResult.query,
                        mentionQueryStartPosition = mentionDetectionResult.startPosition,
                        selectedMentionIndex = if (mentionDetectionResult.shouldShowSuggestions) 0 else -1, // 새 검색 시 첫 번째 항목 선택
                        mentionSuggestions = if (mentionDetectionResult.shouldShowSuggestions) {
                            getMentionSuggestions(mentionDetectionResult.query)
                        } else {
                            emptyList()
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 입력 변경 처리 실패", e)
            }
        }
    }

    /**
     * 텍스트에서 멘션 입력 감지
     */
    private fun detectMentionInput(text: String, cursorPosition: Int): MentionDetectionResult {

        // 커서 이전 텍스트에서 마지막 @ 문자 찾기
        val lastAtIndex = text.lastIndexOf('@', cursorPosition - 1)

        if (lastAtIndex == -1) {
            // @ 문자가 없으면 멘션 모드 아님
            return MentionDetectionResult(false, "", -1)
        }

        // 이전 글자 조건 제거: 어디서든 '@' 입력 시 제안 표시

        // @ 이후의 텍스트 추출
        val afterAtText = text.substring(lastAtIndex + 1, cursorPosition)

        // @ 이후에 공백이 있으면 멘션 모드 종료
        if (afterAtText.contains(' ')) {
            return MentionDetectionResult(false, "", -1)
        }

        // 멘션 쿼리가 너무 길면 제한
        if (afterAtText.length > 20) {
            return MentionDetectionResult(false, "", -1)
        }

        return MentionDetectionResult(true, afterAtText, lastAtIndex)
    }

    /**
     * 멘션 감지 결과
     */
    private data class MentionDetectionResult(
        val shouldShowSuggestions: Boolean,
        val query: String,
        val startPosition: Int
    )

    /**
     * 메시지 수정 확인
     */
    fun confirmEditMessage() {
        Log.d(TAG, "confirmEditMessage")
        viewModelScope.launch {
            try {
                val messageId = uiState.value.editingMessageId
                val newContent = uiState.value.pendingMessageTextFieldValue.text

                if (messageId != null && newContent.isNotBlank()) {
                    editMessage(messageId, newContent)
                    _uiState.update {
                        it.copy(
                            isEditing = false,
                            editingMessageId = null,
                            pendingMessageTextFieldValue = TextFieldValue("")
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
                val content = uiState.value.pendingMessageTextFieldValue.text.trim()
                val attachments = uiState.value.selectedAttachmentUris

                // 전송 가능 조건: 텍스트 있거나 첨부가 있거나
                if (content.isBlank() && attachments.isEmpty()) return@launch

                val senderId = AuthUtil.getCurrentUserId()

                // 프로젝트 채널이면 쓰기 권한 검사 (캐시)
                if (!checkAllowed(com.example.domain.model.data.project.RolePermission.CHANNEL_WRITE)) return@launch

                val result = services.messageService.sendMessage(
                    senderId = UserId(senderId),
                    textContent = content,
                    imageUris = attachments,
                    replyToMessageId = null,
                    mentions = uiState.value.pendingMessageMentions
                )

                when (result) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "✅ 메시지 전송 성공: ${result.data}")
                        _eventFlow.emit(ChatEvent.ScrollToBottom)
                        // 전송 성공 시 입력/첨부/멘션 초기화
                        _uiState.update {
                            it.copy(
                                pendingMessageTextFieldValue = TextFieldValue(""),
                                selectedAttachmentUris = emptyList(),
                                isAttachmentAreaVisible = false,
                                pendingMessageMentions = emptyList(),
                                isMentionSuggestionVisible = false,
                                mentionQueryText = "",
                                mentionQueryStartPosition = -1
                            )
                        }
                        // 멘션 매핑도 초기화
                        currentMentionMappings.clear()
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
                        pendingMessageTextFieldValue = TextFieldValue("")
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

                val currentState = uiState.value
                val currentTextFieldValue = currentState.pendingMessageTextFieldValue
                val currentText = currentTextFieldValue.text
                val queryStartPosition = currentState.mentionQueryStartPosition

                if (queryStartPosition >= 0 && queryStartPosition < currentText.length) {
                    // @ 부터 현재 쿼리까지를 선택한 멘션으로 교체
                    val beforeMention = currentText.substring(0, queryStartPosition)
                    val afterMention =
                        currentText.substring(queryStartPosition + 1 + currentState.mentionQueryText.length)

                    // 간단한 멘션 표시: @displayName 형태로 삽입 (강제로 뒤에 공백 추가)
                    val mentionText = "@${suggestion.displayName}"
                    val spaceAfterMention = " " // 무조건 공백 한 칸 추가
                    val newText = "$beforeMention$mentionText$spaceAfterMention$afterMention"

                    // 커서를 멘션 뒤 공백 다음으로 위치 계산
                    val newCursorPosition =
                        beforeMention.length + mentionText.length + spaceAfterMention.length

                    // TextFieldValue로 새로운 상태 생성
                    val newTextFieldValue = TextFieldValue(
                        text = newText,
                        selection = TextRange(newCursorPosition.coerceIn(0, newText.length))
                    )

                    // 현재 메시지의 멘션 리스트에 추가
                    val mentionInfo = MentionInfo.create(
                        type = suggestion.type,
                        id = suggestion.id,
                        displayName = suggestion.displayName
                    )
                    addMentionToPendingMessage(mentionInfo)

                    // 상태 업데이트
                    _uiState.update { state ->
                        state.copy(
                            pendingMessageTextFieldValue = newTextFieldValue,
                            isMentionSuggestionVisible = false,
                            mentionQueryText = "",
                            mentionQueryStartPosition = -1,
                            selectedMentionIndex = -1,
                            mentionSuggestions = emptyList()
                        )
                    }

                    // 멘션 매핑 업데이트 (인덱스 기반)
                    val mentionIndex = uiState.value.pendingMessageMentions.size - 1
                    currentMentionMappings["@${suggestion.displayName}"] =
                        "$mentionIndex:${suggestion.type.name}:${suggestion.id}"

                    Log.d(TAG, "✅ 멘션 추가 완료: $mentionText (인덱스: $mentionIndex)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 멘션 제안 클릭 처리 실패", e)
            }
        }
    }

    /**
     * 키보드 화살표 키로 이전 멘션 제안 선택
     */
    fun selectPreviousMention() {
        val currentState = uiState.value
        if (!currentState.isMentionSuggestionVisible || currentState.mentionSuggestions.isEmpty()) {
            return
        }

        val newIndex = if (currentState.selectedMentionIndex <= 0) {
            currentState.mentionSuggestions.size - 1  // 처음에서 위로 가면 마지막으로
        } else {
            currentState.selectedMentionIndex - 1
        }

        _uiState.update { state ->
            state.copy(selectedMentionIndex = newIndex)
        }
        Log.d(TAG, "🔝 이전 멘션 선택: $newIndex")
    }

    /**
     * 키보드 화살표 키로 다음 멘션 제안 선택
     */
    fun selectNextMention() {
        val currentState = uiState.value
        if (!currentState.isMentionSuggestionVisible || currentState.mentionSuggestions.isEmpty()) {
            return
        }

        val newIndex =
            if (currentState.selectedMentionIndex >= currentState.mentionSuggestions.size - 1) {
                0  // 마지막에서 아래로 가면 처음으로
            } else {
                currentState.selectedMentionIndex + 1
            }

        _uiState.update { state ->
            state.copy(selectedMentionIndex = newIndex)
        }
        Log.d(TAG, "🔽 다음 멘션 선택: $newIndex")
    }

    /**
     * 키보드 Enter 키로 현재 선택된 멘션 제안 적용
     */
    fun selectCurrentMention() {
        val currentState = uiState.value
        if (!currentState.isMentionSuggestionVisible || currentState.mentionSuggestions.isEmpty()) {
            return
        }

        val selectedIndex = currentState.selectedMentionIndex
        if (selectedIndex >= 0 && selectedIndex < currentState.mentionSuggestions.size) {
            val selectedSuggestion = currentState.mentionSuggestions[selectedIndex]
            Log.d(TAG, "⌨️ 키보드로 멘션 선택: ${selectedSuggestion.displayName}")
            onMentionSuggestionClick(selectedSuggestion)
        }
    }

    /**
     * 현재 메시지의 멘션 리스트 반환
     */
    private fun getCurrentMessageMentions(): List<MentionInfo> {
        return uiState.value.pendingMessageMentions
    }

    /**
     * 현재 메시지에 멘션 추가
     */
    private fun addMentionToPendingMessage(mentionInfo: MentionInfo) {
        _uiState.update { state ->
            val currentMentions = state.pendingMessageMentions.toMutableList()
            currentMentions.add(mentionInfo)
            state.copy(pendingMessageMentions = currentMentions)
        }
    }

    /**
     * 현재 메시지의 멘션 리스트 초기화
     */
    private fun clearPendingMessageMentions() {
        _uiState.update { state ->
            state.copy(pendingMessageMentions = emptyList())
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
                        pendingMessageTextFieldValue = TextFieldValue(message.message)
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
     * DM 채널 차단
     */
    fun blockDMChannel() {
        if (projectId != null) return // 프로젝트 채널에서는 차단 불가

        viewModelScope.launch {
            try {
                val currentUserId = currentUserId ?: return@launch
                val dmUseCases = dmUseCaseProvider.createForUser(UserId.from(currentUserId))
                dmUseCases.blockDMChannelUseCase(DocumentId.from(channelId)).collect { result ->
                    when (result) {
                        is CustomResult.Loading -> {
                            // 로딩 상태는 UI에서 처리하지 않음 (빠른 응답 예상)
                        }

                        is CustomResult.Success -> {
                            _uiState.update { it.copy(isDMBlocked = true) }
                            _eventFlow.emit(ChatEvent.ShowSnackbar("사용자를 차단했습니다"))
                        }

                        is CustomResult.Failure -> {
                            _eventFlow.emit(ChatEvent.ShowSnackbar("차단에 실패했습니다: ${result.error.message}"))
                        }

                        else -> {
                            // Do nothing for other states
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "DM 채널 차단 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("차단에 실패했습니다"))
            }
        }
    }

    /**
     * DM 채널 차단 해제
     */
    fun unblockDMChannel() {
        if (projectId != null) return // 프로젝트 채널에서는 차단 해제 불가

        viewModelScope.launch {
            try {
                val currentUserId = currentUserId ?: return@launch
                val dmUseCases = dmUseCaseProvider.createForUser(UserId.from(currentUserId))
                dmUseCases.unblockDMChannelUseCase(DocumentId.from(channelId)).collect { result ->
                    when (result) {
                        is CustomResult.Loading -> {
                            // 로딩 상태는 UI에서 처리하지 않음 (빠른 응답 예상)
                        }

                        is CustomResult.Success -> {
                            _uiState.update { it.copy(isDMBlocked = false) }
                            _eventFlow.emit(ChatEvent.ShowSnackbar("차단을 해제했습니다"))
                        }

                        is CustomResult.Failure -> {
                            _eventFlow.emit(ChatEvent.ShowSnackbar("차단 해제에 실패했습니다: ${result.error.message}"))
                        }

                        else -> {
                            // Do nothing for other states
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "DM 채널 차단 해제 실패", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("차단 해제에 실패했습니다"))
            }
        }
    }

    /**
     * 멤버 초대 수락 처리 (projectId 기반 Functions 호출)
     */
    fun onAddMember(projectId: String, targetUserId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👥 멤버 초대 수락: projectId=$projectId (target=$targetUserId)")
                val memberUseCases =
                    projectMemberUseCaseProvider.createForProject(DocumentId(projectId))
                when (val result = memberUseCases.joinProjectByIdUseCase(projectId)) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "✅ 멤버 초대 수락 성공")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트에 성공적으로 참여했습니다!"))
                    }

                    is CustomResult.Failure -> {
                        Log.e(TAG, "❌ 멤버 초대 수락 실패", result.error)
                        val msg = result.error.message ?: "프로젝트 참여에 실패했습니다. 잠시 후 다시 시도해 주세요"
                        _eventFlow.emit(ChatEvent.ShowSnackbar(msg))
                    }

                    is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> {
                        Log.d(TAG, "⏳ 멤버 초대 수락 처리 중...")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 멤버 초대 수락 중 예외 발생", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트 참여 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 프로젝트 초대 메시지(프로젝트 참여) 수락 처리
     */
    fun onJoinProject(projectId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👥 프로젝트 참여 요청: projectId=$projectId")
                val memberUseCases =
                    projectMemberUseCaseProvider.createForProject(DocumentId(projectId))
                val result = memberUseCases.joinProjectByIdUseCase(projectId)
                when (result) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "✅ 프로젝트 참여 성공")
                        // 로컬 상태 업데이트: 해당 프로젝트에 참여했음을 표시
                        updateProjectMembershipState(projectId, true)
                        _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                    }

                    is CustomResult.Failure -> {
                        Log.e(TAG, "❌ 프로젝트 참여 실패", result.error)
                        _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트 참여 실패: ${result.error.message}"))
                    }

                    else -> {
                        Log.w(TAG, "⚠️ 프로젝트 참여 처리 알 수 없는 상태")
                        _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트 참여 중 문제가 발생했습니다"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 프로젝트 참여 처리 중 예외", e)
                _eventFlow.emit(ChatEvent.ShowSnackbar("프로젝트 참여 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * 프로젝트 멤버십 상태를 업데이트합니다.
     */
    private fun updateProjectMembershipState(projectId: String, isJoined: Boolean) {
        _projectMembershipStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                put(projectId, isJoined)
            }
        }
        Log.d(TAG, "📝 프로젝트 멤버십 상태 업데이트: $projectId -> $isJoined")
    }

    /**
     * 특정 프로젝트의 멤버십 상태를 확인합니다.
     */
    fun checkProjectMembership(projectId: String) {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                val memberUseCases =
                    projectMemberUseCaseProvider.createForProject(DocumentId(projectId))
                val isJoined = memberUseCases.checkUserProjectMembershipUseCase(projectId, userId)
                updateProjectMembershipState(projectId, isJoined)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 프로젝트 멤버십 확인 실패", e)
                // 실패 시 안전하게 false로 설정
                updateProjectMembershipState(projectId, false)
            }
        }
    }

    /**
     * 프로젝트 멤버십 상태를 반환합니다.
     */
    fun isProjectMember(projectId: String): Boolean {
        return _projectMembershipStates.value[projectId] ?: false
    }

    /**
     * 멘션용 데이터 로드 (참가자, 프로젝트 멤버, 역할)
     */
    private fun loadMentionData() {
        viewModelScope.launch {
            try {
                if (projectId != null) {
                    // 프로젝트 채널: 프로젝트 멤버와 역할 로드
                    loadProjectMentionData(projectId)
                } else {
                    // DM 채널: 참가자 로드
                    loadDMMentionData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 멘션 데이터 로드 실패", e)
            }
        }
    }

    /**
     * 프로젝트 채널의 멘션 데이터 로드
     */
    private suspend fun loadProjectMentionData(projectId: String) {
        try {
            val memberService = services.memberService
            val roleService = services.roleService

            // 실제 프로젝트 멤버 로드 (UserProfileService로 이름/이미지 포함)
            val projectMembers = memberService?.loadMembers() ?: emptyList()

            // 실제 프로젝트 역할 로드 (GetProjectRolesUseCase 활용, +@everyone 추가)
            val loadedRoles = roleService?.loadRoles() ?: emptyList()

            // 역할별 멤버 수 계산 및 업데이트
            val projectRoles = if (loadedRoles.isNotEmpty()) {
                roleService?.updateRoleMemberCounts(loadedRoles) ?: loadedRoles
            } else {
                loadedRoles
            }

            _uiState.update { state ->
                state.copy(
                    projectMembers = projectMembers,
                    projectRoles = projectRoles,
                    isLoadingProjectData = false
                )
            }

            // 멘션 입력 중이면 최신 데이터로 제안 재생성
            val current = uiState.value
            if (current.isMentionSuggestionVisible) {
                _uiState.update { it.copy(mentionSuggestions = getMentionSuggestions(current.mentionQueryText)) }
            }

            Log.d(TAG, "✅ 프로젝트 멘션 데이터 로드 완료: 멤버 ${projectMembers.size}명, 역할 ${projectRoles.size}개")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 프로젝트 멘션 데이터 로드 예외", e)
            _uiState.update { state ->
                state.copy(
                    projectMembers = emptyList(),
                    projectRoles = emptyList(),
                    isLoadingProjectData = false
                )
            }
        }
    }

    /**
     * DM 채널의 멘션 데이터 로드
     */
    private suspend fun loadDMMentionData() {
        try {
            val participantService = services.participantService
            val participants = participantService?.loadParticipants() ?: emptyList()

            _uiState.update { state ->
                state.copy(
                    participants = participants,
                    isLoadingParticipants = false
                )
            }

            // 멘션 입력 중이면 최신 데이터로 제안 재생성
            val current = uiState.value
            if (current.isMentionSuggestionVisible) {
                _uiState.update { it.copy(mentionSuggestions = getMentionSuggestions(current.mentionQueryText)) }
            }

            Log.d(TAG, "✅ DM 멘션 데이터 로드 완료: 참가자 ${participants.size}명")
        } catch (e: Exception) {
            Log.e(TAG, "❌ DM 멘션 데이터 로드 예외", e)
            _uiState.update { state ->
                state.copy(
                    participants = emptyList(),
                    isLoadingParticipants = false
                )
            }
        }
    }
    

    companion object {
        private const val TAG = "JEONJU_CHAT"
    }
}
