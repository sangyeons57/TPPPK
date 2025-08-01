package com.example.websocket.constant

/**
 * WebSocket 작업 상태 상수 관리
 *
 * WebSocket 작업의 성공/실패 상태와 로깅에 사용되는 상수들을 중앙에서 관리한다.
 * 로그 메시지의 일관성을 유지하고 상태 추적을 용이하게 한다.
 */
object OperationStatus {

    // ================================
    // 작업 결과 상태
    // ================================

    /** 작업 성공 */
    const val SUCCESS = "SUCCESS"

    /** 작업 실패 */
    const val FAILED = "FAILED"

    /** 작업 진행 중 */
    const val IN_PROGRESS = "IN_PROGRESS"

    /** 작업 대기 중 */
    const val PENDING = "PENDING"

    /** 작업 취소됨 */
    const val CANCELLED = "CANCELLED"

    /** 작업 시간 초과 */
    const val TIMEOUT = "TIMEOUT"

    // ================================
    // 작업 유형
    // ================================

    /** 메시지 수신 작업 */
    const val ACTION_RECEIVE = "RECEIVE"

    /** 메시지 전송 작업 */
    const val ACTION_SEND = "SEND"

    /** 메시지 수정 작업 */
    const val ACTION_EDIT = "EDIT"

    /** 메시지 삭제 작업 */
    const val ACTION_DELETE = "DELETE"

    /** 방 입장 작업 */
    const val ACTION_JOIN_ROOM = "JOIN_ROOM"

    /** 방 퇴장 작업 */
    const val ACTION_LEAVE_ROOM = "LEAVE_ROOM"

    /** 연결 작업 */
    const val ACTION_CONNECT = "CONNECT"

    /** 연결 해제 작업 */
    const val ACTION_DISCONNECT = "DISCONNECT"

    /** 인증 작업 */
    const val ACTION_AUTHENTICATE = "AUTHENTICATE"

    /** 재연결 작업 */
    const val ACTION_RECONNECT = "RECONNECT"

    // ================================
    // 우선순위 레벨
    // ================================

    /** 높은 우선순위 */
    const val PRIORITY_HIGH = "HIGH"

    /** 보통 우선순위 */
    const val PRIORITY_MEDIUM = "MEDIUM"

    /** 낮은 우선순위 */
    const val PRIORITY_LOW = "LOW"

    /** 긴급 우선순위 */
    const val PRIORITY_URGENT = "URGENT"

    // ================================
    // 로그 레벨
    // ================================

    /** 디버그 로그 */
    const val LOG_DEBUG = "DEBUG"

    /** 정보 로그 */
    const val LOG_INFO = "INFO"

    /** 경고 로그 */
    const val LOG_WARNING = "WARNING"

    /** 오류 로그 */
    const val LOG_ERROR = "ERROR"

    /** 치명적 오류 로그 */
    const val LOG_FATAL = "FATAL"

    // ================================
    // 연결 상태
    // ================================

    /** 연결됨 */
    const val CONNECTION_CONNECTED = "CONNECTED"

    /** 연결 중 */
    const val CONNECTION_CONNECTING = "CONNECTING"

    /** 연결 해제됨 */
    const val CONNECTION_DISCONNECTED = "DISCONNECTED"

    /** 재연결 중 */
    const val CONNECTION_RECONNECTING = "RECONNECTING"

    /** 연결 오류 */
    const val CONNECTION_ERROR = "CONNECTION_ERROR"

    // ================================
    // 인증 상태
    // ================================

    /** 인증됨 */
    const val AUTH_AUTHENTICATED = "AUTHENTICATED"

    /** 인증되지 않음 */
    const val AUTH_UNAUTHENTICATED = "UNAUTHENTICATED"

    /** 인증 만료됨 */
    const val AUTH_EXPIRED = "EXPIRED"

    /** 인증 갱신 중 */
    const val AUTH_REFRESHING = "REFRESHING"

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 성공 상태인지 확인
     */
    fun isSuccess(status: String): Boolean {
        return status == SUCCESS
    }

    /**
     * 실패 상태인지 확인
     */
    fun isFailed(status: String): Boolean {
        return status == FAILED
    }

    /**
     * 진행 중 상태인지 확인
     */
    fun isInProgress(status: String): Boolean {
        return status == IN_PROGRESS
    }

    /**
     * 대기 중 상태인지 확인
     */
    fun isPending(status: String): Boolean {
        return status == PENDING
    }

    /**
     * 완료된 상태인지 확인 (성공 또는 실패)
     */
    fun isCompleted(status: String): Boolean {
        return status in setOf(SUCCESS, FAILED, CANCELLED, TIMEOUT)
    }

    /**
     * 활성 상태인지 확인 (진행 중 또는 대기 중)
     */
    fun isActive(status: String): Boolean {
        return status in setOf(IN_PROGRESS, PENDING)
    }

    /**
     * 연결된 상태인지 확인
     */
    fun isConnected(connectionStatus: String): Boolean {
        return connectionStatus == CONNECTION_CONNECTED
    }

    /**
     * 연결 진행 중인지 확인
     */
    fun isConnecting(connectionStatus: String): Boolean {
        return connectionStatus in setOf(CONNECTION_CONNECTING, CONNECTION_RECONNECTING)
    }

    /**
     * 인증된 상태인지 확인
     */
    fun isAuthenticated(authStatus: String): Boolean {
        return authStatus == AUTH_AUTHENTICATED
    }

    /**
     * 상태에 따른 UI 표시용 텍스트 반환
     */
    fun getDisplayText(status: String): String {
        return when (status) {
            SUCCESS -> "성공"
            FAILED -> "실패"
            IN_PROGRESS -> "진행 중"
            PENDING -> "대기 중"
            CANCELLED -> "취소됨"
            TIMEOUT -> "시간 초과"
            CONNECTION_CONNECTED -> "연결됨"
            CONNECTION_CONNECTING -> "연결 중"
            CONNECTION_DISCONNECTED -> "연결 해제됨"
            CONNECTION_RECONNECTING -> "재연결 중"
            CONNECTION_ERROR -> "연결 오류"
            AUTH_AUTHENTICATED -> "인증됨"
            AUTH_UNAUTHENTICATED -> "인증되지 않음"
            AUTH_EXPIRED -> "인증 만료"
            AUTH_REFRESHING -> "인증 갱신 중"
            else -> "알 수 없음"
        }
    }

    /**
     * 상태에 따른 로그 레벨 반환
     */
    fun getLogLevel(status: String): String {
        return when (status) {
            SUCCESS -> LOG_INFO
            FAILED, TIMEOUT, CONNECTION_ERROR -> LOG_ERROR
            IN_PROGRESS, PENDING -> LOG_DEBUG
            CANCELLED -> LOG_WARNING
            else -> LOG_INFO
        }
    }
}