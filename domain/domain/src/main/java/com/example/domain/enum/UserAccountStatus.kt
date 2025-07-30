package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * 사용자의 계정 상태를 나타내는 열거형입니다.
 * Firestore의 `users/{userId}.accountStatus` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class UserAccountStatus(val value: String) {
    /**
     * 계정이 활성 상태입니다.
     */
    @PropertyName("active")
    ACTIVE("active"),

    /**
     * 계정이 정지된 상태입니다.
     */
    @PropertyName("suspended")
    SUSPENDED("suspended"),

    /**
     * 계정이 삭제된 상태입니다.
     */
    @PropertyName("deleted")
    DELETED("deleted"),

    /**
     * 사용자가 탈퇴하여 계정 정보가 익명화되고 비활성화된 상태입니다.
     */
    @PropertyName("withdrawn")
    WITHDRAWN("withdrawn"),

    /**
     * 알 수 없거나 정의되지 않은 계정 상태
     */
    @PropertyName("unknown")
    UNKNOWN("unknown");

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