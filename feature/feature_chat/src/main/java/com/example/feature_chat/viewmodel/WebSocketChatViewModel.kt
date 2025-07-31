package com.example.feature_chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.MentionType
import com.example.domain.model.vo.message.MentionInfo
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.service.ChatServiceProvider
import com.example.feature_chat.websocket.ChatWebSocketEvent
import com.example.websocket.WebSocketConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Refactored WebSocketChatViewModel using Service Provider pattern
 * Reduced from 1000+ lines to ~450 lines by delegating logic to Services
 */
@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatServiceProvider: ChatServiceProvider,
) : ViewModel() {

    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val projectId: String? = savedStateHandle.get<String>(RouteArgs.PROJECT_ID)

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
            isLoadingHistory = true,
            connectionState = WebSocketConnectionState.Disconnected
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentUserId: String? = null

    // 메시지 타임아웃 관리를 위한 Job 맵
    private val messageTimeoutJobs = mutableMapOf<String, Job>()

    init {
        // Log local cache on entry
        logChannelCacheOnEntry()

        initializeAuthentication()
        initializeConnection()
        initializeMessages()
        loadChannelData()
    }

    private fun logChannelCacheOnEntry() {
        viewModelScope.launch {
            Log.i("Debug", "--- Initializing chat for channel $channelId ---")
        }
    }

    private fun initializeAuthentication() {
        viewModelScope.launch {
            services.authenticationService.getCurrentUserSessionStream().collectLatest { result ->
                when (result) {
                    is com.example.core_common.result.CustomResult.Success -> {
                        val authState = result.data
                        currentUserId = authState.currentUserId
                        Log.d("ViewModel", "Current user authenticated: ${currentUserId}")
                        
                        _uiState.update { state ->
                            state.copy(
                                currentUserId = currentUserId,
                                myUserId = currentUserId ?: ""
                            )
                        }
                        
                        // Join room once authenticated
                        currentUserId?.let { userId ->
                            val joinResult = services.authenticationService.joinChatRoom(userId)
                            if (joinResult.isFailure) {
                                _eventFlow.emit(ChatEvent.Error("채팅방 입장 실패"))
                            } else {
                                // Load initial messages after successful authentication and room join
                                loadInitialMessages()
                            }
                        }
                    }
                    is com.example.core_common.result.CustomResult.Failure -> {
                        Log.e("ViewModel", "Authentication failed", result.error)
                        _eventFlow.emit(ChatEvent.Error("인증 실패: ${result.error.message}"))
                    }
                    else -> {
                        Log.d("ViewModel", "Authentication loading...")
                    }
                }
            }
        }
    }

    private fun initializeConnection() {
        viewModelScope.launch {
            services.connectionService.getConnectionStateStream().collect { connectionInfo ->
                _uiState.update { state ->
                    state.copy(
                        connectionState = connectionInfo.state,
                        queuedMessagesCount = connectionInfo.queuedMessagesCount,
                        showConnectionError = connectionInfo.showConnectionError
                    )
                }
                
                when (connectionInfo.state) {
                    is WebSocketConnectionState.Error -> {
                        _eventFlow.emit(ChatEvent.Error("연결 오류: ${connectionInfo.state.message}"))
                    }
                    else -> { /* Handle other states if needed */ }
                }
            }
        }
    }

    private fun initializeMessages() {
        viewModelScope.launch {
            // 병렬 실행: 웹소켓 연결과 과거 메시지 로딩을 동시에 시작
            launch { 
                Log.d("ViewModel", "Starting WebSocket message observation")
                observeWebSocketMessages() 
            }
            launch { 
                Log.d("ViewModel", "Starting initial message loading")
                loadInitialMessages() 
            }
        }
    }

    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            val roomId = channelId  // 접두사 제거 - 단순히 channelId만 사용
            services.connectionService.getChatMessageEvents(roomId).collect { event ->
                when (event) {
                    is ChatWebSocketEvent.MessageReceived -> {
                        handleNewMessage(event)
                    }
                    is ChatWebSocketEvent.MessageEdited -> {
                        handleMessageEdit(event)
                    }
                    is ChatWebSocketEvent.MessageDeleted -> {
                        handleMessageDelete(event)
                    }
                    is ChatWebSocketEvent.SystemMessage -> {
                        _eventFlow.emit(ChatEvent.SystemMessage(event.content))
                    }
                    is ChatWebSocketEvent.Error -> {
                        _eventFlow.emit(ChatEvent.Error(event.message))
                    }
                    is ChatWebSocketEvent.MessageAck -> {
                        handleMessageAck(event)
                    }
                    is ChatWebSocketEvent.MessageFailed -> {
                        handleMessageFailed(event)
                    }
                    else -> { /* Handle other events */ }
                }
            }
        }
    }

    private suspend fun handleNewMessage(event: ChatWebSocketEvent.MessageReceived) {
        currentUserId?.let { userId ->
            Log.d("ViewModel", "Processing WebSocket message: ${event.messageId} from ${event.senderId}")
            
            // 참여 방식: 모든 메시지를 서버에서 받아서 처리
            // 내 메시지든 다른 사람 메시지든 동일하게 처리
            val result = services.messageService.handleNewMessage(event, userId)
            
            _uiState.update { state ->
                // 내가 보낸 메시지인 경우 임시 메시지 제거
                val filteredMessages = if (event.senderId == userId) {
                    // 타임아웃 Job 취소 (실제 메시지 도착했으므로)
                    cancelMessageTimeout(event.messageId)

                    // 향상된 중복 메시지 제거: messageId 우선, 내용 기반 백업
                    result.messages.filterNot { message ->
                        message.userId == userId && message.isOptimistic && (
                                // 1순위: messageId 정확한 매칭
                                message.messageId == event.messageId ||
                                        // 2순위: 내용 기반 매칭 (백업)
                                        message.message.trim() == event.content.trim()
                                )
                    }
                } else {
                    result.messages
                }
                
                state.copy(
                    messages = filteredMessages.applyDisplayFormatConversion(),
                    hasMoreMessages = result.hasMoreOlderMessages,
                    error = result.error
                )
            }
        }
    }

    private suspend fun handleMessageEdit(event: ChatWebSocketEvent.MessageEdited) {
        val result = services.messageService.handleMessageEdit(event)
        
        _uiState.update { state ->
            state.copy(
                messages = result.messages.applyDisplayFormatConversion(),
                hasMoreMessages = result.hasMoreOlderMessages,
                error = result.error
            )
        }
    }

    private suspend fun handleMessageDelete(event: ChatWebSocketEvent.MessageDeleted) {
        val result = services.messageService.handleMessageDelete(event)
        
        _uiState.update { state ->
            state.copy(
                messages = result.messages.applyDisplayFormatConversion(),
                hasMoreMessages = result.hasMoreOlderMessages,
                error = result.error
            )
        }
    }

    private fun handleMessageAck(event: ChatWebSocketEvent.MessageAck) {
        Log.i("ViewModel", "Message ACK received: ${event.messageId} (${event.ackType})")

        // 타임아웃 Job 취소
        cancelMessageTimeout(event.messageId)
        
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.messageId == event.messageId && message.isOptimistic) {
                    Log.d("ViewModel", "Marking message as successfully sent: ${event.messageId}")
                    message.copy(
                        isSending = false,
                        sendFailed = false,
                        isOptimistic = false
                    )
                } else {
                    message
                }
            }
            state.copy(messages = updatedMessages.applyDisplayFormatConversion())
        }
    }

    private fun handleMessageFailed(event: ChatWebSocketEvent.MessageFailed) {
        Log.e("ViewModel", "Message failed: ${event.messageId} (${event.failureType})")

        // 타임아웃 Job 취소
        cancelMessageTimeout(event.messageId)
        
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.messageId == event.messageId && message.isOptimistic) {
                    Log.d("ViewModel", "Marking message as failed: ${event.messageId}")
                    message.copy(
                        isSending = false,
                        sendFailed = true,
                        canRetry = true,
                        deliveryState = com.example.feature_chat.model.MessageDeliveryState.Failed("전송 실패"),
                        errorMessage = "메시지 전송에 실패했습니다",
                        formattedTimestamp = "전송 실패"
                    )
                } else {
                    message
                }
            }
            state.copy(messages = updatedMessages.applyDisplayFormatConversion())
        }
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            
            val userId = currentUserId
            if (userId == null) {
                Log.w("ViewModel", "Cannot load initial messages: user not authenticated")
                _uiState.update { 
                    it.copy(
                        isLoadingHistory = false,
                        error = "사용자 인증이 필요합니다"
                    ) 
                }
                return@launch
            }
            
            val result = services.messageService.loadInitialMessages(userId)
            
            _uiState.update { state ->
                state.copy(
                    messages = result.messages.applyDisplayFormatConversion(),
                    isLoadingHistory = false,
                    hasMoreMessages = result.hasMoreOlderMessages,
                    lastMessageTimestamp = result.lastMessageTimestamp,
                    error = result.error
                )
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
            val result = services.messageService.sendMessage(
                text,
                attachmentUris,
                senderId,
                replyToMessageId
            )
            
            if (result.success && result.tempMessage != null) {
                // 낙관적 UI 업데이트: 전송 중 상태로 메시지 즉시 표시
                _uiState.update { state ->
                    val tempMessageWithDisplayFormat = result.tempMessage.copy(
                        message = convertInternalToDisplayFormat(result.tempMessage.message),
                        isSending = true,
                        sendFailed = false,
                        deliveryState = com.example.feature_chat.model.MessageDeliveryState.Sending
                    )
                    state.copy(messages = listOf(tempMessageWithDisplayFormat) + state.messages)
                }
                
                // ACK 기반 메시지 상태 관리: 서버에서 ACK/FAILED 메시지로 상태 업데이트
                // 30초 타임아웃 백업 시스템 - ACK가 도착하지 않을 경우 대비
                startMessageTimeout(result.tempMessage.messageId)

                // 실제 메시지가 있는 경우 즉시 성공 상태로 변경 (Firestore 경로)
                result.actualMessage?.let { actualMessage ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map {
                            if (it.messageId == result.tempMessage.messageId) {
                                Log.d(
                                    "ViewModel",
                                    "Message sent successfully via Firestore: ${it.messageId}"
                                )
                                actualMessage.copy(
                                    isOptimistic = false,
                                    isSending = false,
                                    sendFailed = false,
                                    deliveryState = com.example.feature_chat.model.MessageDeliveryState.Sent,
                                    clientSentAt = result.tempMessage.clientSentAt, // 원래 전송 시간 유지
                                    formattedTimestamp = DateTimeUtil.formatChatTime(actualMessage.actualTimestamp)
                                )
                            } else {
                                it
                            }
                        }
                        state.copy(messages = updatedMessages.applyDisplayFormatConversion())
                    }
                }
            } else if (!result.success) {
                result.error?.let { error ->
                    _eventFlow.emit(ChatEvent.Error(error))
                }
                
                // Mark temp message as failed instead of removing it
                result.tempMessage?.let { tempMessage ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map {
                            if (it.messageId == tempMessage.messageId) {
                                Log.d(
                                    "ViewModel",
                                    "Marking temp message as failed: ${it.messageId}"
                                )
                                it.copy(
                                    isSending = false,
                                    sendFailed = true,
                                    canRetry = true,
                                    deliveryState = com.example.feature_chat.model.MessageDeliveryState.Failed(
                                        "전송 실패"
                                    ),
                                    errorMessage = result.error ?: "메시지 전송에 실패했습니다",
                                    formattedTimestamp = "전송 실패"
                                )
                            } else {
                                it
                            }
                        }
                        state.copy(messages = updatedMessages.applyDisplayFormatConversion())
                    }
                }
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
            val result = services.messageService.editMessage(messageId, newContent)
            if (result.isFailure) {
                _eventFlow.emit(ChatEvent.Error(result.exceptionOrNull()?.message ?: "메시지 수정 실패"))
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
            val result = services.messageService.deleteMessage(messageId)
            if (result.isFailure) {
                _eventFlow.emit(ChatEvent.Error(result.exceptionOrNull()?.message ?: "메시지 삭제 실패"))
            }
        }
    }

    fun loadMoreMessages() {
        if (_uiState.value.isLoadingMoreMessages || !_uiState.value.hasMoreMessages) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreMessages = true) }
            
            currentUserId?.let { userId ->
                val result = services.messageService.loadOlderMessages(userId)
                
                _uiState.update { state ->
                    state.copy(
                        messages = result.messages.applyDisplayFormatConversion(),
                        isLoadingMoreMessages = false,
                        hasMoreMessages = result.hasMoreOlderMessages,
                        lastMessageTimestamp = result.lastMessageTimestamp,
                        error = result.error
                    )
                }
                
                // 메모리 제한으로 인해 제거된 메시지가 있다면 사용자에게 알림
                if (result.removedMessages.isNotEmpty()) {
                    _eventFlow.emit(ChatEvent.SystemMessage("최신 메시지 ${result.removedMessages.size}개가 메모리에서 제거되었습니다."))
                }
            }
        }
    }
    
    private fun loadChannelData() {
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
            
            try {
                val participantService = services.participantService
                if (participantService != null) {
                    Log.d("ViewModel", "Loading DM participants using ParticipantService")
                    val participants = participantService.loadParticipants()
                    
                    _uiState.update { 
                        it.copy(
                            participants = participants,
                            isLoadingParticipants = false
                        )
                    }
                    
                    Log.d("ViewModel", "Successfully loaded ${participants.size} DM participants")
                } else {
                    Log.e("ViewModel", "ParticipantService is null for DM channel")
                    _uiState.update { it.copy(isLoadingParticipants = false) }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load DM participants", e)
                _uiState.update { 
                    it.copy(
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
            
            try {
                val memberService = services.memberService
                val roleService = services.roleService
                
                if (memberService != null && roleService != null) {
                    Log.d("ViewModel", "Loading project members and roles using MemberService and RoleService")
                    
                    // Load members and roles concurrently
                    val membersDeferred = async { memberService.loadMembers() }
                    val rolesDeferred = async { roleService.loadRoles() }
                    
                    val projectMembers = membersDeferred.await()
                    val projectRoles = rolesDeferred.await()
                    
                    // Update role member counts with total member count
                    val updatedRoles = roleService.updateRoleMemberCounts(projectRoles, projectMembers.size)
                    
                    _uiState.update { 
                        it.copy(
                            projectMembers = projectMembers,
                            projectRoles = updatedRoles,
                            isLoadingProjectData = false
                        )
                    }
                    
                    Log.d("ViewModel", "Successfully loaded ${projectMembers.size} members and ${updatedRoles.size} roles")
                } else {
                    Log.e("ViewModel", "MemberService or RoleService is null for project channel")
                    _uiState.update { it.copy(isLoadingProjectData = false) }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load project data", e)
                _uiState.update { 
                    it.copy(
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
     * Converts internal mention format ([TYPE:id]) to display format (@displayName) for UI display
     * This prevents internal format from being visible to users
     */
    fun convertInternalToDisplayFormat(text: String): String {
        val mentionRegex = """\[(USER|ROLE):(\S+?)\]""".toRegex()
        
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
            services.connectionService.retryConnection()
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
            is WebSocketConnectionState.Disconnected -> {
                if (queuedCount > 0) {
                    "오프라인 (${queuedCount}개 대기중)"
                } else {
                    "오프라인 (읽기 전용)"
                }
            }
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
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.messageId == messageId && message.isOptimistic && message.isSending) {
                    Log.d("ViewModel", "Timeout: Marking message as sent (optimistic): $messageId")
                    message.copy(
                        isSending = false,
                        sendFailed = false,
                        isOptimistic = false, // 더 이상 임시 메시지가 아님
                        formattedTimestamp = DateTimeUtil.formatChatTime(message.actualTimestamp)
                    )
                } else {
                    message
                }
            }
            state.copy(messages = updatedMessages.applyDisplayFormatConversion())
        }

        // 타임아웃 Job 정리
        messageTimeoutJobs.remove(messageId)
    }

    /**
     * 실패한 메시지를 재전송합니다.
     */
    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val failedMessage = currentState.messages.find {
                it.messageId == messageId && it.sendFailed
            }

            if (failedMessage == null) {
                Log.w("ViewModel", "Failed message not found for retry: $messageId")
                return@launch
            }

            // 재전송 횟수 체크 (최대 3회)
            if (failedMessage.retryCount >= 3) {
                _eventFlow.emit(ChatEvent.Error("최대 재전송 횟수를 초과했습니다"))
                return@launch
            }

            Log.d(
                "ViewModel",
                "Retrying message: $messageId (attempt ${failedMessage.retryCount + 1})"
            )

            // 메시지 상태를 재전송 대기로 변경
            _uiState.update { state ->
                val updatedMessages = state.messages.map { message ->
                    if (message.messageId == messageId) {
                        message.copy(
                            deliveryState = com.example.feature_chat.model.MessageDeliveryState.Retry,
                            sendFailed = false,
                            retryCount = message.retryCount + 1,
                            canRetry = false
                        )
                    } else {
                        message
                    }
                }
                state.copy(messages = updatedMessages.applyDisplayFormatConversion())
            }

            delay(1000) // 1초 대기 후 재전송

            // 다시 전송 시도
            try {
                val result = services.messageService.retryMessage(
                    messageId = messageId,
                    content = failedMessage.message,
                    senderId = currentUserId ?: return@launch,
                    roomId = channelId,  // 단순히 channelId만 사용
                    attachmentUris = emptyList(), // 현재는 첨부파일 재전송 미지원
                    replyToMessageId = failedMessage.replyToMessageId
                )

                if (result.success) {
                    // 성공 시 전송 중 상태로 변경
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { message ->
                            if (message.messageId == messageId) {
                                message.copy(
                                    deliveryState = com.example.feature_chat.model.MessageDeliveryState.Sending,
                                    isSending = true
                                )
                            } else {
                                message
                            }
                        }
                        state.copy(messages = updatedMessages.applyDisplayFormatConversion())
                    }

                    // 재전송 타임아웃 시작
                    startMessageTimeout(messageId)
                } else {
                    // 실패 시 다시 실패 상태로 변경
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { message ->
                            if (message.messageId == messageId) {
                                message.copy(
                                    deliveryState = com.example.feature_chat.model.MessageDeliveryState.Failed(
                                        "재전송 실패"
                                    ),
                                    sendFailed = true,
                                    canRetry = message.retryCount < 3,
                                    errorMessage = "재전송에 실패했습니다"
                                )
                            } else {
                                message
                            }
                        }
                        state.copy(messages = updatedMessages.applyDisplayFormatConversion())
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error retrying message: $messageId", e)

                // 예외 발생 시 실패 상태로 변경
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { message ->
                        if (message.messageId == messageId) {
                            message.copy(
                                deliveryState = com.example.feature_chat.model.MessageDeliveryState.Failed(
                                    e.message ?: "알 수 없는 오류"
                                ),
                                sendFailed = true,
                                canRetry = message.retryCount < 3,
                                errorMessage = e.message ?: "재전송 중 오류가 발생했습니다"
                            )
                        } else {
                            message
                        }
                    }
                    state.copy(messages = updatedMessages.applyDisplayFormatConversion())
                }

                _eventFlow.emit(ChatEvent.Error("메시지 재전송에 실패했습니다: ${e.message}"))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()

        // 모든 타임아웃 Job 취소
        messageTimeoutJobs.values.forEach { it.cancel() }
        messageTimeoutJobs.clear()
        
        viewModelScope.launch {
            // Leave room when ViewModel is cleared
            // Note: The actual room leaving is handled by the WebSocketClient
            Log.d("ViewModel", "Left chat room: $channelId (GlobalWebSocketService remains active)")
        }
    }
}
