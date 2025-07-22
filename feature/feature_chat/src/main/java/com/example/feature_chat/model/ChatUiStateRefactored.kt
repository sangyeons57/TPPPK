package com.example.feature_chat.model

import java.time.Instant

/**
 * 리팩토링된 ChatViewModel의 UI 상태를 관리하는 데이터 클래스
 * 각 영역별로 명확하게 분리된 상태 관리
 */
data class ChatUiStateRefactored(
    // 기본 채널 정보
    val channelId: String = "",
    val channelName: String = "채팅방",
    val channelPath: String = "",
    
    // 메시지 관련 상태
    val messages: List<ChatMessageUiModel> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val lastMessageTimestamp: Instant? = null,
    
    // 메시지 입력 상태 (새로운 모델 클래스 사용)
    val messageInputState: MessageInputState = MessageInputState.initial(),
    
    // 연결 상태 (새로운 모델 클래스 사용)
    val connectionState: ConnectionState = ConnectionState.initial(),
    
    // 사용자 정보
    val currentUserId: String? = null,
    val myUserId: String = "",
    val myUserNameDisplay: String? = null,
    val myUserProfileUrl: String? = null,
    
    // 갤러리 및 첨부파일
    val galleryImages: List<GalleryImageUiModel> = emptyList(),
    val isLoadingGallery: Boolean = false,
    
    // 프로필 로딩 상태
    val loadingProfileUserIds: Set<String> = emptySet(),
    
    // 오류 상태
    val error: String? = null,
    
    // 페이지네이션
    val isLastPage: Boolean = false
) {
    
    /**
     * 메시지 전송이 가능한지 확인
     */
    fun canSendMessage(): Boolean {
        return currentUserId != null && 
               connectionState.canPerformWriteOperations() && 
               messageInputState.canSend()
    }
    
    /**
     * 편집 모드인지 확인
     */
    fun isInEditMode(): Boolean {
        return messageInputState.isInEditMode()
    }
    
    /**
     * 연결되어 있는지 확인
     */
    fun isConnected(): Boolean {
        return connectionState.isConnected()
    }
    
    /**
     * 읽기 전용 모드인지 확인
     */
    fun isReadOnlyMode(): Boolean {
        return currentUserId == null || connectionState.isReadOnlyMode()
    }
    
    /**
     * 오류가 있는지 확인
     */
    fun hasError(): Boolean {
        return error != null || connectionState.hasError()
    }
    
    /**
     * 로딩 중인지 확인
     */
    fun isLoading(): Boolean {
        return isLoadingHistory || isLoadingMoreMessages || isLoadingGallery
    }
    
    /**
     * 대기 중인 메시지가 있는지 확인
     */
    fun hasQueuedMessages(): Boolean {
        return connectionState.hasQueuedMessages()
    }
    
    companion object {
        /**
         * 초기 상태 생성
         */
        fun initial(channelId: String = "", channelName: String = "채팅방"): ChatUiStateRefactored {
            return ChatUiStateRefactored(
                channelId = channelId,
                channelName = channelName,
                isLoadingHistory = true
            )
        }
    }
}