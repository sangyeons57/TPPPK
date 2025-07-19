package com.example.feature_chat.model

import android.net.Uri
import com.example.core_common.websocket.WebSocketConnectionState

/**
 * ChatViewModel의 UI 상태를 관리하는 데이터 클래스
 */
data class ChatUiState(
    val channelId: String = "", // 생성자에서 초기화되므로 non-null
    val channelName: String = "채팅방",
    val channelPath: String = "",
    val messages: List<ChatMessageUiModel> = emptyList(), // ★ UI 모델 사용
    val messageInput: String = "",
    val isAttachmentAreaVisible: Boolean = false,
    val galleryImages: List<GalleryImageUiModel> = emptyList(), // ★ UI 모델 사용
    val selectedImages: Set<Uri> = emptySet(),
    val isLoadingHistory: Boolean = false, // ★ 이름 명확화: 과거 메시지 로딩
    val isSendingMessage: Boolean = false, // ★ 이름 명확화: 메시지 전송 중
    val isEditing: Boolean = false,
    val editingMessageId: Int? = null,
    val myUserId: String = "", // 실제로는 외부에서 주입 또는 설정 필요
    val myUserNameDisplay: String? = null, // For optimistic UI updates
    val myUserProfileUrl: String? = null, // For optimistic UI updates
    val isLastPage: Boolean = false,
    val error: String? = null,
    // Added for ChatViewModel refactor
    val pendingMessageText: String = "", 
    val selectedAttachmentUris: List<Uri> = emptyList(),
    val isLoadingGallery: Boolean = false,
    
    // WebSocket connection state
    val connectionState: WebSocketConnectionState = WebSocketConnectionState.Disconnected,
    val queuedMessagesCount: Int = 0,
    val showConnectionError: Boolean = false
) 