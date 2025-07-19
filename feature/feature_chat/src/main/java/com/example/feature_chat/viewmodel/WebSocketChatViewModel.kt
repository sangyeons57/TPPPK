package com.example.feature_chat.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.provider.chat.ChatUseCaseProvider
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.queue.QueuedMessageAction
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatUseCaseProvider: ChatUseCaseProvider,
    private val webSocketClient: ChatWebSocketClient,
    private val offlineMessageQueue: OfflineMessageQueue
) : ViewModel() {

    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)
    private val roomId: String get() = "chat_room_$channelId" // Convert channelId to roomId format

    private val chatUseCases by lazy { 
        chatUseCaseProvider.createForChannel(channelId)
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
    private var tempMessageCounter = 0L

    init {
        initializeChat()
    }

    private fun initializeChat() {
        viewModelScope.launch {
            // Initialize connection to WebSocket server
            val serverUrl = "wss://your-websocket-server.com/chat" // TODO: Get from config
            val authToken = getCurrentUserAuthToken() // TODO: Get from auth repository
            
            if (authToken != null) {
                webSocketClient.connect(serverUrl, authToken)
                webSocketClient.joinRoom(roomId)
            }
            
            // Observe connection state
            observeConnectionState()
            
            // Observe real-time messages
            observeWebSocketMessages()
            
            // Load initial messages from repository
            loadInitialMessages()
        }
    }

    private suspend fun getCurrentUserAuthToken(): String? {
        // TODO: Get auth token from auth repository
        return "dummy_token" // Placeholder
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

    private fun handleNewMessage(event: ChatWebSocketEvent.MessageReceived) {
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
                        message.toUiModel(currentUserId ?: "", ::generateTempId)
                    }
                    _uiState.update { 
                        it.copy(
                            messages = messages,
                            isLoadingHistory = false
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
        if (text.isBlank() && attachmentUris.isEmpty()) return
        
        val senderId = currentUserId ?: return
        val messageId = DocumentId.generate()
        
        // Create optimistic UI message
        val tempUiMessage = ChatMessageUiModel(
            localId = generateTempId(),
            chatId = messageId.value,
            userId = senderId,
            userName = getUserDisplayName(senderId),
            userProfileUrl = getUserProfileUrl(senderId),
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

        viewModelScope.launch {
            val message = Message.create(
                id = messageId,
                senderId = UserId(senderId),
                content = MessageContent(text),
                replyToMessageId = null
            )

            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    // Send via WebSocket
                    val result = webSocketClient.sendMessage(
                        roomId = roomId,
                        senderId = UserId(senderId),
                        content = text,
                        messageId = messageId
                    )
                    
                    when {
                        result.isSuccess -> {
                            // Message sent successfully via WebSocket
                            // Also save to repository for persistence
                            chatUseCases.sendMessageUseCase(
                                UserId(senderId),
                                MessageContent(text)
                            )
                        }
                        result.isFailure -> {
                            // WebSocket failed, fall back to repository only
                            handleSendMessageFallback(message, tempUiMessage)
                        }
                    }
                }
                is WebSocketConnectionState.Disconnected,
                is WebSocketConnectionState.Connecting,
                is WebSocketConnectionState.Error -> {
                    // WebSocket not connected, queue message and use repository
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
            is CustomResult.Initial -> {
                // Handle initial state
            }
            is CustomResult.Loading -> {
                // Keep loading state
            }
            is CustomResult.Progress -> {
                // Show progress if needed
            }
            is CustomResult.Success -> {
                // Update UI with real message
                _uiState.update { state ->
                    val updatedMessages = state.messages.map {
                        if (it.localId == tempUiMessage.localId) {
                            result.data.toUiModel(currentUserId ?: "", ::generateTempId)
                        } else {
                            it
                        }
                    }
                    state.copy(messages = updatedMessages)
                }
            }
            is CustomResult.Failure -> {
                // Remove failed message from UI
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
        viewModelScope.launch {
            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    webSocketClient.editMessage(
                        roomId = roomId,
                        messageId = DocumentId(messageId),
                        newContent = newContent
                    )
                }
                else -> {
                    offlineMessageQueue.queueMessage(
                        QueuedMessageAction.Edit(messageId, newContent, roomId)
                    )
                }
            }
            
            // Also update in repository
            when (val result = chatUseCases.editMessageUseCase(DocumentId(messageId), MessageContent(newContent))) {
                is CustomResult.Initial -> {
                    // Handle initial state
                }
                is CustomResult.Loading -> {
                    // Keep loading state
                }
                is CustomResult.Progress -> {
                    // Show progress if needed
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(ChatEvent.Error("메시지 수정 실패: ${result.error.message}"))
                }
                is CustomResult.Success -> { /* Success handled by WebSocket event */ }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    webSocketClient.deleteMessage(roomId, DocumentId(messageId))
                }
                else -> {
                    offlineMessageQueue.queueMessage(
                        QueuedMessageAction.Delete(messageId, roomId)
                    )
                }
            }
            
            // Also update in repository
            when (val result = chatUseCases.deleteMessageUseCase(DocumentId(messageId))) {
                is CustomResult.Initial -> {
                    // Handle initial state
                }
                is CustomResult.Loading -> {
                    // Keep loading state
                }
                is CustomResult.Progress -> {
                    // Show progress if needed
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(ChatEvent.Error("메시지 삭제 실패: ${result.error.message}"))
                }
                is CustomResult.Success -> { /* Success handled by WebSocket event */ }
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
            val serverUrl = "wss://your-websocket-server.com/chat" // TODO: Get from config
            val authToken = getCurrentUserAuthToken()
            
            if (authToken != null) {
                webSocketClient.connect(serverUrl, authToken)
                webSocketClient.joinRoom(roomId)
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
        return when (val state = _uiState.value.connectionState) {
            is WebSocketConnectionState.Connected -> "실시간 연결됨"
            is WebSocketConnectionState.Connecting -> "연결 중..."
            is WebSocketConnectionState.Disconnected -> "오프라인"
            is WebSocketConnectionState.Error -> "연결 오류: ${state.message}"
        }
    }

    private fun generateTempId(): String = "temp_${++tempMessageCounter}"

    private fun getUserDisplayName(userId: String): String {
        // TODO: Get from user repository or cache
        return "User $userId"
    }

    private fun getUserProfileUrl(userId: String): String? {
        // TODO: Get from user repository or cache
        return null
    }

    // Additional methods for UI compatibility
    fun onImagesSelected(uris: List<Uri>) {
        // Implementation for handling multiple image selection
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uris) }
    }
    
    fun loadMoreMessages() {
        // Load more messages functionality - placeholder for now
    }
    
    fun onBackClick() {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.NavigateBack)
        }
    }
    
    fun confirmEditMessage() {
        // Implementation for confirming message edit
        val messageId = _uiState.value.editingMessageId?.toString()
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
                editingMessageId = messageId.toIntOrNull(), 
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
            webSocketClient.disconnect()
        }
    }
}

// Extension function to convert domain Message to UI model
private fun Message.toUiModel(currentUserId: String, tempIdGenerator: () -> String): ChatMessageUiModel {
    return ChatMessageUiModel(
        localId = tempIdGenerator(),
        chatId = this.id.value,
        userId = this.senderId.value,
        userName = "User ${this.senderId.value}", // TODO: Get real username
        userProfileUrl = null, // TODO: Get real profile URL
        message = this.content.value,
        formattedTimestamp = DateTimeUtil.formatChatTime(this.createdAt),
        isModified = false, // TODO: Check if message was edited
        attachmentImageUrls = emptyList(), // TODO: Handle attachments
        isMyMessage = this.senderId.value == currentUserId,
        isDeleted = this.isDeleted.value,
        actualTimestamp = this.createdAt
    )
}