package com.example.websocket.constant

/**
 * WebSocket 필드 및 페이로드 키 상수 관리 (클라이언트 측)
 *
 * 서버의 WebSocketEventConstants.java 및 PayloadConstants.java와 일치하는
 * 클라이언트 측 상수들을 정의한다.
 * 
 * 서버와 클라이언트 간 일관성을 보장하고 통신 오류를 방지한다.
 */
object WebSocketFieldConstants {

    // ================================
    // JSON 필드 이름 상수 (서버와 일치)
    // ================================
    
    /** 메시지 타입 필드 */
    const val FIELD_TYPE = "type"
    
    /** 방 ID 필드 */
    const val FIELD_ROOM_ID = "roomId"
    
    /** 발신자 ID 필드 */
    const val FIELD_SENDER_ID = "senderId"
    
    /** 메시지 타입 필드 */
    const val FIELD_MESSAGE_TYPE = "messageType"
    
    /** 타임스탬프 필드 */
    const val FIELD_TIMESTAMP = "timestamp"


    /** 메시지 ID 필드 */
    const val FIELD_MESSAGE_ID = "messageId"
    
    /** 답장 대상 메시지 ID 필드 */
    const val FIELD_REPLY_TO_MESSAGE_ID = "replyToMessageId"
    
    /** 페이로드 필드 */
    const val FIELD_PAYLOAD = "payload"

    /** 중첩 메시지 필드 (도메인 Message 래핑) */
    const val FIELD_MESSAGE = "message"

    /** 멘션 리스트 필드 (NestedMessage 관할) */
    const val FIELD_MENTIONS = "mentions"

    const val FIELD_MENTIONS_FIELD_TYPE = "type"
    const val FIELD_MENTIONS_FIELD_ID = "id"
    const val FIELD_MENTIONS_FIELD_DISPLAY_NAME = "displayName"

    /** 프로젝트 ID 필드 */
    const val FIELD_PROJECT_ID = "projectId"
    
    /** 채널 타입 필드 */
    const val FIELD_CHANNEL_TYPE = "channelType"
    
    /** 인증 토큰 필드 */
    const val FIELD_AUTH_TOKEN = "authToken"
    
    /** 오류 코드 필드 */
    const val FIELD_ERROR_CODE = "errorCode"
    
    /** 메타데이터 필드 */
    const val FIELD_METADATA = "metadata"

    // ================================
    // 메시지 타입 상수 (서버와 일치)
    // ================================
    
    /** 텍스트 메시지 */
    const val MESSAGE_TYPE_TEXT = "TEXT"

    // 시스템 관련 메시지 타입은 사용하지 않음

    /** 프로젝트 초대 메시지 */
    const val MESSAGE_TYPE_PROJECT_INVITE = "PROJECT_INVITE"

    // ================================
    // 프로젝트 초대 메시지 전용 필드 상수
    // ================================

    /** 초대 ID 필드 */
    const val FIELD_INVITATION_ID = "invitationId"

    /** 프로젝트 이름 필드 */
    const val FIELD_PROJECT_NAME = "projectName"

    /** 초대자 이름 필드 */
    const val FIELD_INVITER_NAME = "inviterName"

    // ================================
    // 유틸리티 함수 (메시지 타입 판별만 보유)
    // ================================

    fun isTextMessage(messageType: String?): Boolean = messageType == MESSAGE_TYPE_TEXT

    fun isSystemMessage(messageType: String?): Boolean = false

    fun isMediaMessage(messageType: String?): Boolean =
        false // No longer using separate IMAGE/FILE types
}
