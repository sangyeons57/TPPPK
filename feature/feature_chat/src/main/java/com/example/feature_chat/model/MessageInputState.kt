package com.example.feature_chat.model

import android.net.Uri

/**
 * 메시지 입력과 관련된 UI 상태를 관리하는 데이터 클래스
 */
data class MessageInputState(
    val pendingMessageText: String = "",
    val selectedAttachmentUris: List<Uri> = emptyList(),
    val isAttachmentAreaVisible: Boolean = false,
    val isEditing: Boolean = false,
    val editingMessageId: String? = null,
    val canSendMessage: Boolean = false
) {
    
    /**
     * 메시지를 전송할 수 있는지 확인
     */
    fun canSend(): Boolean {
        return pendingMessageText.isNotBlank() || selectedAttachmentUris.isNotEmpty()
    }
    
    /**
     * 편집 모드인지 확인
     */
    fun isInEditMode(): Boolean {
        return isEditing && editingMessageId != null
    }
    
    /**
     * 첨부 파일이 있는지 확인
     */
    fun hasAttachments(): Boolean {
        return selectedAttachmentUris.isNotEmpty()
    }
    
    companion object {
        /**
         * 초기 상태 생성
         */
        fun initial(): MessageInputState {
            return MessageInputState()
        }
        
        /**
         * 편집 모드로 전환
         */
        fun editMode(messageId: String, currentText: String): MessageInputState {
            return MessageInputState(
                pendingMessageText = currentText,
                isEditing = true,
                editingMessageId = messageId,
                selectedAttachmentUris = emptyList(),
                isAttachmentAreaVisible = false
            )
        }
    }
}