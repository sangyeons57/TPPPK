package com.example.core_common.constants

/**
 * 메시지 전송 상태 상수 관리
 * 클라이언트 측에서 메시지의 전송 상태를 추적하기 위한 상수들
 *
 * Room 데이터베이스의 delivery_status 필드에서 사용됨
 */
object MessageDeliveryStatus {

    // ================================
    // 메시지 전송 상태
    // ================================

    /** 전송 중 - 서버로 전송 중인 상태 */
    const val SENDING = "SENDING"

    /** 전송 완료 - 서버에서 ACK를 받은 상태 */
    const val SENT = "SENT"

    /** 전송 실패 - 서버 전송 실패 또는 타임아웃 */
    const val FAILED = "FAILED"

    // ================================
    // 기본값 및 유틸리티
    // ================================

    /** 기본 상태 - 서버에서 받은 메시지의 기본 상태 */
    const val DEFAULT = SENT

    /**
     * 유효한 전송 상태인지 확인
     */
    fun isValidStatus(status: String): Boolean {
        return status in setOf(SENDING, SENT, FAILED)
    }

    /**
     * 재전송 가능한 상태인지 확인
     */
    fun canRetry(status: String): Boolean {
        return status == FAILED
    }

    /**
     * 전송 진행 중인 상태인지 확인
     */
    fun isInProgress(status: String): Boolean {
        return status == SENDING
    }

    /**
     * 전송 완료된 상태인지 확인
     */
    fun isCompleted(status: String): Boolean {
        return status == SENT
    }

    /**
     * 전송 실패 상태인지 확인
     */
    fun isFailed(status: String): Boolean {
        return status == FAILED
    }

    /**
     * 상태에 따른 UI 표시용 텍스트 반환
     */
    fun getDisplayText(status: String): String {
        return when (status) {
            SENDING -> "전송 중..."
            SENT -> "전송됨"
            FAILED -> "전송 실패"
            else -> "알 수 없음"
        }
    }
}