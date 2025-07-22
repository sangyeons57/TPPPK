package com.example.feature_chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.feature_chat.service.ChatServiceProvider
import com.example.feature_chat.websocket.ChatWebSocketEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
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
                    else -> { /* Handle other events */ }
                }
            }
        }
    }

    private suspend fun handleNewMessage(event: ChatWebSocketEvent.MessageReceived) {
        currentUserId?.let { userId ->
            // 서버에서 이미 echo prevention을 처리하므로 모든 메시지를 처리
            Log.d("ViewModel", "Processing WebSocket message: ${event.messageId} from ${event.senderId}")
            
            val result = services.messageService.handleNewMessage(event, userId)
            
            _uiState.update { state ->
                state.copy(
                    messages = result.messages,
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
                messages = result.messages,
                hasMoreMessages = result.hasMoreOlderMessages,
                error = result.error
            )
        }
    }

    private fun handleMessageDelete(event: ChatWebSocketEvent.MessageDeleted) {
        val result = services.messageService.handleMessageDelete(event)
        
        _uiState.update { state ->
            state.copy(
                messages = result.messages,
                hasMoreMessages = result.hasMoreOlderMessages,
                error = result.error
            )
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
                    messages = result.messages,
                    isLoadingHistory = false,
                    hasMoreMessages = result.hasMoreOlderMessages,
                    lastMessageTimestamp = result.lastMessageTimestamp,
                    error = result.error
                )
            }
        }
    }

    fun sendMessage(text: String, attachmentUris: List<Uri> = emptyList()) {
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
            val result = services.messageService.sendMessage(text, attachmentUris, senderId)
            
            if (result.success && result.tempMessage != null) {
                // Add optimistic message to UI
                _uiState.update { state ->
                    state.copy(messages = listOf(result.tempMessage) + state.messages)
                }
                
                // If we have actual message, replace temp message
                result.actualMessage?.let { actualMessage ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map {
                            if (it.localId == result.tempMessage.localId) {
                                actualMessage
                            } else {
                                it
                            }
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
            } else if (!result.success) {
                result.error?.let { error ->
                    _eventFlow.emit(ChatEvent.Error(error))
                }
                
                // Remove temp message if it was added
                result.tempMessage?.let { tempMessage ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.filter { it.localId != tempMessage.localId }
                        )
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
                        messages = result.messages,
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

    // UI Action Methods
    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(pendingMessageText = text) }
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