package com.example.feature_chat.model

import android.net.Uri

/**
 * ChatViewModel의 UI 상태를 관리하는 데이터 클래스
 */
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