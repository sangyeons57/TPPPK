package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * 친구 관계의 상태를 나타내는 열거형입니다.
 * Firestore의 `users/{userId}/friends/{friendId}.status` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class FriendStatus(val value: String) {
    /**
     * 친구 요청을 보냈거나 받은 상태 (상대방의 수락 대기 중)
     */
    @PropertyName("PENDING")
    PENDING("PENDING"),
    @PropertyName("REQUESTED")
    REQUESTED("REQUESTED"),

    /**
     * 친구 관계가 수락된 상태
     */
    @PropertyName("ACCEPTED")
    ACCEPTED("ACCEPTED"),

    /**
     * 친구 요청이 거절된 상태 (선택적)
     */
    @PropertyName("DECLINED")
    DECLINED("DECLINED"),
    @PropertyName("REJECTED")
    REJECTED("REJECTED"),

    /**
     * 해당 사용자를 차단한 상태 (선택적)
     */
    @PropertyName("BLOCKED")
    BLOCKED("BLOCKED"),

    /**
     * 친구 관계가 해제된 상태
     */
    @PropertyName("REMOVED")
    REMOVED("REMOVED"),

    /**
     * 알 수 없거나 정의되지 않은 상태
     */
    @PropertyName("UNKNOWN")
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 문자열 값으로부터 FriendStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 FriendStatus 상수, 없으면 FriendStatus.UNKNOWN
         */
        fun fromString(value: String?): FriendStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 