package com.example.domain.model

/**
 * 채널의 기능 모드(텍스트, 음성 등)를 나타내는 열거형입니다.
 */
enum class ChannelMode(val value: String) {
    TEXT("TEXT"),   // 텍스트 채널 모드
    VOICE("VOICE"), // 음성 채널 모드
    UNKNOWN("UNKNOWN"); // 알 수 없거나 정의되지 않은 모드

    companion object {
        /**
         * 문자열로부터 ChannelMode를 파싱합니다.
         * 일치하는 모드가 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(modeString: String?): ChannelMode {
            return values().find { it.value.equals(modeString, ignoreCase = true) } 
                ?: UNKNOWN
        }
    }
} 