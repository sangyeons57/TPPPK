package com.example.feature_chat.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import com.example.domain.repository.ChatRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// --- Domain 모델 -> UI 모델 변환 함수 ---
// ViewModel 내부 또는 별도 Mapper 파일에 위치 가능
private fun ChatMessage.toUiModel(myUserId: Int, tempIdGenerator: () -> String): ChatMessageUiModel {
    // 시간 포맷팅 (오전/오후 h:mm 형식, 한국어)
    val formatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

    // UTC LocalDateTime을 사용자의 로컬 시간대로 변환
    val localSentAt = this.sentAt.atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()

    return ChatMessageUiModel(
        localId = tempIdGenerator(), // 실제 ID 또는 임시 ID 사용
        chatId = this.chatId,
        userId = this.userId,
        userName = this.userName,
        userProfileUrl = this.userProfileUrl,
        message = this.message,
        formattedTimestamp = localSentAt.format(formatter), // 변환된 로컬 시간 포맷팅
        isModified = this.isModified,
        attachmentImageUrls = this.attachmentImageUrls,
        isMyMessage = this.userId == myUserId
    )
}

private fun MediaImage.toUiModel(): GalleryImageUiModel {
    return GalleryImageUiModel(
        uri = Uri.parse(this.contentUri),
        id = this.id,
        isSelected = false
    )
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    // TODO: private val chatRepository: ChatRepository
) : ViewModel() {

    private val channelId: String = savedStateHandle["channelId"] ?: error("channelId가 필요합니다.")
    private val initialLastChatId: Int? = savedStateHandle["lastChatId"] // 처음 진입 시 기준 ID

    private val _uiState = MutableStateFlow(ChatUiState(channelId = channelId, channelName = "채팅방 $channelId")) // 초기 상태
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 임시 메시지 ID 생성을 위한 카운터 (실제 앱에서는 UUID 등 사용 권장)
    private var tempMessageLocalIdCounter = -1L

    init {
        observeMessages()
        fetchInitialMessages()
        // loadGalleryImages() // 필요 시 갤러리 이미지 로드
    }

    // 메시지 스트림 구독 및 UI 상태 업데이트
    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.getMessagesStream(channelId)
                .map { domainMessages ->
                    // Domain 모델 리스트 -> UI 모델 리스트 변환
                    domainMessages.map { it.toUiModel(uiState.value.myUserId) { "temp_${tempMessageLocalIdCounter--}" } }
                }
                .catch { e -> _uiState.update { it.copy(error = "메시지 스트림 오류: ${e.message}") } }
                .collect { uiMessages ->
                    // TODO: 정렬 로직 개선 (chatId, 임시 ID, 시간 등 고려)
                    _uiState.update { it.copy(messages = uiMessages, isLoadingHistory = false) } // 로딩 상태 해제
                    // 새 메시지 수신 시 스크롤 (조건 추가 가능)
                    if (uiMessages.isNotEmpty()) {
                        _eventFlow.emit(ChatEvent.ScrollToBottom)
                    }
                }
        }
    }


    // 초기 메시지 로드 (과거 데이터 가져오기)
    private fun fetchInitialMessages() {
        if (uiState.value.isLoadingHistory) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            println("ViewModel: Fetching initial messages for channel $channelId")
            // 가장 오래된 메시지 ID 또는 시간 기준으로 과거 메시지 요청
            val oldestMessageId = uiState.value.messages.minByOrNull { it.chatId }?.chatId ?: Int.MAX_VALUE // 예시
            val result = chatRepository.fetchPastMessages(channelId, beforeMessageId = oldestMessageId, limit = 20) // 첫 로드 시 적절한 limit

            if (result.isFailure) {
                _uiState.update { it.copy(error = "초기 메시지 로드 실패", isLoadingHistory = false) }
            } else {
                // 성공 시 Flow가 DB 변경을 감지하여 업데이트하므로, 여기서는 로딩 상태만 해제
                _uiState.update { it.copy(isLoadingHistory = false, isLastPage = result.getOrNull()?.isEmpty() ?: false) }
            }
        }
    }

    // 과거 메시지 추가 로드
    fun loadMoreMessages() {
        if (uiState.value.isLoadingHistory || uiState.value.isLastPage) return // 로딩 중이거나 마지막 페이지면 중지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            val oldestMessageId = uiState.value.messages.minByOrNull { it.chatId }?.chatId ?: Int.MAX_VALUE
            println("ViewModel: Loading more messages before $oldestMessageId")

            val result = chatRepository.fetchPastMessages(channelId, beforeMessageId = oldestMessageId, limit = 20) // 추가 로드 limit

            if (result.isSuccess) {
                val fetchedMessages = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        isLastPage = fetchedMessages.isEmpty() // 더 이상 가져올 메시지가 없으면 마지막 페이지
                    )
                    // 성공 시 Flow가 DB 변경을 감지하여 업데이트하므로, 여기서는 상태만 업데이트
                }
            } else {
                _uiState.update { it.copy(isLoadingHistory = false, error = "과거 메시지 로드 실패") }
                _eventFlow.emit(ChatEvent.ShowSnackbar("과거 메시지를 불러오는 데 실패했습니다."))
            }
        }
    }

    // 메시지 입력 변경
    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(messageInput = text) }
    }

    // 메시지 전송
    fun onSendMessageClick() {
        val messageToSend = uiState.value.messageInput.trim()
        val selectedImages = uiState.value.selectedImages.toList()
        val myUserId = uiState.value.myUserId

        if (messageToSend.isBlank() && selectedImages.isEmpty()) return
        if (uiState.value.isSendingMessage) return // 중복 전송 방지

        // 1. 임시 UI 모델 생성 (즉각적인 UI 피드백용)
        val tempLocalId = "temp_${tempMessageLocalIdCounter--}"
        val tempMessage = ChatMessageUiModel(
            localId = tempLocalId,
            chatId = 0, // 아직 서버 ID 없음
            userId = myUserId,
            userName = "나", // 실제 사용자 이름 사용 필요
            userProfileUrl = null, // 실제 사용자 프로필 URL 사용 필요
            message = messageToSend,
            formattedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)),
            isModified = false,
            attachmentImageUrls = selectedImages.map { it.toString() }, // 임시 URL 표시
            isMyMessage = true,
            isSending = true, // 전송 중 상태 표시
            sendFailed = false
        )

        // 2. UI 상태 업데이트 (임시 메시지 추가)
        _uiState.update {
            it.copy(
                messages = listOf(tempMessage) + it.messages, // 맨 앞에 추가
                messageInput = "",
                selectedImages = emptySet(),
                isAttachmentAreaVisible = false,
                isSendingMessage = true // 전체적인 전송 상태 플래그 (선택적)
            )
        }
        viewModelScope.launch { _eventFlow.emit(ChatEvent.ScrollToBottom) } // 스크롤

        // 3. 실제 메시지 전송 요청 (Repository 호출)
        viewModelScope.launch {
            val result = chatRepository.sendMessage(channelId, messageToSend, selectedImages)

            // 4. 결과 처리
            if (result.isSuccess) {
                // 성공: Flow가 DB 변경을 감지하고 실제 메시지로 업데이트할 것을 기대.
                // 또는 여기서 직접 임시 메시지를 제거하고 실제 메시지 데이터(result.getOrThrow())로 교체할 수도 있음.
                // 여기서는 Flow에 맡기고 전송 상태만 해제.
                _uiState.update { it.copy(isSendingMessage = false) } // 전체 전송 상태 해제
                // 개별 메시지 상태 업데이트는 Flow를 통해 처리되도록 기대
            } else {
                // 실패: 임시 메시지의 상태를 '실패'로 변경하고 스낵바 표시
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { if (it.localId == tempLocalId) it.copy(isSending = false, sendFailed = true) else it },
                        isSendingMessage = false
                    )
                }
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 전송 실패: ${result.exceptionOrNull()?.message}"))
            }
            _eventFlow.emit(ChatEvent.ClearFocus)
        }
    }


    // --- 갤러리 및 첨부 관련 ---

    fun onAttachmentClick() {
        _uiState.update { it.copy(isAttachmentAreaVisible = !it.isAttachmentAreaVisible) }
    }

    // 갤러리 이미지 로드
    fun loadGalleryImages(page: Int = 0) { // 페이징 지원 예시
        viewModelScope.launch {
            // TODO: 갤러리 로딩 상태 관리 (필요시 uiState에 추가)
            val result = chatRepository.getLocalGalleryImages(page, pageSize = 50) // 페이지 크기 조정
            if (result.isSuccess) {
                val newImages = result.getOrThrow().map { it.toUiModel() }
                _uiState.update { currentState ->
                    // 기존 이미지와 합치되 중복 제거 (ID 기준)
                    val updatedImages = (currentState.galleryImages + newImages).distinctBy { it.id }
                    currentState.copy(galleryImages = updatedImages)
                }
            } else {
                _eventFlow.emit(ChatEvent.ShowSnackbar("갤러리 이미지 로드 실패"))
            }
        }
    }


    fun onImagesSelected(uris: List<Uri>) { // 여러 이미지 선택 (런처 결과)
        _uiState.update {
            it.copy(selectedImages = it.selectedImages + uris)
        }
    }

    fun onImageSelected(uri: Uri) {
        // 이미 선택된 이미지는 선택 해제
        if (uiState.value.selectedImages.contains(uri)) {
            onImageDeselected(uri)
            return
        }
        
        _uiState.update { 
            it.copy(
                selectedImages = it.selectedImages + uri,
                galleryImages = it.galleryImages.map { galleryImage -> 
                    if (galleryImage.uri == uri) galleryImage.copy(isSelected = true) else galleryImage 
                }
            )
        }
    }

    fun onImageDeselected(uri: Uri) {
        _uiState.update { 
            it.copy(
                selectedImages = it.selectedImages - uri,
                galleryImages = it.galleryImages.map { galleryImage -> 
                    if (galleryImage.uri == uri) galleryImage.copy(isSelected = false) else galleryImage 
                }
            )
        }
    }


    // --- 메시지 수정/삭제 관련 ---

    fun onMessageLongClick(message: ChatMessageUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowEditDeleteDialog(message))
        }
    }

    fun startEditMessage(messageId: Int, text: String) {
        _uiState.update { 
            it.copy(
                messageInput = text,
                isEditing = true,
                editingMessageId = messageId
            )
        }
    }

    // 메시지 편집 취소
    fun cancelEdit() {
        _uiState.update { 
            it.copy(
                messageInput = "",
                isEditing = false,
                editingMessageId = null
            )
        }
    }

    // 메시지 편집 완료
    fun confirmEditMessage() {
        val editingId = uiState.value.editingMessageId ?: return
        val newMessage = uiState.value.messageInput.trim()
        
        if (newMessage.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지를 입력해주세요."))
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true) }
            
            val result = chatRepository.editMessage(channelId, editingId, newMessage)
            
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        messageInput = "",
                        isEditing = false,
                        editingMessageId = null,
                        isSendingMessage = false
                    )
                }
            } else {
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 수정에 실패했습니다."))
            }
        }
    }


    fun confirmDeleteMessage(messageId: Int) {
        viewModelScope.launch {
            val result = chatRepository.deleteMessage(channelId, messageId)
            
            if (result.isFailure) {
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 삭제에 실패했습니다."))
            }
            // 성공 시 Flow가 DB 변경을 감지하여 자동으로 UI 업데이트
        }
    }

    // --- 기타 ---
    fun onUserProfileClick(userId: Int) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowUserProfileDialog(userId))
        }
    }
}