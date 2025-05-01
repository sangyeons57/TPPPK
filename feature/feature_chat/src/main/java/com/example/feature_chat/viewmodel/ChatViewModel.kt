package com.example.teamnovapersonalprojectprojectingkotlin.feature_chat.viewmodel

import android.R
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.ChatMessage
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.MediaImage
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random // 임시 ID 생성을 위해 추가

// --- UI 전용 데이터 모델 (ViewModel 내 정의 또는 별도 파일) ---
// UI에 채팅 메시지를 어떻게 보여줄지 정의
data class ChatMessageUiModel(
    val localId: String, // LazyColumn Key 및 임시 ID용 (String으로 변경하여 임시/실제 ID 모두 처리)
    val chatId: Int, // 서버 ID (아직 없으면 0 또는 음수)
    val userId: Int,
    val userName: String,
    val userProfileUrl: String?,
    val message: String,
    val formattedTimestamp: String, // UI 표시용 포맷된 시간
    val isModified: Boolean,
    val attachmentImageUrls: List<String> = emptyList(),
    val isMyMessage: Boolean,
    val isSending: Boolean = false, // 메시지 전송 중 상태 (UI 피드백용)
    val sendFailed: Boolean = false // 메시지 전송 실패 상태 (UI 피드백용)
)

data class GalleryImageUiModel( // GalleryImage 대체
    val uri: Uri,
    val id: Long,
    var isSelected: Boolean = false // 선택 상태 추가 (UI 관리용)
)


// --- Domain 모델 -> UI 모델 변환 함수 ---
// ViewModel 내부 또는 별도 Mapper 파일에 위치 가능
private fun ChatMessage.toUiModel(myUserId: Int, tempIdGenerator: () -> String): ChatMessageUiModel {
    val formatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    return ChatMessageUiModel(
        localId = this.chatId.toString(), // 실제 ID 사용
        chatId = this.chatId,
        userId = this.userId,
        userName = this.userName,
        userProfileUrl = this.userProfileUrl,
        message = this.message,
        formattedTimestamp = this.sentAt.format(formatter),
        isModified = this.isModified,
        attachmentImageUrls = this.attachmentImageUrls,
        isMyMessage = this.userId == myUserId
    )
}
private fun MediaImage.toUiModel(): GalleryImageUiModel {
    return GalleryImageUiModel(
        uri = this.contentUri,
        id = this.id,
        isSelected = false,

    )
}

// --- UI 상태 ---
data class ChatUiState(
    val channelId: String = "", // 생성자에서 초기화되므로 non-null
    val channelName: String = "채팅방",
    val messages: List<ChatMessageUiModel> = emptyList(), // ★ UI 모델 사용
    val messageInput: String = "",
    val isAttachmentAreaVisible: Boolean = false,
    val galleryImages: List<GalleryImageUiModel> = emptyList(), // ★ UI 모델 사용
    val selectedImages: Set<Uri> = emptySet(),
    val isLoadingHistory: Boolean = false, // ★ 이름 명확화: 과거 메시지 로딩
    val isSendingMessage: Boolean = false, // ★ 이름 명확화: 메시지 전송 중
    val isEditing: Boolean = false,
    val editingMessageId: Int? = null,
    val myUserId: Int = 1, // 실제로는 외부에서 주입 또는 설정 필요
    val isLastPage: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class ChatEvent {
    object ScrollToBottom : ChatEvent()
    data class ShowEditDeleteDialog(val message: ChatMessageUiModel) : ChatEvent()
    data class ShowUserProfileDialog(val userId: Int) : ChatEvent()
    data class ShowSnackbar(val message: String) : ChatEvent()
    object ClearFocus : ChatEvent() // 키보드 숨기기 요청 등
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
        val shouldLoad = !uiState.value.isAttachmentAreaVisible
        _uiState.update { it.copy(isAttachmentAreaVisible = !it.isAttachmentAreaVisible) }
        if (shouldLoad && uiState.value.galleryImages.isEmpty()) { // 처음 열 때만 로드 (또는 새로고침 로직 추가)
            loadGalleryImages()
        }
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

    fun onImageSelected(uri: Uri) { // 그리드에서 이미지 선택
        _uiState.update { it.copy(selectedImages = it.selectedImages + uri) }
    }

    fun onImageDeselected(uri: Uri) { // 그리드 또는 미리보기에서 선택 해제
        _uiState.update { it.copy(selectedImages = it.selectedImages - uri) }
    }


    // --- 메시지 수정/삭제 관련 ---

    fun onMessageLongClick(message: ChatMessageUiModel) {
        // TODO: 내 메시지인지, 수정/삭제 가능한 상태인지 등 확인 로직 추가 가능
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowEditDeleteDialog(message))
        }
    }

    fun startEditMessage(chatId: Int, currentText: String) {
        // TODO: 수정하려는 메시지가 유효한지 추가 확인 가능
        _uiState.update {
            it.copy(
                isEditing = true,
                editingMessageId = chatId,
                messageInput = currentText // 입력 필드에 수정할 텍스트 설정
            )
        }
        // TODO: 키보드 표시 요청 필요 시 이벤트 발생
    }

    // 수정 취소
    fun cancelEdit() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editingMessageId = null,
                messageInput = "" // 입력 필드 초기화
            )
        }
    }

    // TODO: 메시지 수정 완료 로직 (별도 함수 또는 onSendMessageClick 재활용)
    fun confirmEditMessage() {
        if (!uiState.value.isEditing || uiState.value.editingMessageId == null) return

        val chatId = uiState.value.editingMessageId!!
        val newMessage = uiState.value.messageInput.trim()

        if (newMessage.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 내용을 입력해주세요.")) }
            return
        }
        // TODO: 원본 메시지와 비교하여 변경 여부 확인

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true) } // 로딩 표시 (isSendingMessage 재활용)
            val result = chatRepository.editMessage(channelId, chatId, newMessage)
            if (result.isSuccess) {
                // 성공 시 Flow가 업데이트하거나, 직접 UI 상태 업데이트
                _uiState.update {
                    it.copy(
                        isEditing = false,
                        editingMessageId = null,
                        messageInput = "",
                        isSendingMessage = false
                        // messages = it.messages.map { msg -> if(msg.chatId == chatId) msg.copy(message = newMessage, isModified = true) else msg } // 직접 업데이트 예시
                    )
                }
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지가 수정되었습니다."))
            } else {
                _uiState.update { it.copy(isSendingMessage = false) }
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 수정 실패"))
            }
            _eventFlow.emit(ChatEvent.ClearFocus)
        }
    }


    fun confirmDeleteMessage(chatId: Int) {
        if (uiState.value.isLoadingHistory) return // 삭제 중복 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) } // 로딩 상태 사용 (이름 변경 고려)
            println("ViewModel: Deleting message $chatId")
            val result = chatRepository.deleteMessage(channelId, chatId)

            if (result.isSuccess) {
                // 성공 시: Flow가 DB 변경을 감지하여 UI 업데이트할 것을 기대.
                // 또는 여기서 직접 목록 필터링:
                // _uiState.update { state ->
                //     state.copy(messages = state.messages.filterNot { it.chatId == chatId })
                // }
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지가 삭제되었습니다."))
            } else {
                _eventFlow.emit(ChatEvent.ShowSnackbar("메시지 삭제 실패"))
            }
            _uiState.update { it.copy(isLoadingHistory = false) } // 로딩 해제
        }
    }

    // --- 기타 ---
    fun onUserProfileClick(userId: Int) {
        viewModelScope.launch {
            _eventFlow.emit(ChatEvent.ShowUserProfileDialog(userId))
        }
    }
}