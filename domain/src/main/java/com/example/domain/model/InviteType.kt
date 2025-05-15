package com.example.domain.model

/**
 * 초대 유형을 나타내는 열거형입니다.
 */
enum class InviteType(val value: String) {
    PROJECT_INVITE("project_invite"), // 프로젝트 초대 유형
    UNKNOWN("UNKNOWN");               // 알 수 없거나 정의되지 않은 초대 유형

    companion object {
        /**
         * 문자열로부터 InviteType을 파싱합니다.
         * 일치하는 유형이 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(typeString: String?): InviteType {
            return values().find { it.value.equals(typeString, ignoreCase = true) } 
                ?: UNKNOWN
        }
    }
} 