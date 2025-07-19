package com.example.feature_chat.model

import android.net.Uri

/**
 * 채팅 화면에서 발생하는 일회성 이벤트들을 정의하는 sealed class
 */
sealed class ChatEvent {
    object ScrollToBottom : ChatEvent()
    data class ShowEditDeleteDialog(val message: ChatMessageUiModel) : ChatEvent()
    data class ShowUserProfileDialog(val userId: String) : ChatEvent()
    data class ShowSnackbar(val message: String) : ChatEvent()
    object ClearFocus : ChatEvent() // 키보드 숨기기 요청 등
    object NavigateBack : ChatEvent() // 뒤로 가기 네비게이션 요청
    
    // 새로 추가된 이벤트
    data class Error(val message: String) : ChatEvent() // 오류 발생 알림
    data class ShowMessageActions(val messageId: String, val messageText: String) : ChatEvent() // 메시지 액션 표시 (편집/삭제)
    data class ImagesSelected(val uris: List<Uri>) : ChatEvent() // 이미지 선택됨
    object AttachmentClicked : ChatEvent() // 첨부 버튼 클릭
    data class ImageSelected(val uri: Uri) : ChatEvent() // 단일 이미지 선택
    data class ImageDeselected(val uri: Uri) : ChatEvent() // 단일 이미지 선택 취소
    data class SystemMessage(val content: String) : ChatEvent() // 시스템 메시지
} 