package com.example.websocket.util

import android.net.Uri
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
        isSystemMessage: Boolean = false
    ): MessageTypeInfo {

        if (isSystemMessage) {
            return MessageTypeInfo(
                messageType = WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM
            )
        }

        val hasImages = imageUris.isNotEmpty()
        val messageType = if (hasImages) {
            WebSocketFieldConstants.MESSAGE_TYPE_IMAGE
        } else {
            WebSocketFieldConstants.MESSAGE_TYPE_TEXT
        }

        return MessageTypeInfo(
            messageType = messageType,
            hasImages = hasImages
        )
    }

    /**
     * 페이로드를 위한 기본적인 정보 생성
     */
    fun createBasicPayload(
        textContent: String,
        images: List<Map<String, Any?>> = emptyList(),
        additionalData: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        return buildMap {
            if (textContent.isNotEmpty()) {
                put(WebSocketFieldConstants.PAYLOAD_CONTENT, textContent)
            }
            if (images.isNotEmpty()) {
                put("attachments", images)
            }
            additionalData.forEach { (key, value) ->
                put(key, value)
            }
        }
    }
}