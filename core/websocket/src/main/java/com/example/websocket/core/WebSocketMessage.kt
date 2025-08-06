package com.example.websocket.core

import kotlinx.serialization.Serializable

/**
 * WebSocket을 통해 송수신되는 메시지의 데이터 클래스
 *
 * JSON 직렬화/역직렬화가 가능하며, 클라이언트-서버 간 통신에 사용된다.
 * 다양한 메시지 타입을 지원하여 채팅, 시스템 알림, 인증 등의 기능을 처리한다.
 */
@Serializable
data class WebSocketMessage(
    /**
     * 메시지 타입 (필수)
     *
     * 가능한 값들:
     * - MESSAGE: 일반 채팅 메시지
     * - EDIT_MESSAGE: 메시지 수정
     * - DELETE_MESSAGE: 메시지 삭제
     * - MESSAGE_ACK: 메시지 처리 확인
     * - EDIT_MESSAGE_ACK: 메시지 수정 확인
     * - DELETE_MESSAGE_ACK: 메시지 삭제 확인
     * - JOIN_ROOM: 방 입장
     * - LEAVE_ROOM: 방 퇴장
     * - JOINED_ROOM: 방 입장 완료 알림
     * - LEFT_ROOM: 방 퇴장 완료 알림
     * - AUTH: 인증 요청
     * - AUTH_SUCCESS: 인증 성공
     * - ERROR: 오류 메시지
     * - SYSTEM: 시스템 메시지
     */
    val type: String,

    /**
     * 방 ID (옵션)
     *
     * 메시지가 속한 채팅방이나 그룹의 식별자
     */
    val roomId: String? = null,

    /**
     * 발신자 ID (옵션)
     *
     * 메시지를 보낸 사용자의 식별자
     */
    val senderId: String? = null,

    /**
     * 메시지 내용 (옵션, DEPRECATED)
     *
     * 채팅 메시지의 텍스트 내용이나 기타 데이터
     * 새로운 구현에서는 payload 사용을 권장함
     */
    val content: String? = null,

    /**
     * 메시지 타입 (옵션)
     *
     * 메시지의 종류를 나타냄 (TEXT, SYSTEM_DATE, SYSTEM_PROJECT_JOIN 등)
     */
    val messageType: String? = "TEXT",

    /**
     * 메시지 페이로드 (옵션)
     *
     * JSON 형태의 메시지 내용 - content 필드를 대체하는 새로운 형식
     */
    val payload: Map<String, String>? = null,

    /**
     * 메시지 ID (옵션)
     *
     * 개별 메시지의 고유 식별자 (수정/삭제 시 사용)
     */
    val messageId: String? = null,

    /**
     * 답장 대상 메시지 ID (옵션)
     *
     * 이 메시지가 답장하는 원본 메시지의 ID
     */
    val replyToMessageId: String? = null,

    /**
     * 타임스탬프 (옵션)
     *
     * 메시지가 생성된 시간 (Unix epoch seconds)
     */
    val timestamp: Double? = null,

    /**
     * 프로젝트 ID (옵션)
     *
     * 메시지가 속한 프로젝트의 식별자
     */
    val projectId: String? = null,

    /**
     * 채널 타입 (옵션)
     *
     * 메시지가 전송된 채널의 타입 (예: TEXT, VOICE, etc.)
     */
    val channelType: String? = null,

    /**
     * 인증 토큰 (옵션)
     *
     * 인증 메시지에서 사용되는 JWT 토큰
     */
    val authToken: String? = null,

    /**
     * 오류 코드 (옵션)
     *
     * 오류 메시지에서 사용되는 오류 코드
     */
    val errorCode: String? = null,

    /**
     * 추가 메타데이터 (옵션)
     *
     * 기타 필요한 정보를 담는 키-값 맵
     */
    val metadata: Map<String, String>? = null
) {

    companion object {
        // ================================
        // 메시지 타입 상수
        // ================================

        // 기본 메시지 타입
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_EDIT_MESSAGE = "EDIT_MESSAGE"
        const val TYPE_DELETE_MESSAGE = "DELETE_MESSAGE"

        // ACK 메시지 타입
        const val TYPE_MESSAGE_ACK = "MESSAGE_ACK"
        const val TYPE_EDIT_MESSAGE_ACK = "EDIT_MESSAGE_ACK"
        const val TYPE_DELETE_MESSAGE_ACK = "DELETE_MESSAGE_ACK"

        // FAILED 메시지 타입
        const val TYPE_MESSAGE_FAILED = "MESSAGE_FAILED"
        const val TYPE_EDIT_MESSAGE_FAILED = "EDIT_MESSAGE_FAILED"
        const val TYPE_DELETE_MESSAGE_FAILED = "DELETE_MESSAGE_FAILED"

        // 방 관리 메시지 타입
        const val TYPE_JOIN_ROOM = "JOIN_ROOM"
        const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
        const val TYPE_JOINED_ROOM = "JOINED_ROOM"
        const val TYPE_LEFT_ROOM = "LEFT_ROOM"

        // 인증 메시지 타입
        const val TYPE_AUTH = "AUTH"
        const val TYPE_AUTH_SUCCESS = "AUTH_SUCCESS"

        // 시스템 메시지 타입
        const val TYPE_ERROR = "ERROR"
        const val TYPE_SYSTEM = "SYSTEM"


        // 채널 타입 상수
        const val CHANNEL_TYPE_PROJECT = "PROJECT"
        const val CHANNEL_TYPE_DM = "DM"

        // ================================
        // 메시지 생성 헬퍼 메서드
        // ================================

        /**
         * 채팅 메시지 생성
         */
        fun createChatMessage(
            roomId: String,
            senderId: String,
            content: String,
            messageId: String,
            replyToMessageId: String? = null,
            timestamp: Double? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_MESSAGE,
                roomId = roomId,
                senderId = senderId,
                content = content,
                messageId = messageId,
                replyToMessageId = replyToMessageId,
                timestamp = timestamp
            )
        }

        /**
         * 방 입장 메시지 생성
         */
        fun createJoinRoomMessage(
            roomId: String,
            senderId: String,
            timestamp: Double? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_JOIN_ROOM,
                roomId = roomId,
                senderId = senderId,
                timestamp = timestamp
            )
        }

        /**
         * 방 퇴장 메시지 생성
         */
        fun createLeaveRoomMessage(
            roomId: String,
            senderId: String,
            timestamp: Double? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_LEAVE_ROOM,
                roomId = roomId,
                senderId = senderId,
                timestamp = timestamp
            )
        }

        /**
         * 인증 메시지 생성
         */
        fun createAuthMessage(authToken: String): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_AUTH,
                authToken = authToken
            )
        }

        /**
         * 에러 메시지 생성
         */
        fun createErrorMessage(
            message: String,
            errorCode: String? = null,
            roomId: String? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_ERROR,
                content = message,
                errorCode = errorCode,
                roomId = roomId
            )
        }


    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 메시지가 채팅 메시지인지 확인
     */
    fun isChatMessage(): Boolean {
        return type in setOf(TYPE_MESSAGE, TYPE_EDIT_MESSAGE, TYPE_DELETE_MESSAGE)
    }

    /**
     * 메시지가 ACK 메시지인지 확인
     */
    fun isAckMessage(): Boolean {
        return type in setOf(TYPE_MESSAGE_ACK, TYPE_EDIT_MESSAGE_ACK, TYPE_DELETE_MESSAGE_ACK)
    }

    /**
     * 메시지가 FAILED 메시지인지 확인
     */
    fun isFailedMessage(): Boolean {
        return type in setOf(
            TYPE_MESSAGE_FAILED,
            TYPE_EDIT_MESSAGE_FAILED,
            TYPE_DELETE_MESSAGE_FAILED
        )
    }

    /**
     * 메시지가 방 관리 메시지인지 확인
     */
    fun isRoomMessage(): Boolean {
        return type in setOf(TYPE_JOIN_ROOM, TYPE_LEAVE_ROOM, TYPE_JOINED_ROOM, TYPE_LEFT_ROOM)
    }

    /**
     * 메시지가 인증 관련 메시지인지 확인
     */
    fun isAuthMessage(): Boolean {
        return type in setOf(TYPE_AUTH, TYPE_AUTH_SUCCESS)
    }

    /**
     * 메시지가 시스템 메시지인지 확인
     */
    fun isSystemMessage(): Boolean {
        return type in setOf(TYPE_ERROR, TYPE_SYSTEM)
    }

    /**
     * 메시지가 특정 방과 관련이 있는지 확인
     */
    fun isForRoom(targetRoomId: String): Boolean {
        return roomId == targetRoomId
    }

    /**
     * 메시지의 간단한 문자열 표현
     */
    override fun toString(): String {
        return "WebSocketMessage(type=$type, roomId=$roomId, senderId=$senderId, messageId=$messageId)"
    }
}