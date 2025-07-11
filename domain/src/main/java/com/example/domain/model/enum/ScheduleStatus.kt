package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * 일정의 상태를 나타내는 열거형입니다.
 * Firestore의 `schedules/{scheduleId}.status` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class ScheduleStatus(val value: String) {
    /**
     * 일정이 확정된 상태입니다.
     */
    @PropertyName("confirmed")
    CONFIRMED("confirmed"),

    /**
     * 일정이 임시적이거나 미확정된 상태입니다.
     */
    @PropertyName("tentative")
    TENTATIVE("tentative"),

    /**
     * 일정이 취소된 상태입니다.
     */
    @PropertyName("cancelled")
    CANCELLED("cancelled"),

    /**
     * 일정이 진행 중인 상태입니다. (기존 ScheduleStatus.kt 참고)
     */
    @PropertyName("in_progress")
    IN_PROGRESS("in_progress"),

    /**
     * 일정이 완료된 상태입니다. (기존 ScheduleStatus.kt 참고)
     */
    @PropertyName("completed")
    COMPLETED("completed"),

    /**
     * 알 수 없거나 정의되지 않은 상태
     */
    @PropertyName("unknown")
    UNKNOWN("unknown");

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