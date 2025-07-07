package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * DM 채널의 상태를 나타내는 열거형
 * Firebase Firestore와의 호환성을 위해 PropertyName 어노테이션 사용
 */
enum class DMChannelStatus(val value: String) {
    /**
     * 활성 상태 - 정상적으로 사용 가능한 채널
     */
    @PropertyName("ACTIVE")
    ACTIVE("ACTIVE"),

    /**
     * 보관 상태 - 보관되어 일반 목록에서 숨겨진 채널
     */
    @PropertyName("ARCHIVED")
    ARCHIVED("ARCHIVED"),

    /**
     * 차단 상태 - 한 사용자가 상대방을 차단한 채널
     */
    @PropertyName("BLOCKED")
    BLOCKED("BLOCKED"),

    /**
     * 삭제 상태 - 소프트 삭제된 채널
     */
    @PropertyName("DELETED")
    DELETED("DELETED"),

    /**
     * 알 수 없는 상태 - 기본값 또는 인식되지 않은 상태
     */
    @PropertyName("UNKNOWN")
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 문자열 값으로부터 DMChannelStatus를 찾아 반환
         * @param value 찾을 상태 문자열
         * @return 해당하는 DMChannelStatus, 없으면 UNKNOWN
         */
        fun fromString(value: String?): DMChannelStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}