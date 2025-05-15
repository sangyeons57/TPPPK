package com.example.domain.model

/**
 * 채널 메타데이터의 출처를 나타내는 열거형입니다.
 * (예: DM, 프로젝트, 일정 등)
 */
enum class ChannelMetadataSource(val value: String) {
    DM("dm"),       // DM 채널 출처
    PROJECT("project"), // 프로젝트 채널 출처
    SCHEDULE("schedule"), // 일정 관련 채널 출처
    UNKNOWN("UNKNOWN");   // 알 수 없거나 정의되지 않은 출처

    companion object {
        /**
         * 문자열로부터 ChannelMetadataSource를 파싱합니다.
         * 일치하는 출처가 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(sourceString: String?): ChannelMetadataSource {
            return values().find { it.value.equals(sourceString, ignoreCase = true) } 
                ?: UNKNOWN
        }
    }
} 