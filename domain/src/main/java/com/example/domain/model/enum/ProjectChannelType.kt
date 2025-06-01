package com.example.domain.model.enum

/**
 * 프로젝트 채널의 유형을 나타내는 열거형입니다.
 * Firestore의 `projects/{projectId}/channels/{channelId}.channelType` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class ProjectChannelType(val value: String) {
    /**
     * 일반 메시지 교환용 채널
     */
    MESSAGES("MESSAGES"),

    /**
     * 작업 또는 할 일 관리용 채널
     */
    TASKS("TASKS"),

    /**
     * 공지사항 전달용 채널 (예시)
     */
    ANNOUNCEMENTS("ANNOUNCEMENTS"),

    /**
     * 알 수 없거나 정의되지 않은 채널 유형
     */
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 문자열 값으로부터 ProjectChannelType Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 ProjectChannelType 상수, 없으면 ProjectChannelType.UNKNOWN
         */
        fun fromString(value: String?): ProjectChannelType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 