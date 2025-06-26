package com.example.domain.model.enum

/**
 * 사용자의 접속 상태를 나타내는 열거형입니다.
 * Firestore의 `users/{userId}.status` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class UserStatus(val value: String) {
    /**
     * 사용자가 온라인 상태입니다.
     */
    ONLINE("online"),

    /**
     * 사용자가 오프라인 상태입니다.
     */
    OFFLINE("offline"),

    /**
     * 사용자가 자리 비움 상태입니다. (예시)
     */
    AWAY("away"),

    /**
     * 사용자가 다른 용무 중(방해 금지) 상태입니다. (예시)
     */
    DO_NOT_DISTURB("do_not_disturb"),

    /**
     * 알 수 없거나 정의되지 않은 상태
     */
    UNKNOWN("unknown");

    companion object {
        /**
         * 문자열 값으로부터 UserStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 UserStatus 상수, 없으면 UserStatus.UNKNOWN
         */
        fun fromString(value: String?): UserStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 