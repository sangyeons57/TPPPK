package com.example.feature_chat.model

/**
 * 채팅 화면에서 발생하는 일회성 이벤트들을 정의하는 sealed class
 */
sealed class ChatEvent {
    object ScrollToBottom : ChatEvent()
    data class ShowEditDeleteDialog(val message: ChatMessageUiModel) : ChatEvent()
    data class ShowUserProfileDialog(val userId: Int) : ChatEvent()
    data class ShowSnackbar(val message: String) : ChatEvent()
    object ClearFocus : ChatEvent() // 키보드 숨기기 요청 등
    object NavigateBack : ChatEvent() // 뒤로 가기 네비게이션 요청
} 