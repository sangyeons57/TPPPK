package com.example.feature_chat.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.base.Message
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.auth.AuthSessionUseCases
import com.example.domain.provider.chat.ChatUseCaseProvider
import com.example.domain.provider.chat.ChatUseCases
import com.example.domain.provider.file.FileUseCaseProvider
import com.example.domain.provider.file.FileUseCases
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.domain.provider.user.UserUseCases
import com.example.domain.model.base.User
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.queue.QueuedMessageAction
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatUseCaseProvider: ChatUseCaseProvider,
    private val webSocketClient: ChatWebSocketClient,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val navigationManger: NavigationManger,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val fileUseCaseProvider: FileUseCaseProvider
) : ViewModel() {

    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val projectId: String? = savedStateHandle.get<String>(RouteArgs.PROJECT_ID) // Optional for project channels
    private val roomId: String get() = "chat_room_$channelId" // Convert channelId to roomId format

    private val chatUseCases: ChatUseCases by lazy { 
        if (projectId != null) {
            // This is a project channel
            Log.d("ViewModel", "Creating chat use cases for project channel: $projectId/$channelId")
            chatUseCaseProvider.createForChannel(projectId, channelId)
        } else {
            // This is a DM channel
            Log.d("ViewModel", "Creating chat use cases for DM channel: $channelId")
            chatUseCaseProvider.createForDMChannel(channelId)
        }
    }
    
    private val authUseCases: AuthSessionUseCases by lazy { 
        authSessionUseCaseProvider.create()
    }
    
    private val userUseCases: UserUseCases by lazy { 
        userUseCaseProvider.createForUser()
    }

    private val fileUseCases: FileUseCases by lazy {
        fileUseCaseProvider.create()
    }

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
    private var currentUserSession: UserSession? = null
    private var tempMessageCounter = 0L
    
    // User profile cache
    private val userProfileCache = mutableMapOf<String, User>()
    private var hasMoreMessages = true
    private var lastMessageTimestamp: Instant? = null

    init {
        initializeAuthentication()
        initializeChat()
    }
    
    private fun initializeAuthentication() {
        viewModelScope.launch {
            authUseCases.getCurrentUserSessionStreamUseCase().collectLatest { result ->
                when (result) {
                    is CustomResult.Success -> {
                        currentUserSession = result.data
                        currentUserId = result.data.userId.value
                        Log.d("ViewModel", "Current user authenticated: ${currentUserId}")
                        
                        // Update UI state with user info
                        _uiState.update { state ->
                            state.copy(
                                currentUserId = currentUserId,
                                myUserId = currentUserId ?: ""
                            )
                        }
                        
                        // Join room once authenticated (connection is managed globally)
                        joinChatRoom()
                    }
                    is CustomResult.Failure -> {
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

    private fun initializeChat() {
        viewModelScope.launch {
            // Observe global connection state (managed by GlobalWebSocketService)
            observeConnectionState()
            
            // Observe real-time messages
            observeWebSocketMessages()
            
            // Load initial messages from repository
            loadInitialMessages()
        }
    }
    
    private suspend fun joinChatRoom() {
        val userId = currentUserId
        if (userId != null) {
            Log.d("ViewModel", "Joining chat room: $roomId")
            val result = webSocketClient.joinRoom(roomId, UserId(userId))
            
            if (result.isSuccess) {
                Log.d("ViewModel", "Successfully joined chat room: $roomId")
            } else {
                Log.e("ViewModel", "Failed to join chat room: ${result.exceptionOrNull()?.message}")
                _eventFlow.emit(ChatEvent.Error("채팅방 입장 실패"))
            }
        } else {
            Log.w("ViewModel", "Cannot join room: user not authenticated")
            _eventFlow.emit(ChatEvent.Error("인증이 필요합니다"))
        }
    }
    
    @Deprecated("Connection is now managed globally by GlobalWebSocketService")
    private suspend fun reconnectWithAuth() {
        // This method is deprecated as connection is now managed by GlobalWebSocketService
        // We only need to join the room once authenticated
        joinChatRoom()
    }

    private suspend fun getCurrentUserAuthToken(): String? {
        return when (val result = authUseCases.getCurrentUserSessionUseCase()) {
            is CustomResult.Success -> {
                val token = result.data.idToken?.value
                Log.d("ViewModel", "Got auth token: ${token?.take(10)}...")
                token
            }
            is CustomResult.Failure -> {
                Log.e("ViewModel", "Failed to get auth token", result.error)
                null
            }
            else -> {
                Log.d("ViewModel", "Auth token loading...")
                null
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _uiState.update { 
                    it.copy(
                        connectionState = state,
                        queuedMessagesCount = offlineMessageQueue.getQueueSize(),
                        showConnectionError = state is WebSocketConnectionState.Error
                    ) 
                }
                
                when (state) {
                    is WebSocketConnectionState.Connected -> {
                        // Process any queued offline messages
                        // The queue will automatically process when connection is established
                    }
                    is WebSocketConnectionState.Error -> {
                        _eventFlow.emit(ChatEvent.Error("연결 오류: ${state.message}"))
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
    }

    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            webSocketClient.getChatMessages(roomId).collect { event ->
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
                    else -> { /* Handle other events */ }
                }
            }
        }
    }

    private fun handleNewMessage(event: ChatWebSocketEvent.MessageReceived) = viewModelScope.launch {
        loadUserProfile(event.senderId)
        
        val uiMessage = ChatMessageUiModel(
            localId = generateTempId(),
            chatId = event.messageId,
            userId = event.senderId,
            userName = getUserDisplayName(event.senderId),
            userProfileUrl = getUserProfileUrl(event.senderId),
            message = event.content,
            formattedTimestamp = DateTimeUtil.formatChatTime(Instant.parse(event.timestamp)),
            isMyMessage = event.senderId == currentUserId,
            isModified = false,
            attachmentImageUrls = emptyList(),
            isDeleted = false,
            actualTimestamp = Instant.parse(event.timestamp)
        )

        _uiState.update { state ->
            val updatedMessages = (listOf(uiMessage) + state.messages)
                .distinctBy { it.chatId }
                .sortedByDescending { it.actualTimestamp }
            state.copy(messages = updatedMessages)
        }
    }

    private fun handleMessageEdit(event: ChatWebSocketEvent.MessageEdited) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.chatId == event.messageId) {
                    message.copy(
                        message = event.newContent,
                        isModified = true,
                        formattedTimestamp = DateTimeUtil.formatChatTime(Instant.parse(event.timestamp))
                    )
                } else {
                    message
                }
            }
            state.copy(messages = updatedMessages)
        }
    }

    private fun handleMessageDelete(event: ChatWebSocketEvent.MessageDeleted) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { message ->
                if (message.chatId == event.messageId) {
                    message.copy(isDeleted = true)
                } else {
                    message
                }
            }
            state.copy(messages = updatedMessages)
        }
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            
            when (val result = chatUseCases.fetchPastMessagesUseCase(limit = 50)) {
                is CustomResult.Initial -> {
                    _uiState.update { it.copy(isLoadingHistory = false) }
                }
                is CustomResult.Loading -> {
                    // Keep loading state
                }
                is CustomResult.Progress -> {
                    // Show progress if needed
                }
                is CustomResult.Success -> {
                    val messages = result.data.map { message ->
                        async { 
                            loadUserProfile(message.senderId.value)
                            message.toUiModel(currentUserId ?: "", ::generateTempId, ::getUserDisplayName, ::getUserProfileUrl)
                        }
                    }.awaitAll()

                    _uiState.update { 
                        it.copy(
                            messages = messages,
                            isLoadingHistory = false,
                            hasMoreMessages = messages.size == 50, // Assume more messages if we got a full page
                            lastMessageTimestamp = messages.lastOrNull()?.actualTimestamp
                        )
                    }
                }
                is CustomResult.Failure -> {
                    _uiState.update { 
                        it.copy(
                            error = "메시지 로드 실패: ${result.error.message}",
                            isLoadingHistory = false
                        )
                    }
                }
            }
        }
    }

    fun sendMessage(text: String, attachmentUris: List<Uri> = emptyList()) {
        if (text.isBlank() && attachmentUris.isEmpty()) {
            Log.w("ViewModel", "Attempted to send empty message")
            return
        }
        
        val senderId = currentUserId
        if (senderId == null) {
            Log.e("ViewModel", "Cannot send message: user not authenticated")
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }
        
        val messageId = DocumentId.generate()
        Log.d("ViewModel", "Sending message: ${text.take(50)}... by user: $senderId")
        
        viewModelScope.launch {
            val tempUiMessage = ChatMessageUiModel(
                localId = generateTempId(),
                chatId = messageId.value,
                userId = senderId,
                userName = getUserDisplayName(senderId), // Use cache or default
                userProfileUrl = getUserProfileUrl(senderId), // Dynamically generate URL
                message = text,
                formattedTimestamp = "전송 중...",
                isMyMessage = true,
                isModified = false,
                attachmentImageUrls = attachmentUris.map { it.toString() },
                isDeleted = false,
                actualTimestamp = Instant.now()
            )
            
            // Add optimistic message to UI
            _uiState.update { state ->
                state.copy(messages = listOf(tempUiMessage) + state.messages)
            }

            val message = Message.create(
                id = messageId,
                senderId = UserId(senderId),
                content = MessageContent(text),
                replyToMessageId = null
            )

            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    val result = webSocketClient.sendMessage(
                        roomId = roomId,
                        senderId = UserId(senderId),
                        content = text,
                        messageId = messageId
                    )
                    
                    when {
                        result.isSuccess -> {
                            chatUseCases.sendMessageUseCase(
                                UserId(senderId),
                                MessageContent(text)
                            )
                        }
                        result.isFailure -> {
                            handleSendMessageFallback(message, tempUiMessage)
                        }
                    }
                }
                is WebSocketConnectionState.Disconnected,
                is WebSocketConnectionState.Connecting,
                is WebSocketConnectionState.Error -> {
                    offlineMessageQueue.queueMessage(
                        QueuedMessageAction.Send(message, roomId)
                    )
                    handleSendMessageFallback(message, tempUiMessage)
                }
            }
        }
    }

    private suspend fun handleSendMessageFallback(message: Message, tempUiMessage: ChatMessageUiModel) {
        when (val result = chatUseCases.sendMessageUseCase(message.senderId, message.content)) {
            is CustomResult.Initial -> {}
            is CustomResult.Loading -> {}
            is CustomResult.Progress -> {}
            is CustomResult.Success -> {
                _uiState.update { state ->
                    val updatedMessages = state.messages.map {
                        if (it.localId == tempUiMessage.localId) {
                            result.data.toUiModel(currentUserId ?: "", ::generateTempId, ::getUserDisplayName, ::getUserProfileUrl)
                        } else {
                            it
                        }
                    }
                    state.copy(messages = updatedMessages)
                }
            }
            is CustomResult.Failure -> {
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filter { it.localId != tempUiMessage.localId }
                    )
                }
                _eventFlow.emit(ChatEvent.Error("메시지 전송 실패: ${result.error.message}"))
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        if (newContent.isBlank()) {
            Log.w("ViewModel", "Attempted to edit message with empty content")
            return
        }
        
        if (currentUserId == null) {
            Log.e("ViewModel", "Cannot edit message: user not authenticated")
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }
        
        Log.d("ViewModel", "Editing message: $messageId")
        
        viewModelScope.launch {
            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    Log.d("ViewModel", "Editing message via WebSocket")
                    webSocketClient.editMessage(
                        roomId = roomId,
                        messageId = DocumentId(messageId),
                        newContent = newContent
                    )
                }
                else -> {
                    Log.d("ViewModel", "Queueing message edit for offline processing")
                    offlineMessageQueue.queueMessage(
                        QueuedMessageAction.Edit(messageId, newContent, roomId)
                    )
                }
            }
            
            when (val result = chatUseCases.editMessageUseCase(DocumentId(messageId), MessageContent(newContent))) {
                is CustomResult.Failure -> {
                    _eventFlow.emit(ChatEvent.Error("메시지 수정 실패: ${result.error.message}"))
                }
                else -> { /* Success handled by WebSocket event or no-op */ }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        if (currentUserId == null) {
            Log.e("ViewModel", "Cannot delete message: user not authenticated")
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.Error("로그인이 필요합니다"))
            }
            return
        }
        
        Log.d("ViewModel", "Deleting message: $messageId")
        
        viewModelScope.launch {
            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    Log.d("ViewModel", "Deleting message via WebSocket")
                    webSocketClient.deleteMessage(roomId, DocumentId(messageId))
                }
                else -> {
                    Log.d("ViewModel", "Queueing message delete for offline processing")
                    offlineMessageQueue.queueMessage(
                        QueuedMessageAction.Delete(messageId, roomId)
                    )
                }
            }
            
            when (val result = chatUseCases.deleteMessageUseCase(DocumentId(messageId))) {
                is CustomResult.Failure -> {
                    _eventFlow.emit(ChatEvent.Error("메시지 삭제 실패: ${result.error.message}"))
                }
                else -> { /* Success handled by WebSocket event or no-op */ }
            }
        }
    }

    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(pendingMessageText = text) }
    }

    fun onSendMessageClick() {
        val message = _uiState.value.pendingMessageText
        val attachments = _uiState.value.selectedAttachmentUris
        
        if (message.isBlank() && attachments.isEmpty()) return
        
        sendMessage(message, attachments)
        _uiState.update { 
            it.copy(
                pendingMessageText = "", 
                selectedAttachmentUris = emptyList()
            ) 
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            joinChatRoom()
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
               webSocketClient.connectionState.value is WebSocketConnectionState.Connected
    }
    
    fun isReadOnlyMode(): Boolean {
        return !canPerformWriteOperations()
    }

    private fun generateTempId(): String = "temp_${++tempMessageCounter}"

    private fun getUserDisplayName(userId: String): String {
        return userProfileCache[userId]?.name?.value ?: "User $userId"
    }

    private suspend fun getUserProfileUrl(userId: String): String? {
        val path = "user_profiles/$userId/profile.webp"
        val fileExists = fileUseCases.checkFileExistenceUseCase(path)
        
        return if (fileExists) {
            val projectId = "teamnovaprojectprojecting"
            val encodedPath = "user_profiles%2F${userId}%2Fprofile.webp"
            "https://firebasestorage.googleapis.com/v0/b/${projectId}.appspot.com/o/${encodedPath}?alt=media"
        } else {
            null
        }
    }
    
    private fun loadUserProfile(userId: String) {
        if (userProfileCache.containsKey(userId)) return
        
        viewModelScope.launch {
            when (val result = userUseCases.getUserByIdUseCase(DocumentId(userId))) {
                is CustomResult.Success -> {
                    userProfileCache[userId] = result.data
                    Log.d("ViewModel", "Loaded user profile: ${result.data.name.value}")
                    updateMessagesForUser(userId)
                }
                is CustomResult.Failure -> {
                    Log.w("ViewModel", "Failed to load user profile for $userId", result.error)
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    private fun updateMessagesForUser(userId: String) = viewModelScope.launch {
        val updatedMessages = _uiState.value.messages.map { message ->
            if (message.userId == userId) {
                message.copy(
                    userName = getUserDisplayName(userId),
                    userProfileUrl = getUserProfileUrl(userId)
                )
            } else {
                message
            }
        }
        _uiState.update { it.copy(messages = updatedMessages) }
    }

    fun onImagesSelected(uris: List<Uri>) {
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uris) }
    }
    
    fun loadMoreMessages() {
        if (_uiState.value.isLoadingMoreMessages || !_uiState.value.hasMoreMessages) {
            Log.d("ViewModel", "Already loading or no more messages")
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreMessages = true) }
            
            val oldestMessage = _uiState.value.messages.minByOrNull { it.actualTimestamp }
            val beforeTimestamp = oldestMessage?.actualTimestamp
            
            Log.d("ViewModel", "Loading more messages before: $beforeTimestamp")
            
            when (val result = chatUseCases.fetchPastMessagesUseCase(limit = 50)) {
                is CustomResult.Success -> {
                    val allMessages = result.data
                    
                    val newMessages = if (beforeTimestamp != null) {
                        allMessages.filter { it.createdAt.isBefore(beforeTimestamp) }
                    } else {
                        allMessages
                    }
                    
                    val newUiMessages = newMessages.map {
                        async {
                            loadUserProfile(it.senderId.value)
                            it.toUiModel(currentUserId ?: "", ::generateTempId, ::getUserDisplayName, ::getUserProfileUrl)
                        }
                    }.awaitAll()
                    
                    val hasMore = newUiMessages.size == 50
                    
                    _uiState.update { state ->
                        val combinedMessages = (state.messages + newUiMessages)
                            .distinctBy { it.chatId }
                            .sortedByDescending { it.actualTimestamp }
                            
                        state.copy(
                            messages = combinedMessages,
                            isLoadingMoreMessages = false,
                            hasMoreMessages = hasMore,
                            lastMessageTimestamp = newUiMessages.lastOrNull()?.actualTimestamp
                        )
                    }
                    
                    Log.d("ViewModel", "Loaded ${newUiMessages.size} more messages, hasMore: $hasMore")
                }
                is CustomResult.Failure -> {
                    Log.e("ViewModel", "Failed to load more messages", result.error)
                    _uiState.update { 
                        it.copy(
                            isLoadingMoreMessages = false,
                            error = "메시지 로드 실패: ${result.error.message}"
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingMoreMessages = false) }
                }
            }
        }
    }
    
    fun onBackClick() {
        viewModelScope.launch {
            navigationManger.navigateBack()
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
            webSocketClient.leaveRoom(roomId)
            Log.d("ViewModel", "Left chat room: $roomId (GlobalWebSocketService remains active)")
        }
    }
}

private suspend fun Message.toUiModel(currentUserId: String, tempIdGenerator: () -> String, getUserDisplayName: (String) -> String, getUserProfileUrl: suspend (String) -> String?): ChatMessageUiModel {
    val isModified = this.updatedAt.isAfter(this.createdAt.plusSeconds(1))
    
    return ChatMessageUiModel(
        localId = tempIdGenerator(),
        chatId = this.id.value,
        userId = this.senderId.value,
        userName = getUserDisplayName(this.senderId.value),
        userProfileUrl = getUserProfileUrl(this.senderId.value),
        message = this.content.value,
        formattedTimestamp = DateTimeUtil.formatChatTime(this.createdAt),
        isModified = isModified,
        attachmentImageUrls = emptyList(), // TODO: Handle attachments
        isMyMessage = this.senderId.value == currentUserId,
        isDeleted = this.isDeleted.value,
        actualTimestamp = this.createdAt
    )
}