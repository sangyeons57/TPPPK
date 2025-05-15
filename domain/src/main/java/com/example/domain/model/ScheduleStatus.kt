package com.example.domain.model

/**
 * 일정의 상태를 나타내는 열거형입니다.
 * Firestore `schedules/{scheduleId}.status` 필드 값과 일치합니다.
 */
enum class ScheduleStatus(val value: String) {
    SCHEDULED("SCHEDULED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    UNKNOWN("UNKNOWN"); // 스키마에 없는 값을 위한 기본 상태

    companion object {
        /**
         * 문자열로부터 ScheduleStatus를 파싱합니다.
         * 일치하는 상태가 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(statusString: String?): ScheduleStatus {
            return entries.find { it.value.equals(statusString, ignoreCase = true) } 
                ?: UNKNOWN
        }
    }
} 