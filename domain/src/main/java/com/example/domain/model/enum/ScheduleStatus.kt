package com.example.domain.model.enum

/**
 * 일정의 상태를 나타내는 열거형입니다.
 * Firestore의 `schedules/{scheduleId}.status` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class ScheduleStatus(val value: String) {
    /**
     * 일정이 확정된 상태입니다.
     */
    CONFIRMED("CONFIRMED"),

    /**
     * 일정이 임시적이거나 미확정된 상태입니다.
     */
    TENTATIVE("TENTATIVE"),

    /**
     * 일정이 취소된 상태입니다.
     */
    CANCELLED("CANCELLED"),

    /**
     * 일정이 진행 중인 상태입니다. (기존 ScheduleStatus.kt 참고)
     */
    IN_PROGRESS("IN_PROGRESS"),

    /**
     * 일정이 완료된 상태입니다. (기존 ScheduleStatus.kt 참고)
     */
    COMPLETED("COMPLETED"),

    /**
     * 알 수 없거나 정의되지 않은 상태
     */
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 문자열 값으로부터 ScheduleStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 ScheduleStatus 상수, 없으면 ScheduleStatus.UNKNOWN
         */
        fun fromString(value: String?): ScheduleStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 