package com.example.feature_chat.logging

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 간소화된 채팅 로거 - 표준 Android Log로 대체
 * 이전 ChatLogger API 호환성을 위한 스텁 클래스
 */
@Singleton
class ChatLogger @Inject constructor() {
    
    companion object {
        // 로그 카테고리 (호환성용)
        const val CATEGORY_WEBSOCKET = "WEBSOCKET"
        const val CATEGORY_FIREBASE = "FIREBASE"
        const val CATEGORY_UI = "UI"
        const val CATEGORY_MESSAGE = "MESSAGE"
        const val CATEGORY_CONNECTION = "CONNECTION"
        const val CATEGORY_ERROR = "ERROR"
        const val CATEGORY_TEST = "TEST"
    }
    
    fun logInfo(
        category: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        Log.i("Chat_$category", formatMessage(message, userId, roomId, messageId, metadata))
    }
    
    fun logWarning(
        category: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        Log.w("Chat_$category", formatMessage(message, userId, roomId, messageId, metadata))
    }
    
    fun logError(
        category: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        val formattedMessage = formatMessage(message, userId, roomId, messageId, metadata)
        if (throwable != null) {
            Log.e("Chat_$category", formattedMessage, throwable)
        } else {
            Log.e("Chat_$category", formattedMessage)
        }
    }
    
    fun logDebug(
        category: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        Log.d("Chat_$category", formatMessage(message, userId, roomId, messageId, metadata))
    }
    
    // WebSocket 특화 로깅 메서드들 (호환성용)
    fun logWebSocketConnection(success: Boolean, serverUrl: String, userId: String?) {
        val status = if (success) "SUCCESS" else "FAILED"
        Log.i("Chat_$CATEGORY_WEBSOCKET", "WebSocket 연결 $status | URL: $serverUrl | User: $userId")
    }
    
    fun logWebSocketMessage(
        action: String,
        messageId: String,
        roomId: String,
        userId: String?,
        success: Boolean = true
    ) {
        val status = if (success) "SUCCESS" else "FAILED"
        Log.i("Chat_$CATEGORY_MESSAGE", "WebSocket 메시지 $action $status | Room: $roomId | User: $userId | Msg: $messageId")
    }
    
    fun logFirebaseUpdate(
        operation: String,
        collection: String,
        documentId: String,
        success: Boolean,
        userId: String?
    ) {
        val status = if (success) "SUCCESS" else "FAILED"
        Log.i("Chat_$CATEGORY_FIREBASE", "Firebase $operation $status | Collection: $collection | Doc: $documentId | User: $userId")
    }
    
    fun logTestResult(
        testName: String,
        success: Boolean,
        details: String = "",
        duration: Long? = null
    ) {
        val status = if (success) "PASSED" else "FAILED"
        val durationStr = duration?.let { " (${it}ms)" } ?: ""
        Log.i("Chat_$CATEGORY_TEST", "테스트 $testName $status$durationStr | $details")
    }
    
    private fun formatMessage(
        message: String,
        userId: String?,
        roomId: String?,
        messageId: String?,
        metadata: Map<String, String>
    ): String {
        return buildString {
            append(message)
            userId?.let { append(" | User: $it") }
            roomId?.let { append(" | Room: $it") }
            messageId?.let { append(" | Msg: $it") }
            if (metadata.isNotEmpty()) {
                append(" | Meta: $metadata")
            }
        }
    }
}