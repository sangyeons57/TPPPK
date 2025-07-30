package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * 초대 링크 상태를 나타내는 열거형
 * Firebase Functions InviteEntity와 동일한 구조
 */
enum class InviteStatus(val value: String) {
    /**
     * 활성 상태 - 사용 가능한 초대 링크
     */
    @PropertyName("active")
    ACTIVE("active"),

    /**
     * 만료됨 - 시간이 지나서 사용 불가
     */
    @PropertyName("expired")
    EXPIRED("expired"),

    /**
     * 취소됨 - 생성자가 의도적으로 무효화
     */
    @PropertyName("revoked")
    REVOKED("revoked");

    companion object {
        /**
         * 문자열 값으로부터 InviteStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 EXPIRED를 반환합니다 (안전).
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 InviteStatus 상수, 없으면 InviteStatus.EXPIRED
         */
        fun fromString(value: String?): InviteStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: EXPIRED
        }
    }
    
    /**
     * 사용 가능한 상태인지 확인
     */
    fun isUsable(): Boolean {
        return this == ACTIVE
    }
    
    /**
     * 완료된 상태인지 확인 (더 이상 변경 불가)
     */
    fun isCompleted(): Boolean {
        return this in setOf(EXPIRED, REVOKED)
    }
} 