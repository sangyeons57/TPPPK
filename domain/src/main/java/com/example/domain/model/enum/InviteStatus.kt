package com.example.domain.model.enum

/**
 * 초대장의 상태를 나타내는 열거형입니다.
 * Firestore의 `invites/{inviteId}.status` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class InviteStatus(val value: String) {
    /**
     * 초대장이 활성 상태이며 사용 가능합니다.
     */
    ACTIVE("active"),

    /**
     * 초대장이 비활성 상태이며 더 이상 사용할 수 없습니다.
     */
    INACTIVE("inactive"),

    /**
     * 초대장이 만료되어 더 이상 사용할 수 없습니다.
     */
    EXPIRED("expired"),

    /**
     * 알 수 없거나 정의되지 않은 상태
     */
    UNKNOWN("unknown");

    companion object {
        /**
         * 문자열 값으로부터 InviteStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 InviteStatus 상수, 없으면 InviteStatus.UNKNOWN
         */
        fun fromString(value: String?): InviteStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 