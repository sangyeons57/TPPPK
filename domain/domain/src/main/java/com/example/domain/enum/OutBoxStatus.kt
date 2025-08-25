package com.example.domain.enum

/**
 * OutBox 상태를 나타내는 열거형
 *
 * 메시지 전송 상태를 관리하는 데 사용됩니다:
 * - PENDING: 전송 대기 중
 * - DISPATCHED: 전송 완료 (ACK 수신 포함)
 * - FAILED: 전송 실패
 */
enum class OutBoxStatus(val value: String) {
    PENDING("PENDING"),
    DISPATCHED("DISPATCHED"),
    FAILED("FAILED");

    companion object {
        /**
         * 문자열 값으로부터 OutBoxStatus Enum 상수를 반환합니다.
         * null이면 OutBox 레코드가 없다는 의미로 DISPATCHED(전송완료)를 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 OutBoxStatus 상수, null이면 DISPATCHED
         */
        fun fromString(value: String?): OutBoxStatus {
            if (value == null) return DISPATCHED // OutBox 레코드가 없으면 전송 완료로 간주
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: PENDING
        }
    }

    /**
     * 전송 대기 중인지 확인
     */
    fun isPending(): Boolean = this == PENDING

    /**
     * 전송 완료되었는지 확인
     */
    fun isDispatched(): Boolean = this == DISPATCHED

    /**
     * 전송 실패했는지 확인
     */
    fun isFailed(): Boolean = this == FAILED

    /**
     * 전송이 완료되었는지 확인 (DISPATCHED 또는 FAILED)
     */
    fun isCompleted(): Boolean = this == DISPATCHED || this == FAILED
} 