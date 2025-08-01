package com.example.websocket

/**
 * WebSocket 이벤트 타입 통합 상수 관리
 * 클라이언트와 서버에서 공통으로 사용하는 WebSocket 이벤트 타입들을 정의
 *
 * 이벤트 타입 변경 시 이 파일에서만 수정하면 전체 시스템에 적용됨
 */
object WebSocketEventTypes {

    // ================================
    // 기본 연결 및 인증 이벤트
    // ================================

    /** 인증 요청 */
    const val AUTH = "AUTH"

    /** 인증 성공 알림 */
    const val AUTH_SUCCESS = "AUTH_SUCCESS"

    /** 방 입장 요청 */
    const val JOIN_ROOM = "JOIN_ROOM"

    /** 방 나가기 요청 */
    const val LEAVE_ROOM = "LEAVE_ROOM"

    /** 방 입장 성공 알림 */
    const val JOINED_ROOM = "JOINED_ROOM"

    /** 방 나가기 성공 알림 */
    const val LEFT_ROOM = "LEFT_ROOM"

    // ================================
    // 메시지 관련 이벤트
    // ================================

    /** 일반 메시지 전송 */
    const val MESSAGE = "MESSAGE"

    /** 메시지 편집 */
    const val EDIT_MESSAGE = "EDIT_MESSAGE"

    /** 메시지 삭제 */
    const val DELETE_MESSAGE = "DELETE_MESSAGE"

    // ================================
    // ACK (성공 응답) 이벤트
    // ================================

    /** 메시지 전송 성공 확인 */
    const val MESSAGE_ACK = "MESSAGE_ACK"

    /** 메시지 편집 성공 확인 */
    const val EDIT_MESSAGE_ACK = "EDIT_MESSAGE_ACK"

    /** 메시지 삭제 성공 확인 */
    const val DELETE_MESSAGE_ACK = "DELETE_MESSAGE_ACK"

    // ================================
    // FAILED (실패 응답) 이벤트
    // ================================

    /** 메시지 전송 실패 알림 */
    const val MESSAGE_FAILED = "MESSAGE_FAILED"

    /** 메시지 편집 실패 알림 */
    const val EDIT_MESSAGE_FAILED = "EDIT_MESSAGE_FAILED"

    /** 메시지 삭제 실패 알림 */
    const val DELETE_MESSAGE_FAILED = "DELETE_MESSAGE_FAILED"

    // ================================
    // 시스템 이벤트
    // ================================

    /** 시스템 메시지 */
    const val SYSTEM = "SYSTEM"

    /** 에러 메시지 */
    const val ERROR = "ERROR"

    /** 하트비트/핑 */
    const val HEARTBEAT = "HEARTBEAT"

    /** 핑 */
    const val PING = "PING"

    /** 퐁 응답 */
    const val PONG = "PONG"

    /** 일반 ACK */
    const val ACK = "ACK"

    // ================================
    // 채널 타입 상수
    // ================================

    /** DM (Direct Message) 채널 */
    const val CHANNEL_TYPE_DM = "DM"

    /** 프로젝트 채널 */
    const val CHANNEL_TYPE_PROJECT = "PROJECT"

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * ACK 이벤트인지 확인
     */
    fun isAckEvent(eventType: String): Boolean {
        return eventType in setOf(MESSAGE_ACK, EDIT_MESSAGE_ACK, DELETE_MESSAGE_ACK)
    }

    /**
     * FAILED 이벤트인지 확인
     */
    fun isFailedEvent(eventType: String): Boolean {
        return eventType in setOf(MESSAGE_FAILED, EDIT_MESSAGE_FAILED, DELETE_MESSAGE_FAILED)
    }

    /**
     * 메시지 관련 이벤트인지 확인
     */
    fun isMessageEvent(eventType: String): Boolean {
        return eventType in setOf(MESSAGE, EDIT_MESSAGE, DELETE_MESSAGE)
    }

    /**
     * 시스템 이벤트인지 확인
     */
    fun isSystemEvent(eventType: String): Boolean {
        return eventType in setOf(SYSTEM, ERROR, JOINED_ROOM, LEFT_ROOM, AUTH_SUCCESS)
    }
}