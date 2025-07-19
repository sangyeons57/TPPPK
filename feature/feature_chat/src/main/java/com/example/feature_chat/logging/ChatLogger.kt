package com.example.feature_chat.logging

import android.util.Log
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import com.google.cloud.logging.Payload
import com.google.cloud.logging.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChatLogEvent(
    val timestamp: String,
    val level: String,
    val category: String,
    val message: String,
    val metadata: Map<String, String> = emptyMap(),
    val userId: String? = null,
    val roomId: String? = null,
    val messageId: String? = null
)

@Singleton
class ChatLogger @Inject constructor() {
    
    companion object {
        private const val TAG = "ChatLogger"
        private const val LOG_NAME = "android-chat-app"
        
        // 로그 카테고리
        const val CATEGORY_WEBSOCKET = "WEBSOCKET"
        const val CATEGORY_FIREBASE = "FIREBASE"
        const val CATEGORY_UI = "UI"
        const val CATEGORY_MESSAGE = "MESSAGE"
        const val CATEGORY_CONNECTION = "CONNECTION"
        const val CATEGORY_ERROR = "ERROR"
        const val CATEGORY_TEST = "TEST"
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    
    // Google Cloud Logging (lazy initialization for performance)
    private val cloudLogging: Logging? by lazy {
        try {
            LoggingOptions.getDefaultInstance().service
        } catch (e: Exception) {
            Log.w(TAG, "Google Cloud Logging 초기화 실패: ${e.message}")
            null
        }
    }
    
    fun logInfo(
        category: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        log(LogLevel.INFO, category, message, metadata, userId, roomId, messageId)
    }
    
    fun logWarning(
        category: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        log(LogLevel.WARNING, category, message, metadata, userId, roomId, messageId)
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
        val errorMetadata = metadata.toMutableMap()
        throwable?.let {
            errorMetadata["exception"] = it.javaClass.simpleName
            errorMetadata["stackTrace"] = it.stackTraceToString()
        }
        log(LogLevel.ERROR, category, message, errorMetadata, userId, roomId, messageId)
    }
    
    fun logDebug(
        category: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null
    ) {
        log(LogLevel.DEBUG, category, message, metadata, userId, roomId, messageId)
    }
    
    private fun log(
        level: LogLevel,
        category: String,
        message: String,
        metadata: Map<String, String>,
        userId: String?,
        roomId: String?,
        messageId: String?
    ) {
        val logEvent = ChatLogEvent(
            timestamp = Instant.now().toString(),
            level = level.name,
            category = category,
            message = message,
            metadata = metadata,
            userId = userId,
            roomId = roomId,
            messageId = messageId
        )
        
        // Android Log 출력
        logToAndroid(level, category, message, logEvent)
        
        // Google Cloud Logging 출력 (비동기)
        logToGoogleCloud(level, logEvent)
    }
    
    private fun logToAndroid(level: LogLevel, category: String, message: String, logEvent: ChatLogEvent) {
        val tag = "Chat_$category"
        val formattedMessage = buildString {
            append("[$category] $message")
            if (logEvent.userId != null) append(" | User: ${logEvent.userId}")
            if (logEvent.roomId != null) append(" | Room: ${logEvent.roomId}")
            if (logEvent.messageId != null) append(" | Msg: ${logEvent.messageId}")
            if (logEvent.metadata.isNotEmpty()) {
                append(" | Meta: ${logEvent.metadata}")
            }
        }
        
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, formattedMessage)
            LogLevel.INFO -> Log.i(tag, formattedMessage)
            LogLevel.WARNING -> Log.w(tag, formattedMessage)
            LogLevel.ERROR -> Log.e(tag, formattedMessage)
        }
    }
    
    private fun logToGoogleCloud(level: LogLevel, logEvent: ChatLogEvent) {
        coroutineScope.launch {
            try {
                cloudLogging?.let { logging ->
                    val severity = when (level) {
                        LogLevel.DEBUG -> Severity.DEBUG
                        LogLevel.INFO -> Severity.INFO
                        LogLevel.WARNING -> Severity.WARNING
                        LogLevel.ERROR -> Severity.ERROR
                    }
                    
                    val logEntry = LogEntry.newBuilder(Payload.JsonPayload.of(json.encodeToString(logEvent)))
                        .setSeverity(severity)
                        .setLogName(LOG_NAME)
                        .setResource(com.google.cloud.MonitoredResource.newBuilder("android_app").build())
                        .build()
                    
                    logging.write(listOf(logEntry))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Google Cloud Logging 전송 실패: ${e.message}")
            }
        }
    }
    
    // WebSocket 특화 로깅 메서드들
    fun logWebSocketConnection(success: Boolean, serverUrl: String, userId: String?) {
        val status = if (success) "SUCCESS" else "FAILED"
        logInfo(
            CATEGORY_WEBSOCKET, 
            "WebSocket 연결 $status",
            mapOf("serverUrl" to serverUrl, "status" to status),
            userId = userId
        )
    }
    
    fun logWebSocketMessage(
        action: String, // SEND, RECEIVE, EDIT, DELETE
        messageId: String,
        roomId: String,
        userId: String?,
        success: Boolean = true
    ) {
        val level = if (success) LogLevel.INFO else LogLevel.ERROR
        val status = if (success) "SUCCESS" else "FAILED"
        log(
            level,
            CATEGORY_MESSAGE,
            "WebSocket 메시지 $action $status",
            mapOf("action" to action, "status" to status),
            userId = userId,
            roomId = roomId,
            messageId = messageId
        )
    }
    
    fun logFirebaseUpdate(
        operation: String, // CREATE, UPDATE, DELETE
        collection: String,
        documentId: String,
        success: Boolean,
        userId: String?
    ) {
        val level = if (success) LogLevel.INFO else LogLevel.ERROR
        val status = if (success) "SUCCESS" else "FAILED"
        logInfo(
            CATEGORY_FIREBASE,
            "Firebase $operation $status",
            mapOf(
                "operation" to operation,
                "collection" to collection,
                "documentId" to documentId,
                "status" to status
            ),
            userId = userId
        )
    }
    
    fun logTestResult(
        testName: String,
        success: Boolean,
        details: String = "",
        duration: Long? = null
    ) {
        val level = if (success) LogLevel.INFO else LogLevel.ERROR
        val status = if (success) "PASSED" else "FAILED"
        val metadata = mutableMapOf(
            "testName" to testName,
            "status" to status
        )
        duration?.let { metadata["duration"] = "${it}ms" }
        if (details.isNotEmpty()) metadata["details"] = details
        
        log(
            level,
            CATEGORY_TEST,
            "테스트 $testName $status",
            metadata,
            null, null, null
        )
    }
    
    private enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
}