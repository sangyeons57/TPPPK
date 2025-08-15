package com.example.websocket.util

import android.net.Uri
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.websocket.constant.WebSocketFieldConstants

/**
 * 간단한 메시지 타입 감지 유틸리티
 */
object MessageTypeDetector {

    /**
     * 간단한 메시지 타입 정보
     */
    data class MessageTypeInfo(
        val messageType: String,
        val hasImages: Boolean = false
    )

    /**
     * 간단한 메시지 타입 감지
     */
    fun detectMessageType(
        textContent: String,
        imageUris: List<Uri> = emptyList(),
        isSystemMessage: Boolean = false,
        isProjectInvite: Boolean = false
    ): MessageTypeInfo {

        if (isProjectInvite) {
            return MessageTypeInfo(
                messageType = WebSocketFieldConstants.MESSAGE_TYPE_PROJECT_INVITE
            )
        }

        if (isSystemMessage) {
            return MessageTypeInfo(
                messageType = WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            )
        }

        val hasImages = imageUris.isNotEmpty()
        val messageType =
            WebSocketFieldConstants.MESSAGE_TYPE_TEXT // Always TEXT, images handled via attachments

        return MessageTypeInfo(
            messageType = messageType,
            hasImages = hasImages
        )
    }

    /**
     * 페이로드를 위한 기본적인 정보 생성 (레거시 호환성)
     */
    fun createBasicPayload(
        textContent: String,
        images: List<Map<String, Any?>> = emptyList(),
        additionalData: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        return buildMap {
            if (textContent.isNotEmpty()) {
                put(MessagePayload.KEY_CONTENT, textContent)
            }
            if (images.isNotEmpty()) {
                put(MessagePayload.KEY_ATTACHMENTS, images)
            }
            additionalData.forEach { (key, value) ->
                put(key, value)
            }
        }
    }

    /**
     * MessagePayload를 생성하는 헬퍼 메서드
     */
    fun createMessagePayload(
        textContent: String,
        images: List<Map<String, Any?>> = emptyList(),
        additionalData: Map<String, Any?> = emptyMap()
    ): MessagePayload {
        return if (images.isNotEmpty()) {
            MessagePayload.forTextWithAttachments(textContent, images)
        } else {
            var payload = MessagePayload.forText(textContent)

            // 추가 데이터가 있으면 병합
            if (additionalData.isNotEmpty()) {
                payload = payload.withValues(additionalData)
            }

            payload
        }
    }


    /**
     * MessageType을 WebSocket 문자열로 변환
     */
    fun messageTypeToWebSocketString(messageType: MessageType): String {
        return when (messageType) {
            MessageType.TEXT -> WebSocketFieldConstants.MESSAGE_TYPE_TEXT
            MessageType.SYSTEM -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.SYSTEM_DATE -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.SYSTEM_PROJECT_JOIN -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.SYSTEM_PROJECT_LEAVE -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.SYSTEM_CHAT_START -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.SYSTEM_MEMBER_INVITATION -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.SYSTEM_USER_INVITE -> WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            MessageType.PROJECT_INVITE -> WebSocketFieldConstants.MESSAGE_TYPE_PROJECT_INVITE
        }
    }

    /**
     * 메시지가 특정 타입인지 확인하는 헬퍼 메서드들
     *
     * 주의: MessageType은 Message 엔티티에서 별도 관리되므로
     * payload 기반 타입 감지는 제한적으로만 사용해야 합니다.
     */

    fun isProjectInviteMessage(payload: MessagePayload): Boolean {
        return try {
            val jsonObject = payload.asJsonObject()
            jsonObject.containsKey("projectId") &&
                    jsonObject.containsKey("invitationId") &&
                    jsonObject.containsKey("inviterName")
        } catch (e: Exception) {
            false
        }
    }

    fun isImageMessage(payload: MessagePayload): Boolean {
        return payload.hasAttachments() && payload.getAttachments().any {
            (it[MessagePayload.KEY_KIND] as? String) == "image"
        }
    }

    fun isTextMessage(payload: MessagePayload): Boolean {
        return !isProjectInviteMessage(payload) &&
                !isImageMessage(payload)
    }
}