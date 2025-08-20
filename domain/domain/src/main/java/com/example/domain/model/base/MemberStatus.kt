package com.example.domain.model.base

/**
 * 프로젝트 멤버의 상태를 나타내는 열거형
 *
 * 멤버의 현재 참여 상태와 권한을 정의합니다.
 */
enum class MemberStatus {
    /**
     * 활성 멤버 - 프로젝트에 정상적으로 참여 중
     * 기존 멤버들의 기본값으로 사용됩니다.
     */
    ACTIVE,

    /**
     * 차단된 멤버 - 일시적으로 프로젝트 참여가 제한됨
     * 프로젝트 소유자의 판단으로 다시 초대될 수 있습니다.
     */
    BLOCKED,

    /**
     * 영구 차단된 멤버 - 프로젝트에 재참여할 수 없음
     * 심각한 위반 행위로 인해 영구적으로 제한됩니다.
     */
    BANNED;

    companion object {
        /**
         * 문자열로부터 MemberStatus를 안전하게 변환합니다.
         * 알 수 없는 값이 들어오면 ACTIVE를 기본값으로 반환합니다.
         */
        fun fromString(value: String?): MemberStatus {
            return when (value?.uppercase()) {
                "ACTIVE" -> ACTIVE
                "BLOCKED" -> BLOCKED
                "BANNED" -> BANNED
                else -> ACTIVE // 기존 데이터 호환성을 위한 기본값
            }
        }

        /**
         * 데이터베이스 저장용 문자열로 변환합니다.
         */
        fun toString(status: MemberStatus): String {
            return status.name.lowercase()
        }
    }
}