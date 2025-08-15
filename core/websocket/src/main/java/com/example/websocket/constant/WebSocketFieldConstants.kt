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
    
    /** 시스템 메시지 */
    const val MESSAGE_TYPE_SYSTEM = "SYSTEM"
    
    /** 시스템 날짜 메시지 */
    const val MESSAGE_TYPE_SYSTEM_DATE = "SYSTEM_DATE"
    
    /** 시스템 프로젝트 입장 메시지 */
    const val MESSAGE_TYPE_SYSTEM_PROJECT_JOIN = "SYSTEM_PROJECT_JOIN"
    
    /** 시스템 프로젝트 퇴장 메시지 */
    const val MESSAGE_TYPE_SYSTEM_PROJECT_LEAVE = "SYSTEM_PROJECT_LEAVE"
    
    /** 시스템 사용자 초대 메시지 */
    const val MESSAGE_TYPE_SYSTEM_USER_INVITE = "SYSTEM_USER_INVITE"

    /** 시스템 멤버 초대 메시지 (프로젝트 멤버 초대 안내) */
    const val MESSAGE_TYPE_SYSTEM_MEMBER_INVITATION = "SYSTEM_MEMBER_INVITATION"

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

    fun isSystemMessage(messageType: String?): Boolean = messageType in setOf(
        MESSAGE_TYPE_SYSTEM,
        MESSAGE_TYPE_SYSTEM_DATE,
        MESSAGE_TYPE_SYSTEM_PROJECT_JOIN,
        MESSAGE_TYPE_SYSTEM_PROJECT_LEAVE,
        MESSAGE_TYPE_SYSTEM_USER_INVITE,
        MESSAGE_TYPE_SYSTEM_MEMBER_INVITATION,
        MESSAGE_TYPE_PROJECT_INVITE
    )

    fun isMediaMessage(messageType: String?): Boolean =
        false // No longer using separate IMAGE/FILE types
}
