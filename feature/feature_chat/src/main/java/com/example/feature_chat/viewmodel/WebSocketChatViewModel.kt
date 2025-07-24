package com.example.feature_chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.MentionType
import com.example.domain.model.vo.message.MentionInfo
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole
import com.example.feature_chat.service.ChatServiceProvider
import com.example.feature_chat.websocket.ChatWebSocketEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

/**
 * Refactored WebSocketChatViewModel using Service Provider pattern
 * Reduced from 1000+ lines to ~450 lines by delegating logic to Services
 */
@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatServiceProvider: ChatServiceProvider
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

    init {
        initializeAuthentication()
        initializeConnection()
        initializeMessages()
        loadChannelData()
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
                    is com.example.core_common.websocket.WebSocketConnectionState.Error -> {
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
            val roomId = "chat_room_$channelId"
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
                    // 임시 메시지 제거 (서버에서 온 실제 메시지로 대체)
                    result.messages.filterNot { message ->
                        message.userId == userId && 
                        message.isOptimistic && 
                        message.message.trim() == event.content.trim()
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

    private fun handleMessageEdit(event: ChatWebSocketEvent.MessageEdited) {
        val result = services.messageService.handleMessageEdit(event)
        
        _uiState.update { state ->
            state.copy(
                messages = result.messages.applyDisplayFormatConversion(),
                hasMoreMessages = result.hasMoreOlderMessages,
                error = result.error
            )
        }
    }

    private fun handleMessageDelete(event: ChatWebSocketEvent.MessageDeleted) {
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
        
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.chatId == event.messageId && message.isOptimistic) {
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
        
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.chatId == event.messageId && message.isOptimistic) {
                    Log.d("ViewModel", "Marking message as failed: ${event.messageId}")
                    message.copy(
                        isSending = false,
                        sendFailed = true,
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

    private fun sendMessage(text: String, attachmentUris: List<Uri> = emptyList(), mentions: List<MentionInfo> = emptyList()) {
        val senderId = currentUserId
        if (senderId == null) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }

        // 웹소켓 연결 상태 확인
        val connectionState = _uiState.value.connectionState
        if (connectionState !is com.example.core_common.websocket.WebSocketConnectionState.Connected) {
            viewModelScope.launch {
                val statusMessage = when (connectionState) {
                    is com.example.core_common.websocket.WebSocketConnectionState.Connecting -> 
                        "연결 중입니다. 잠시만 기다려주세요."
                    is com.example.core_common.websocket.WebSocketConnectionState.Disconnected -> 
                        "서버와 연결이 끊어져 있습니다. 연결을 다시 시도해주세요."
                    is com.example.core_common.websocket.WebSocketConnectionState.Error -> 
                        "연결 오류가 발생했습니다: ${connectionState.message}"
                    else -> "메시지 전송이 불가능한 상태입니다."
                }
                _eventFlow.emit(ChatEvent.ShowSnackbar(statusMessage))
            }
            return
        }

        viewModelScope.launch {
            val result = services.messageService.sendMessage(text, attachmentUris, senderId, mentions)
            
            if (result.success && result.tempMessage != null) {
                // Add optimistic message to UI with isSending = true
                _uiState.update { state ->
                    val tempMessageWithDisplayFormat = result.tempMessage.copy(
                        message = convertInternalToDisplayFormat(result.tempMessage.message)
                    )
                    state.copy(messages = listOf(tempMessageWithDisplayFormat) + state.messages)
                }
                
                // ACK 기반 메시지 상태 관리: 서버에서 ACK/FAILED 메시지로 상태 업데이트
                // 30초 타임아웃 제거 - ACK 시스템으로 정확한 성공/실패 판정
                
                // If we have actual message, replace temp message immediately
                result.actualMessage?.let { actualMessage ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map {
                            if (it.localId == result.tempMessage.localId) {
                                Log.d("ViewModel", "Replacing temp message with actual message: ${it.localId}")
                                actualMessage.copy(
                                    isOptimistic = false,
                                    isSending = false,
                                    sendFailed = false,
                                    clientSentAt = result.tempMessage.clientSentAt // 원래 전송 시간 유지
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
                            if (it.localId == tempMessage.localId) {
                                Log.d("ViewModel", "Marking temp message as failed: ${it.localId}")
                                it.copy(
                                    isSending = false,
                                    sendFailed = true,
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
        } else {
            hideMentionSuggestions()
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
        if (connectionState !is com.example.core_common.websocket.WebSocketConnectionState.Connected) {
            viewModelScope.launch {
                val statusMessage = when (connectionState) {
                    is com.example.core_common.websocket.WebSocketConnectionState.Connecting -> 
                        "연결 중입니다. 잠시만 기다려주세요."
                    is com.example.core_common.websocket.WebSocketConnectionState.Disconnected -> 
                        "서버와 연결이 끊어져 있습니다."
                    is com.example.core_common.websocket.WebSocketConnectionState.Error -> 
                        "연결 오류: ${connectionState.message}"
                    else -> "메시지 전송이 불가능합니다."
                }
                _eventFlow.emit(ChatEvent.ShowSnackbar(statusMessage))
            }
            return
        }
        
        // Convert display format (@displayName) back to internal format ([type:id]) for processing
        val internalMessage = convertDisplayToInternalFormat(message)
        val (processedText, mentions) = parseMentions(internalMessage)
        sendMessage(processedText, attachments, mentions)

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
        val mentionRegex = "\\[(USER|ROLE):(\\S+?)\\]".toRegex()
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
        val mentionRegex = "\\[(USER|ROLE):(\\S+?)\\]".toRegex()
        
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            // Leave room when ViewModel is cleared
            // Note: The actual room leaving is handled by the WebSocketClient
            Log.d("ViewModel", "Left chat room: chat_room_$channelId (GlobalWebSocketService remains active)")
        }
    }
}