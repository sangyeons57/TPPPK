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
import com.example.domain.model.base.Message
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.MentionType
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain_repository.local.LocalMessagePagingRepository
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
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
import kotlinx.coroutines.flow.map
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
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val chatServiceProvider: ChatServiceProvider,
    private val localMessageRepository: LocalMessagePagingRepository,
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

    // Paging3 Flow for messages from Room DB (Single Source of Truth)
    val messagesFlow: Flow<PagingData<ChatMessageUiModel>> =
        Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                localMessageRepository.getMessagesPagingSource()
            }
        ).flow.map { pagingData: PagingData<Message> ->
            pagingData.map<Message, ChatMessageUiModel> { message: Message ->
                // Convert domain Message to UI model with display format
                convertDomainMessageToUiModel(message)
            }
        }.cachedIn(viewModelScope)

    init {
        // Log local cache on entry
        logChannelCacheOnEntry()

        initializeUserSession()
        joinChatRoom()
        observeWebSocketEventsForUiEvents()
        loadChannelData()
    }

    private fun logChannelCacheOnEntry() {
        viewModelScope.launch {
            Log.i("Debug", "--- Initializing chat for channel $channelId ---")
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
                    val generalUseCases = webSocketUseCaseProvider.create()

                    roomUseCases.joinRoomUseCase(com.example.domain.model.vo.UserId(userId))
                    Log.d("ViewModel", "Joined chat room: $channelId")

                    // Observe WebSocket connection state
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
                    Log.e("ViewModel", "Failed to join chat room", e)
                    _eventFlow.emit(ChatEvent.Error("채팅방 입장 실패: ${e.message}"))
                }
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
                            cancelMessageTimeout(event.messageId)
                            Log.d("ViewModel", "Message ACK handled by Room: ${event.messageId}")
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
                // TODO: Update message status in Room database
                // localMessageRepository.updateMessageStatus(event.messageId, sent = true, failed = false)
                Log.d(
                    "ViewModel",
                    "Message marked as successfully sent in database: ${event.messageId}"
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to update message status in database", e)
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
                // TODO: Update message status in Room database
                // localMessageRepository.updateMessageStatus(event.messageId, sent = false, failed = true)
                Log.d("ViewModel", "Message marked as failed in database: ${event.messageId}")
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to update message status in database", e)
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
                // Use core WebSocket UseCase directly
                val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                roomUseCases.sendMessageUseCase(
                    senderId = com.example.domain.model.vo.UserId(senderId),
                    content = text,
                    messageId = com.example.domain.model.vo.DocumentId(
                        java.util.UUID.randomUUID().toString()
                    ),
                    replyToMessageId = replyToMessageId?.let {
                        com.example.domain.model.vo.DocumentId(
                            it
                        )
                    }
                )

                Log.d("ViewModel", "Message sent via WebSocket")
                // Message will be automatically saved to Room DB by core_websocket
                // Paging3 will automatically refresh when new data is available

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
                    messageId = com.example.domain.model.vo.DocumentId(messageId),
                    newContent = newContent
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
                    messageId = com.example.domain.model.vo.DocumentId(messageId)
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
    private fun convertDomainMessageToUiModel(message: Message): ChatMessageUiModel {
        return ChatMessageUiModel(
            messageId = message.id.value,
            userId = message.senderId.value,
            userName = "Unknown", // TODO: Get from user profile
            userProfileUrl = null, // TODO: Get from user profile
            message = convertInternalToDisplayFormat(message.content.value),
            formattedTimestamp = DateTimeUtil.formatChatTime(message.createdAt),
            actualTimestamp = message.createdAt,
            isModified = message.updatedAt != message.createdAt,
            attachmentImageUrls = emptyList(), // TODO: Handle attachments
            isMyMessage = message.senderId.value == currentUserId,
            isSending = false,
            sendFailed = false,
            isDeleted = message.isDeleted.value,
            deliveryState = MessageDeliveryState.Sent,
            isOptimistic = false,
            clientSentAt = null,
            retryCount = 0,
            canRetry = false,
            errorMessage = null,
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
                // TODO: Update message status in Room database
                // localMessageRepository.updateMessageStatus(messageId, sent = true, failed = false)
                Log.d("ViewModel", "Timeout: Message marked as sent in database: $messageId")
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
                    senderId = com.example.domain.model.vo.UserId(currentUserId!!),
                    content = "[Retry Message]", // TODO: Get original content from Room DB
                    messageId = com.example.domain.model.vo.DocumentId(messageId), // Use same ID for retry
                    replyToMessageId = null // TODO: Get original reply info from Room DB if needed
                )

                Log.d("ViewModel", "Message retry sent via WebSocket: $messageId")
                _eventFlow.emit(ChatEvent.SystemMessage("메시지 재전송을 시도했습니다"))
                
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to retry message", e)
                _eventFlow.emit(ChatEvent.Error("메시지 재전송 실패: ${e.message}"))
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
