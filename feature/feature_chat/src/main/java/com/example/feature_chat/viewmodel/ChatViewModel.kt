package com.example.feature_chat.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import com.example.domain.model.Channel
import com.example.domain.model.MessageAttachment
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.UserRepository
import com.example.domain.usecase.message.DeleteMessageUseCase
import com.example.domain.usecase.message.EditMessageUseCase
import com.example.domain.usecase.message.FetchPastMessagesUseCase
import com.example.domain.usecase.chat.GetLocalGalleryImagesUseCase
import com.example.domain.usecase.message.GetMessagesStreamUseCase
import com.example.domain.usecase.message.SendMessageUseCase
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.GalleryImageUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import com.example.core_navigation.extension.getRequiredString
import com.example.core_navigation.destination.AppRoutes
import android.util.Log
import com.example.data.model.channel.ChannelLocator
import com.example.domain.model.AttachmentType
import com.example.domain.model.ChannelType

// --- Domain 모델 -> UI 모델 변환 함수 ---
// ViewModel 내부 또는 별도 Mapper 파일에 위치 가능
private fun ChatMessage.toUiModel(myUserId: String, tempIdGenerator: () -> String): ChatMessageUiModel {
    return ChatMessageUiModel(
        localId = tempIdGenerator(),
        chatId = this.id, 
        userId = this.senderId, 
        userName = this.senderName, 
        userProfileUrl = this.senderProfileUrl,
        message = this.text, 
        formattedTimestamp = DateTimeUtil.formatChatTime(this.timestamp),
        isModified = this.isEdited, 
        attachmentImageUrls = this.attachments.map { it.url },
        isMyMessage = this.senderId == myUserId,
        isDeleted = this.isDeleted,
        actualTimestamp = this.timestamp
    )
}

private fun MediaImage.toUiModel(): GalleryImageUiModel {
    return GalleryImageUiModel(
        uri = this.contentPath,
        id = this.id,
        isSelected = false
    )
}

private const val INITIAL_MESSAGE_LOAD_LIMIT = 30
private const val PAST_MESSAGE_LOAD_LIMIT = 20

