package com.example.feature_chat.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 통합 로깅을 위한 유틸리티 클래스
 * Android Studio 로그와 Google Cloud Function 로그 간의 상관관계 식별을 위한 도구
 */
object ChatLogUtils {
    
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    
    /**
     * 시간 기반 상관관계 ID 생성
     * 형식: YYYY-MM-DD_HH-mm-ss-SSS_XXXX
     * 
     * @return 고유한 상관관계 ID
     */
    fun generateCorrelationId(): String {
        val timestamp = LocalDateTime.now().format(timeFormatter)
        val randomSuffix = Random.nextInt(1000, 9999)
        return "${timestamp}_$randomSuffix"
    }
    
    /**
     * 웹소켓 관련 로그를 위한 표준 태그
     */
    const val TAG_WEBSOCKET = "Chat_WebSocket"
    
    /**
     * Firebase 관련 로그를 위한 표준 태그
     */
    const val TAG_FIREBASE = "Chat_Firebase"
    
    /**
     * 메시지 관련 로그를 위한 표준 태그
     */
    const val TAG_MESSAGE = "Chat_Message"
    
    /**
     * 연결 상태 관련 로그를 위한 표준 태그
     */
    const val TAG_CONNECTION = "Chat_Connection"
    
    /**
     * 로그 메시지에 상관관계 ID와 메타데이터를 포함한 형식화된 메시지 생성
     * 
     * @param correlationId 상관관계 ID
     * @param message 기본 메시지
     * @param userId 사용자 ID (선택적)
     * @param roomId 방 ID (선택적)
     * @param messageId 메시지 ID (선택적)
     * @param metadata 추가 메타데이터 (선택적)
     * @return 형식화된 로그 메시지
     */
    fun formatLogMessage(
        correlationId: String,
        message: String,
        userId: String? = null,
        roomId: String? = null,
        messageId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): String {
        return buildString {
            append("[ID:$correlationId] $message")
            userId?.let { append(" | User:$it") }
            roomId?.let { append(" | Room:$it") }
            messageId?.let { append(" | Msg:$it") }
            if (metadata.isNotEmpty()) {
                append(" | Meta:$metadata")
            }
        }
    }
}