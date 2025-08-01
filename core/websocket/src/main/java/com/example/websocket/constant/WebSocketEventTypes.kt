package com.example.websocket.constant

/**
 * WebSocket 이벤트 타입 상수 정의
 *
 * 클라이언트와 서버 간 WebSocket 통신에서 사용되는 모든 이벤트 타입을 중앙에서 관리한다.
 * 이를 통해 코드의 일관성을 유지하고 오타로 인한 오류를 방지한다.
 */
object WebSocketEventTypes {

    // ================================
    // 기본 메시지 이벤트
    // ================================

    /** 일반 채팅 메시지 전송 */
    const val MESSAGE = "MESSAGE"

    /** 메시지 수정 */
    const val EDIT_MESSAGE = "EDIT_MESSAGE"

    /** 메시지 삭제 */
    const val DELETE_MESSAGE = "DELETE_MESSAGE"

    // ================================
    // 메시지 처리 확인 (ACK) 이벤트
    // ================================

    /** 메시지 전송 성공 확인 */
    const val MESSAGE_ACK = "MESSAGE_ACK"

    /** 메시지 수정 성공 확인 */
    const val EDIT_MESSAGE_ACK = "EDIT_MESSAGE_ACK"

    /** 메시지 삭제 성공 확인 */
    const val DELETE_MESSAGE_ACK = "DELETE_MESSAGE_ACK"

    // ================================
    // 메시지 처리 실패 이벤트
    // ================================

    /** 메시지 전송 실패 */
    const val MESSAGE_FAILED = "MESSAGE_FAILED"

    /** 메시지 수정 실패 */
    const val EDIT_MESSAGE_FAILED = "EDIT_MESSAGE_FAILED"

    /** 메시지 삭제 실패 */
    const val DELETE_MESSAGE_FAILED = "DELETE_MESSAGE_FAILED"

    // ================================
    // 방 관리 이벤트
    // ================================

    /** 방 입장 요청 */
    const val JOIN_ROOM = "JOIN_ROOM"

    /** 방 퇴장 요청 */
    const val LEAVE_ROOM = "LEAVE_ROOM"

    /** 방 입장 완료 알림 */
    const val JOINED_ROOM = "JOINED_ROOM"

    /** 방 퇴장 완료 알림 */
    const val LEFT_ROOM = "LEFT_ROOM"

    /** 다른 사용자가 방에 입장함 */
    const val USER_JOINED_ROOM = "USER_JOINED_ROOM"

    /** 다른 사용자가 방을 떠남 */
    const val USER_LEFT_ROOM = "USER_LEFT_ROOM"

    // ================================
    // 인증 관련 이벤트
    // ================================

    /** 인증 요청 */
    const val AUTH = "AUTH"

    /** 인증 성공 */
    const val AUTH_SUCCESS = "AUTH_SUCCESS"

    /** 인증 실패 */
    const val AUTH_FAILED = "AUTH_FAILED"

    /** 토큰 갱신 요청 */
    const val REFRESH_TOKEN = "REFRESH_TOKEN"

    /** 토큰 갱신 성공 */
    const val TOKEN_REFRESHED = "TOKEN_REFRESHED"

    // ================================
    // 연결 관리 이벤트
    // ================================

    /** 연결 상태 확인 (Ping) */
    const val PING = "PING"

    /** 연결 상태 응답 (Pong) */
    const val PONG = "PONG"

    /** 연결 성공 */
    const val CONNECTED = "CONNECTED"

    /** 연결 해제 */
    const val DISCONNECTED = "DISCONNECTED"

    /** 재연결 요청 */
    const val RECONNECT = "RECONNECT"

    // ================================
    // 시스템 이벤트
    // ================================

    /** 일반 오류 */
    const val ERROR = "ERROR"

    /** 시스템 메시지 */
    const val SYSTEM = "SYSTEM"

    /** 서버 공지사항 */
    const val ANNOUNCEMENT = "ANNOUNCEMENT"

    /** 서버 점검 알림 */
    const val MAINTENANCE = "MAINTENANCE"

    /** 강제 로그아웃 */
    const val FORCE_LOGOUT = "FORCE_LOGOUT"

    // ================================
    // 사용자 상태 이벤트
    // ================================

    /** 사용자 온라인 상태 */
    const val USER_ONLINE = "USER_ONLINE"

    /** 사용자 오프라인 상태 */
    const val USER_OFFLINE = "USER_OFFLINE"

    /** 사용자 타이핑 중 */
    const val USER_TYPING = "USER_TYPING"

    /** 사용자 타이핑 중지 */
    const val USER_STOP_TYPING = "USER_STOP_TYPING"