// Channel 클래스에 ProjectSpecificData와 DmSpecificData는 이미 정의되어 있으므로 제거

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val channelRepository: ChannelRepository,
    private val getMessagesStreamUseCase: GetMessagesStreamUseCase,
    private val fetchPastMessagesUseCase: FetchPastMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getLocalGalleryImagesUseCase: GetLocalGalleryImagesUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    // 채널 타입 정의 - AppRoutes.Chat에서 ARG_CHANNEL_TYPE 가져오기
    private val channelId: String = savedStateHandle.getRequiredString(AppRoutes.Chat.ARG_CHANNEL_ID)
    // 프로젝트 채널의 경우 필요한 정보
    private val projectId: String? = savedStateHandle["projectId"] 
    private val categoryId: String? = savedStateHandle["categoryId"]
    // 채널 타입 (dm, project_direct, project_category)
    private val channelType: String = savedStateHandle["channelType"] ?: "dm"

    private val channelLocator: ChannelLocator = run {
        Log.d("ChatViewModel", "Initializing ChannelLocator with type: $channelType, id: $channelId, pId: $projectId, cId: $categoryId")
        when (channelType) {
            "dm" -> ChannelLocator.Dm(channelId = channelId)
            "project_direct" -> {
                val pId = projectId ?: throw IllegalArgumentException("Project ID is required for project_direct channel type")
                ChannelLocator.ProjectDirectChannel(projectId = pId, channelId = channelId)
            }
            "project_category" -> {
                val pId = projectId ?: throw IllegalArgumentException("Project ID is required for project_category channel type")
                val cId = categoryId ?: throw IllegalArgumentException("Category ID is required for project_category channel type")
                ChannelLocator.ProjectCategoryChannel(projectId = pId, categoryId = cId, channelId = channelId)
            }
            else -> throw IllegalArgumentException("Unknown channel type: $channelType")
        }
    }

    private val _uiState = MutableStateFlow(ChatUiState(channelName = "로딩 중...", isLoadingHistory = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentUserId: String? = null
    private var oldestLoadedMessageTimestamp: Instant? = null
    private var oldestMessageReached = false

    private var tempMessageLocalIdCounter = -1L

    init {
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUserStream()
                .filterNotNull() 
                .firstOrNull() 
                ?.getOrNull()
                
            currentUserId = currentUser?.id // userId -> id로 수정
                
            if (currentUserId == null) {
                Log.e("ChatViewModel", "Failed to get current user ID.")
                _uiState.update {
                    it.copy(
                        error = "사용자 정보를 가져올 수 없습니다.",
                        isLoadingHistory = false,
                        channelName = "오류"
                    )
                }
                return@launch 
            }

            _uiState.update { it.copy(myUserId = currentUserId!!) }

            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            Log.d("ChatViewModel", "Fetching channel details for ID: $channelId")
            val result = channelRepository.getChannel(channelId)
            if (result.isSuccess) {
                val channel = result.getOrThrow()
                Log.d("ChatViewModel", "Channel details fetched: ${channel.name}")

                // --- BEGINNING OF ADDED/MODIFIED CODE ---
                if (channel.type == com.example.domain.model.ChannelType.DM) {
                    val isCurrentUserParticipant = channel.dmSpecificData?.participantIds?.contains(currentUserId) == true
                    if (!isCurrentUserParticipant) {
                        Log.e("ChatViewModel", "Access Denied: Current user $currentUserId is not a participant in DM channel ${channel.id}. Participants: ${channel.dmSpecificData?.participantIds}")
                        _uiState.update {
                            it.copy(
                                error = "채팅방에 접근 권한이 없습니다.", // "No permission to access this chat room."
                                channelName = "접근 불가", // "Inaccessible"
                                isLoadingHistory = false
                            )
                        }
                        // Potentially emit an event to navigate back or show a more prominent error UI
                        // _eventFlow.emit(ChatEvent.NavigateBack) // Example
                        return@launch // Stop further processing for this channel
                    }
                }
                // --- END OF ADDED/MODIFIED CODE ---

                validateChannelContext(channel) // Existing validation

                _uiState.update {
                    it.copy(
                        channelName = channel.name,
                        channelPath = channelLocator.getMessagesPath(),
                    )
                }
                initializeChatFeatures()
            } else {
                val errorMsg = "채널 정보를 불러오는데 실패했습니다: ${result.exceptionOrNull()?.message}"
                Log.e("ChatViewModel", errorMsg)
                _uiState.update {
                    it.copy(
                        error = errorMsg,
                        channelName = "오류",
                        isLoadingHistory = false
                    )
                }
            }
        }
    }
    
    private fun validateChannelContext(channel: Channel) {
        // 채널 타입 확인 (ChannelType 모델 활용)
        val channelTypeValue = when (channelType) {
            "dm" -> ChannelType.DM
            "project_direct" -> ChannelType.PROJECT
            "project_category" -> ChannelType.PROJECT
            else -> ChannelType.UNKNOWN
        }
        
        // 채널 타입 일치 여부 확인
        if (channel.type != channelTypeValue) {
            Log.w("ChatViewModel", "Channel context mismatch: Expected type '${channelTypeValue.value}' based on nav arg type '$channelType', but channel type is '${channel.type.value}'. Channel ID: $channelId")
        }
        
        // 프로젝트 ID 확인
        val metadataProjectId = channel.projectSpecificData?.projectId
        
        // 카테고리 ID 확인
        val metadataCategoryId = channel.projectSpecificData?.categoryId
        
        // 프로젝트 ID 일치 여부 확인 (PROJECT 또는 CATEGORY 타입일 때)
        if (channel.type == ChannelType.PROJECT || channel.type == ChannelType.PROJECT) {
            if (projectId != metadataProjectId) {
                Log.w("ChatViewModel", "Channel context mismatch: Expected projectId '$projectId' from nav arg, but channel projectId is '$metadataProjectId'. Channel ID: $channelId")
            }
        }
        
        // 카테고리 ID 일치 여부 확인 (CATEGORY 타입일 때)
        if (channel.type == ChannelType.PROJECT) {
            if (categoryId != metadataCategoryId) {
                Log.w("ChatViewModel", "Channel context mismatch: Expected categoryId '$categoryId' from nav arg, but channel categoryId is '$metadataCategoryId'. Channel ID: $channelId")
            }
        }
    }

    private fun initializeChatFeatures() {
        val myId = currentUserId ?: return 

        observeMessages(myId)
    }

    private fun observeMessages(currentUserIdForMapping: String) {
        viewModelScope.launch {
            getMessagesStreamUseCase(channelId = channelId, limit = INITIAL_MESSAGE_LOAD_LIMIT)
                .catch { e -> 
                    Log.e("ChatViewModel", "Error observing messages", e)
                    _uiState.update { it.copy(error = "메시지를 실시간으로 불러오는 중 오류 발생: ${e.message}", isLoadingHistory = false) }
                }
                .map { messages -> 
                    if (messages.isNotEmpty() && (_uiState.value.messages.isEmpty() || messages.first().timestamp < (oldestLoadedMessageTimestamp ?: Instant.MAX))) {
                         oldestLoadedMessageTimestamp = messages.firstOrNull()?.timestamp
                    }
                     if (messages.size < INITIAL_MESSAGE_LOAD_LIMIT && _uiState.value.messages.isEmpty()) {
                        oldestMessageReached = true
                    }
                    messages.map { msg -> msg.toUiModel(currentUserIdForMapping) { (++tempMessageLocalIdCounter).toString() } } 
                }
                .collect { uiMessages ->
                    _uiState.update { 
                        it.copy(
                            messages = uiMessages,
                            isLoadingHistory = false
                        ) 
                    }
                }
        }
    }

    fun loadMorePastMessages() {
        if (_uiState.value.isLoadingHistory || oldestMessageReached) return

        val myUserId = currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            val result = fetchPastMessagesUseCase(
                channelId = channelId,
                before = oldestLoadedMessageTimestamp,
                limit = PAST_MESSAGE_LOAD_LIMIT
            )

            if (result.isSuccess) {
                val pastMessages = result.getOrThrow()
                if (pastMessages.isNotEmpty()) {
                    oldestLoadedMessageTimestamp = pastMessages.firstOrNull()?.timestamp
                     val uiPastMessages = pastMessages.map { it.toUiModel(myUserId) { (++tempMessageLocalIdCounter).toString() } }
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages = uiPastMessages + currentState.messages,
                            isLoadingHistory = false
                        )
                    }
                } else {
                    oldestMessageReached = true
                    _uiState.update { it.copy(isLoadingHistory = false) }
                }
                if (pastMessages.size < PAST_MESSAGE_LOAD_LIMIT) {
                    oldestMessageReached = true
                }
            } else {
                val errorMsg = "이전 메시지를 불러오는데 실패했습니다: ${result.exceptionOrNull()?.message}"
                Log.e("ChatViewModel", errorMsg)
                _uiState.update { it.copy(error = errorMsg, isLoadingHistory = false) }
            }
        }
    }

    fun sendMessage(text: String, attachmentUris: List<Uri> = emptyList()) {
        if (text.isBlank() && attachmentUris.isEmpty()) {
            return
        }
        val currentSenderId = currentUserId ?: run {
            viewModelScope.launch { _eventFlow.emit(ChatEvent.Error("사용자 정보를 찾을 수 없어 메시지를 보낼 수 없습니다.")) }
            return
        }

        val attachments = attachmentUris.map { uri ->
            MessageAttachment(
                id = (++tempMessageLocalIdCounter).toString(),
                type = AttachmentType.fromString("image"),
                url = uri.toString(),
                fileName = uri.lastPathSegment ?: "attachment",
                size = null,
                mimeType = null,
                thumbnailUrl = null
            )
        }
        
        val tempUiMessage = ChatMessageUiModel(
            localId = (++tempMessageLocalIdCounter).toString(),
            chatId = tempMessageLocalIdCounter.toString(),
            userId = currentSenderId,
            userName = _uiState.value.myUserNameDisplay ?: "나",
            userProfileUrl = _uiState.value.myUserProfileUrl,
            message = text,
            formattedTimestamp = "전송 중...",
            isMyMessage = true,
            attachmentImageUrls = attachmentUris.map { it.toString() },
            isModified = false,
            actualTimestamp = Instant.now()
        )
        _uiState.update { it.copy(messages = it.messages + tempUiMessage) }

        viewModelScope.launch {
            val result = sendMessageUseCase(
                channelId = channelId,
                text = text,
                attachments = attachments
            )

            if (result.isSuccess) {
                val sentMessage = result.getOrThrow()
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.map {
                        if (it.localId == tempUiMessage.localId) sentMessage.toUiModel(currentSenderId) { sentMessage.id }
                        else it
                    }
                    currentState.copy(messages = updatedMessages.filterNot { it.formattedTimestamp == "전송 중..." && it.message == text })
                }
                 _uiState.update { it.copy(pendingMessageText = "") }
            } else {
                Log.e("ChatViewModel", "Failed to send message: ${result.exceptionOrNull()?.message}")
                _eventFlow.emit(ChatEvent.Error("메시지 전송 실패: ${result.exceptionOrNull()?.localizedMessage}"))
                _uiState.update { currentState ->
                    currentState.copy(messages = currentState.messages.filterNot { it.localId == tempUiMessage.localId })
                }
            }
        }
    }

    private fun mapUriToAttachmentType(uri: Uri): String {
        return "image"
    }

    fun onUserInputChange(text: String) {
        _uiState.update { it.copy(pendingMessageText = text) }
    }

    fun onAttachmentSelected(uris: List<Uri>) {
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uris) }
    }
    
    fun removeAttachment(uri: Uri) {
        _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris - uri) }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            val result = editMessageUseCase(
                channelId = channelId,
                messageId = messageId,
                newText = newText
            )
            if (result.isFailure) {
                Log.e("ChatViewModel", "Failed to edit message $messageId: ${result.exceptionOrNull()?.message}")
                _eventFlow.emit(ChatEvent.Error("메시지 수정 실패: ${result.exceptionOrNull()?.localizedMessage}"))
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val result = deleteMessageUseCase(channelId = channelId, messageId = messageId)
            if (result.isFailure) {
                Log.e("ChatViewModel", "Failed to delete message $messageId: ${result.exceptionOrNull()?.message}")
                _eventFlow.emit(ChatEvent.Error("메시지 삭제 실패: ${result.exceptionOrNull()?.localizedMessage}"))
            }
        }
    }
    
    fun onMessageLongClicked(messageId: String) {
        viewModelScope.launch {
             val message = _uiState.value.messages.find { it.chatId == messageId }
            if (message != null && message.userId == currentUserId && !message.isDeleted) {
                 _eventFlow.emit(ChatEvent.ShowMessageActions(messageId, message.message))
            } else if (message != null && message.isDeleted) {
            }
        }
    }

    fun clearSelectionMode() {
    }
    
    fun onImageLongClicked(imageId: String) {
    }

    fun loadGalleryImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGallery = true) }
            try {
                // getLocalGalleryImagesUseCase는 Result<List<MediaImage>>를 반환
                val result = getLocalGalleryImagesUseCase(1, 100)
                
                if (result.isSuccess) {
                    // Result에서 데이터 추출 후 변환
                    val mediaImages = result.getOrThrow()
                    val galleryImages = mediaImages.map { mediaImage ->
                        GalleryImageUiModel(
                            uri = mediaImage.contentPath, 
                            id = mediaImage.id,
                            isSelected = false
                        )
                    }
                    _uiState.update { it.copy(galleryImages = galleryImages, isLoadingGallery = false) }
                } else {
                    // 에러 처리
                    val error = result.exceptionOrNull()?.message ?: "갤러리 이미지를 불러올 수 없습니다."
                    _uiState.update { it.copy(error = error, isLoadingGallery = false) }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading gallery images", e)
                _uiState.update { it.copy(error = "갤러리 이미지를 불러올 수 없습니다.", isLoadingGallery = false) }
            }
        }
    }

    fun onGalleryImageSelected(imageId: String) {
        _uiState.update { currentState ->
            val updatedImages = currentState.galleryImages.map {
                if (it.id == imageId) it.copy(isSelected = !it.isSelected) else it
            }
            currentState.copy(galleryImages = updatedImages)
        }
    }
    
    fun getSelectedGalleryImageUris(): List<Uri> {
        return _uiState.value.galleryImages.filter { it.isSelected }.map { it.uri }
    }
    
    fun clearGallerySelection() {
         _uiState.update { currentState ->
            val updatedImages = currentState.galleryImages.map { it.copy(isSelected = false) }
            currentState.copy(galleryImages = updatedImages)
        }
    }

    fun onImagesSelected(uris: List<Uri>) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ImagesSelected(uris))
            _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uris) }
        }
    }

    fun loadMoreMessages() {
        loadMorePastMessages()
    }

    fun onBackClick() {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.NavigateBack)
        }
    }

    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(pendingMessageText = text) }
    }

    fun confirmEditMessage() {
        val message = _uiState.value.pendingMessageText
        val messageId = _uiState.value.editingMessageId?.toString() ?: return
        
        viewModelScope.launch {
            editMessage(messageId, message)
            _uiState.update { it.copy(isEditing = false, editingMessageId = null, pendingMessageText = "") }
        }
    }

    fun onSendMessageClick() {
        val message = _uiState.value.pendingMessageText
        val attachments = _uiState.value.selectedAttachmentUris
        
        if (message.isBlank() && attachments.isEmpty()) return
        
        sendMessage(message, attachments)
        _uiState.update { it.copy(pendingMessageText = "", selectedAttachmentUris = emptyList()) }
    }

    fun onAttachmentClick() {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.AttachmentClicked)
            _uiState.update { it.copy(isAttachmentAreaVisible = !it.isAttachmentAreaVisible) }
            if (_uiState.value.isAttachmentAreaVisible && _uiState.value.galleryImages.isEmpty()) {
                loadGalleryImages()
            }
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ImageSelected(uri))
            _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris + uri) }
        }
    }

    fun onImageDeselected(uri: Uri) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ImageDeselected(uri))
            _uiState.update { it.copy(selectedAttachmentUris = it.selectedAttachmentUris - uri) }
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(isEditing = false, editingMessageId = null, pendingMessageText = "") }
    }

    fun onMessageLongClick(message: ChatMessageUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowMessageActions(message.chatId, message.message))
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
}