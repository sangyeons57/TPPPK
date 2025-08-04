package com.example.websocket.core

/**
 * WebSocket 연결 상태를 나타내는 sealed class
 *
 * WebSocket의 다양한 연결 상태를 타입 안전하게 표현하며,
 * UI에서 상태에 따른 적절한 처리를 할 수 있도록 도와준다.
 */
sealed class WebSocketConnectionState {

    /**
     * 연결이 해제된 상태
     *
     * 초기 상태이거나 연결이 정상적으로 종료된 상태
     */
    object Disconnected : WebSocketConnectionState()

    /**
     * 연결을 시도하는 중인 상태
     *
     * WebSocket 서버에 연결을 시도하고 있는 상태
     */
    object Connecting : WebSocketConnectionState()

    /**
     * 연결이 성공한 상태
     *
     * WebSocket 서버와 성공적으로 연결되어 메시지를 주고받을 수 있는 상태
     *
     * @param serverUrl 연결된 서버의 URL
     */
    data class Connected(val serverUrl: String) : WebSocketConnectionState()

    /**
     * 인증을 진행하는 중인 상태
     *
     * WebSocket 연결은 성공했지만 서버로부터 인증 성공 응답을 기다리는 상태
     *
     * @param serverUrl 연결된 서버의 URL
     */
    data class Authenticating(val serverUrl: String) : WebSocketConnectionState()

    /**
     * 연결 오류가 발생한 상태
     *
     * 연결 실패, 네트워크 오류, 인증 실패 등의 문제가 발생한 상태
     *
     * @param message 오류 메시지
     * @param throwable 발생한 예외 (옵션)
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : WebSocketConnectionState()

    /**
     * 재연결을 시도하는 중인 상태
     *
     * 연결이 끊어진 후 자동으로 재연결을 시도하고 있는 상태
     *
     * @param attempt 현재 재연결 시도 횟수
     * @param maxAttempts 최대 재연결 시도 횟수
     * @param nextRetryDelayMs 다음 재연결 시도까지의 대기 시간 (밀리초)
     */
    data class Reconnecting(
        val attempt: Int,
        val maxAttempts: Int,
        val nextRetryDelayMs: Long
    ) : WebSocketConnectionState()

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 현재 상태가 연결된 상태인지 확인
     */
    fun isConnected(): Boolean = this is Connected

    /**
     * 현재 상태가 연결 시도 중인 상태인지 확인 (연결 중, 인증 중 또는 재연결 중)
     */
    fun isConnecting(): Boolean =
        this is Connecting || this is Authenticating || this is Reconnecting

    /**
     * 현재 상태가 연결이 해제된 상태인지 확인
     */
    fun isDisconnected(): Boolean = this is Disconnected

    /**
     * 현재 상태가 오류 상태인지 확인
     */
    fun isError(): Boolean = this is Error

    /**
     * 메시지 전송이 가능한 상태인지 확인
     *
     * 연결되어 있고 오류 상태가 아닐 때만 메시지 전송 가능
     */
    fun canSendMessages(): Boolean = this is Connected

    /**
     * 사용자에게 표시할 상태 메시지 반환
     */
    fun getDisplayMessage(): String {
        return when (this) {
            is Disconnected -> "연결 해제됨"
            is Connecting -> "연결 중..."
            is Authenticating -> "인증 중..."
            is Connected -> "연결됨"
            is Error -> "연결 오류: $message"
            is Reconnecting -> "재연결 시도 중... ($attempt/$maxAttempts)"
        }
    }

    /**
     * 상태의 간단한 문자열 표현
     */
    override fun toString(): String {
        return when (this) {
            is Disconnected -> "Disconnected"
            is Connecting -> "Connecting"
            is Authenticating -> "Authenticating($serverUrl)"
            is Connected -> "Connected($serverUrl)"
            is Error -> "Error($message)"
            is Reconnecting -> "Reconnecting($attempt/$maxAttempts)"
        }
    }
}