    // ================================
    // 파일 및 미디어 이벤트
    // ================================

    /** 파일 업로드 시작 */
    const val FILE_UPLOAD_START = "FILE_UPLOAD_START"

    /** 파일 업로드 진행 상황 */
    const val FILE_UPLOAD_PROGRESS = "FILE_UPLOAD_PROGRESS"

    /** 파일 업로드 완료 */
    const val FILE_UPLOAD_COMPLETE = "FILE_UPLOAD_COMPLETE"

    /** 파일 업로드 실패 */
    const val FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED"

    /** 이미지 메시지 */
    const val IMAGE_MESSAGE = "IMAGE_MESSAGE"

    /** 파일 메시지 */
    const val FILE_MESSAGE = "FILE_MESSAGE"

    // ================================
    // 알림 이벤트
    // ================================

    /** 새 알림 */
    const val NOTIFICATION = "NOTIFICATION"

    /** 알림 읽음 표시 */
    const val NOTIFICATION_READ = "NOTIFICATION_READ"

    /** 알림 설정 변경 */
    const val NOTIFICATION_SETTINGS_CHANGED = "NOTIFICATION_SETTINGS_CHANGED"

    // ================================
    // 프로젝트 관련 이벤트
    // ================================

    /** 프로젝트 생성 */
    const val PROJECT_CREATED = "PROJECT_CREATED"

    /** 프로젝트 수정 */
    const val PROJECT_UPDATED = "PROJECT_UPDATED"

    /** 프로젝트 삭제 */
    const val PROJECT_DELETED = "PROJECT_DELETED"

    /** 프로젝트 멤버 추가 */
    const val PROJECT_MEMBER_ADDED = "PROJECT_MEMBER_ADDED"

    /** 프로젝트 멤버 제거 */
    const val PROJECT_MEMBER_REMOVED = "PROJECT_MEMBER_REMOVED"

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 메시지 관련 이벤트인지 확인
     */
    fun isMessageEvent(eventType: String): Boolean {
        return eventType in setOf(
            MESSAGE, EDIT_MESSAGE, DELETE_MESSAGE,
            MESSAGE_ACK, EDIT_MESSAGE_ACK, DELETE_MESSAGE_ACK,
            MESSAGE_FAILED, EDIT_MESSAGE_FAILED, DELETE_MESSAGE_FAILED
        )
    }

    /**
     * 방 관리 이벤트인지 확인
     */
    fun isRoomEvent(eventType: String): Boolean {
        return eventType in setOf(
            JOIN_ROOM, LEAVE_ROOM, JOINED_ROOM, LEFT_ROOM,
            USER_JOINED_ROOM, USER_LEFT_ROOM
        )
    }

    /**
     * 인증 관련 이벤트인지 확인
     */
    fun isAuthEvent(eventType: String): Boolean {
        return eventType in setOf(
            AUTH, AUTH_SUCCESS, AUTH_FAILED,
            REFRESH_TOKEN, TOKEN_REFRESHED, FORCE_LOGOUT
        )
    }

    /**
     * 시스템 이벤트인지 확인
     */
    fun isSystemEvent(eventType: String): Boolean {
        return eventType in setOf(
            ERROR, SYSTEM, ANNOUNCEMENT, MAINTENANCE,
            PING, PONG, CONNECTED, DISCONNECTED, RECONNECT
        )
    }

    /**
     * ACK 이벤트인지 확인
     */
    fun isAckEvent(eventType: String): Boolean {
        return eventType in setOf(
            MESSAGE_ACK, EDIT_MESSAGE_ACK, DELETE_MESSAGE_ACK
        )
    }

    /**
     * 실패 이벤트인지 확인
     */
    fun isFailedEvent(eventType: String): Boolean {
        return eventType in setOf(
            MESSAGE_FAILED, EDIT_MESSAGE_FAILED, DELETE_MESSAGE_FAILED,
            AUTH_FAILED, FILE_UPLOAD_FAILED
        )
    }

    /**
     * 파일 관련 이벤트인지 확인
     */
    fun isFileEvent(eventType: String): Boolean {
        return eventType in setOf(
            FILE_UPLOAD_START, FILE_UPLOAD_PROGRESS, FILE_UPLOAD_COMPLETE, FILE_UPLOAD_FAILED,
            IMAGE_MESSAGE, FILE_MESSAGE
        )
    }

    /**
     * 사용자 상태 이벤트인지 확인
     */
    fun isUserStatusEvent(eventType: String): Boolean {
        return eventType in setOf(
            USER_ONLINE, USER_OFFLINE, USER_TYPING, USER_STOP_TYPING
        )
    }
}