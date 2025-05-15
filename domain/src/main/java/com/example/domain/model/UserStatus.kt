// 경로: domain/model/UserStatus.kt (신규 생성 또는 이동)
package com.example.domain.model

/**
 * 사용자 접속 상태를 나타내는 열거형입니다.
 * Firestore `users/{userId}.status` 필드 값과 일치합니다.
 */
enum class UserStatus(val value: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    // AWAY, DO_NOT_DISTURB 등은 Firestore 스키마에 현재 없으므로,
    // 필요시 스키마 확장 후 추가하거나, UI 표시용으로만 사용한다면 별도 관리 필요.
    // 여기서는 Firestore 스키마와 직접 매칭되는 값만 우선 정의합니다.
    UNKNOWN("UNKNOWN"); // 스키마에 없는 값을 위한 기본 상태

    companion object {
        /**
         * 문자열로부터 UserStatus를 파싱합니다.
         * 일치하는 상태가 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(statusString: String?): UserStatus {
            return entries.find { it.value.equals(statusString, ignoreCase = true) } 
                ?: UNKNOWN
        }
    }
}