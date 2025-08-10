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
    
    /** 이미지 메시지 */
    const val MESSAGE_TYPE_IMAGE = "IMAGE"
    
    /** 파일 메시지 */
    const val MESSAGE_TYPE_FILE = "FILE"
    
    /** 시스템 날짜 메시지 */
    const val MESSAGE_TYPE_SYSTEM_DATE = "SYSTEM_DATE"
    
    /** 시스템 프로젝트 입장 메시지 */
    const val MESSAGE_TYPE_SYSTEM_PROJECT_JOIN = "SYSTEM_PROJECT_JOIN"
    
    /** 시스템 프로젝트 퇴장 메시지 */
    const val MESSAGE_TYPE_SYSTEM_PROJECT_LEAVE = "SYSTEM_PROJECT_LEAVE"
    
    /** 시스템 사용자 초대 메시지 */
    const val MESSAGE_TYPE_SYSTEM_USER_INVITE = "SYSTEM_USER_INVITE"

    // ================================
    // 페이로드 키 상수 (서버와 일치)
    // ================================

    /** 메시지 텍스트 내용 */
    const val PAYLOAD_CONTENT = "content"

    /** 메시지 제목 */
    const val PAYLOAD_TITLE = "title"

    /** 메시지 부제목 */
    const val PAYLOAD_SUBTITLE = "subtitle"

    /** 이미지 URL */
    const val PAYLOAD_IMAGE_URL = "imageUrl"

    /** 파일 URL */
    const val PAYLOAD_FILE_URL = "fileUrl"

    /** 파일 이름 */
    const val PAYLOAD_FILE_NAME = "fileName"

    /** 파일 크기 */
    const val PAYLOAD_FILE_SIZE = "fileSize"

    /** 파일 타입 */
    const val PAYLOAD_FILE_TYPE = "fileType"

    /** MIME 타입 */
    const val PAYLOAD_MIME_TYPE = "mimeType"

    /** 썸네일 URL */
    const val PAYLOAD_THUMBNAIL_URL = "thumbnailUrl"

    /** 업로드 진행률 */
    const val PAYLOAD_UPLOAD_PROGRESS = "uploadProgress"

    // ================================
    // 시스템 메시지 페이로드 키
    // ================================

    /** 시스템 메시지 타입 */
    const val PAYLOAD_SYSTEM_TYPE = "systemType"

    /** 관련 사용자 ID */
    const val PAYLOAD_USER_ID = "userId"

    /** 관련 사용자 이름 */
    const val PAYLOAD_USER_NAME = "userName"

    /** 액션 타입 */
    const val PAYLOAD_ACTION_TYPE = "actionType"

    /** 대상 사용자 ID */
    const val PAYLOAD_TARGET_USER_ID = "targetUserId"

    /** 대상 사용자 이름 */
    const val PAYLOAD_TARGET_USER_NAME = "targetUserName"

    // ================================
    // 프로젝트 관련 페이로드 키
    // ================================

    /** 프로젝트 이름 */
    const val PAYLOAD_PROJECT_NAME = "projectName"

    /** 채널 이름 */
    const val PAYLOAD_CHANNEL_NAME = "channelName"

    /** 초대 코드 */
    const val PAYLOAD_INVITE_CODE = "inviteCode"

    /** 역할/권한 */
    const val PAYLOAD_ROLE = "role"

    // ================================
    // 메타데이터 페이로드 키
    // ================================

    /** 클라이언트 정보 */
    const val PAYLOAD_CLIENT_INFO = "clientInfo"

    /** 디바이스 타입 */
    const val PAYLOAD_DEVICE_TYPE = "deviceType"

    /** 앱 버전 */
    const val PAYLOAD_APP_VERSION = "appVersion"

    /** 타임스탬프 메타데이터 */
    const val PAYLOAD_TIMESTAMP_META = "timestampMeta"

    // ================================
    // 오류 관련 페이로드 키
    // ================================

    /** 오류 메시지 */
    const val PAYLOAD_ERROR_MESSAGE = "errorMessage"

    /** 오류 코드 */
    const val PAYLOAD_ERROR_CODE = "errorCode"

    /** 오류 상세 정보 */
    const val PAYLOAD_ERROR_DETAILS = "errorDetails"

    /** 스택 트레이스 */
    const val PAYLOAD_STACK_TRACE = "stackTrace"

    // ================================
    // 알림 관련 페이로드 키
    // ================================

    /** 알림 제목 */
    const val PAYLOAD_NOTIFICATION_TITLE = "notificationTitle"

    /** 알림 내용 */
    const val PAYLOAD_NOTIFICATION_BODY = "notificationBody"

    /** 알림 아이콘 */
    const val PAYLOAD_NOTIFICATION_ICON = "notificationIcon"

    /** 알림 액션 */
    const val PAYLOAD_NOTIFICATION_ACTION = "notificationAction"

    // ================================
    // 인증 관련 페이로드 키
    // ================================

    /** 인증 토큰 */
    const val PAYLOAD_AUTH_TOKEN = "authToken"

    /** 리프레시 토큰 */
    const val PAYLOAD_REFRESH_TOKEN = "refreshToken"

    /** 토큰 만료 시간 */
    const val PAYLOAD_TOKEN_EXPIRES_AT = "tokenExpiresAt"

    // ================================
    // 기타 통신 관련 페이로드 키
    // ================================

    /** 요청 ID */
    const val PAYLOAD_REQUEST_ID = "requestId"

    /** 응답 상태 */
    const val PAYLOAD_RESPONSE_STATUS = "responseStatus"

    /** 추가 데이터 */
    const val PAYLOAD_EXTRA_DATA = "extraData"

    /** 설정 정보 */
    const val PAYLOAD_CONFIG_DATA = "configData"

    // ================================
    // 유틸리티 함수
    // ================================

    /**
     * 메시지 타입이 텍스트인지 확인
     */
    fun isTextMessage(messageType: String?): Boolean {
        return messageType == MESSAGE_TYPE_TEXT
    }

    /**
     * 메시지 타입이 시스템 메시지인지 확인
     */
    fun isSystemMessage(messageType: String?): Boolean {
        return messageType in setOf(
            MESSAGE_TYPE_SYSTEM,
            MESSAGE_TYPE_SYSTEM_DATE,
            MESSAGE_TYPE_SYSTEM_PROJECT_JOIN,
            MESSAGE_TYPE_SYSTEM_PROJECT_LEAVE,
            MESSAGE_TYPE_SYSTEM_USER_INVITE
        )
    }

    /**
     * 메시지 타입이 미디어(이미지/파일)인지 확인
     */
    fun isMediaMessage(messageType: String?): Boolean {
        return messageType in setOf(MESSAGE_TYPE_IMAGE, MESSAGE_TYPE_FILE)
    }

    /**
     * 페이로드에서 텍스트 콘텐츠 추출
     */
    fun getTextContent(payload: Map<String, String>?): String? {
        return payload?.get(PAYLOAD_CONTENT)
    }

    /**
     * 텍스트 콘텐츠가 포함된 페이로드 생성
     */
    fun createTextPayload(content: String): Map<String, String> {
        return mapOf(PAYLOAD_CONTENT to content)
    }

    /**
     * 시스템 메시지 페이로드 생성
     */
    fun createSystemPayload(
        systemType: String,
        content: String,
        userId: String? = null,
        userName: String? = null
    ): Map<String, String> {
        return buildMap {
            put(PAYLOAD_CONTENT, content)
            put(PAYLOAD_SYSTEM_TYPE, systemType)
            userId?.let { put(PAYLOAD_USER_ID, it) }
            userName?.let { put(PAYLOAD_USER_NAME, it) }
        }
    }

    /**
     * 파일 메시지 페이로드 생성
     */
    fun createFilePayload(
        fileName: String,
        fileUrl: String,
        fileSize: Long? = null,
        fileType: String? = null,
        mimeType: String? = null
    ): Map<String, String> {
        return buildMap {
            put(PAYLOAD_FILE_NAME, fileName)
            put(PAYLOAD_FILE_URL, fileUrl)
            fileSize?.let { put(PAYLOAD_FILE_SIZE, it.toString()) }
            fileType?.let { put(PAYLOAD_FILE_TYPE, it) }
            mimeType?.let { put(PAYLOAD_MIME_TYPE, it) }
        }
    }

    /**
     * 이미지 메시지 페이로드 생성
     */
    fun createImagePayload(
        imageUrl: String,
        thumbnailUrl: String? = null,
        fileName: String? = null
    ): Map<String, String> {
        return buildMap {
            put(PAYLOAD_IMAGE_URL, imageUrl)
            thumbnailUrl?.let { put(PAYLOAD_THUMBNAIL_URL, it) }
            fileName?.let { put(PAYLOAD_FILE_NAME, it) }
        }
    }
}