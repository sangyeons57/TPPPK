package com.example.domain.model.enum

/**
 * 사용자의 계정 상태를 나타내는 열거형입니다.
 * Firestore의 `users/{userId}.accountStatus` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class UserAccountStatus(val value: String) {
    /**
     * 계정이 활성 상태입니다.
     */
    ACTIVE("ACTIVE"),

    /**
     * 계정이 정지된 상태입니다.
     */
    SUSPENDED("SUSPENDED"),

    /**
     * 계정이 삭제된 상태입니다.
     */
    DELETED("DELETED"),

    /**
     * 사용자가 탈퇴하여 계정 정보가 익명화되고 비활성화된 상태입니다.
     */
    WITHDRAWN("WITHDRAWN"),

    /**
     * 알 수 없거나 정의되지 않은 계정 상태
     */
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 문자열 값으로부터 UserAccountStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 UserAccountStatus 상수, 없으면 UserAccountStatus.UNKNOWN
         */
        fun fromString(value: String?): UserAccountStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 