package com.example.domain.model.enum

/**
 * 프로젝트 초대 상태를 나타내는 열거형
 */
enum class ProjectInvitationStatus {
    /**
     * 초대 대기 중 (아직 응답하지 않음)
     */
    PENDING,
    
    /**
     * 초대 수락
     */
    ACCEPTED,
    
    /**
     * 초대 거절
     */
    REJECTED,
    
    /**
     * 초대 만료
     */
    EXPIRED,
    
    /**
     * 초대 취소 (초대한 사람이 취소)
     */
    CANCELLED,
    
    /**
     * 알 수 없는 상태
     */
    UNKNOWN;
    
    companion object {
        /**
         * 문자열에서 ProjectInvitationStatus로 변환
         */
        fun fromString(value: String?): ProjectInvitationStatus {
            return when (value?.uppercase()) {
                "PENDING" -> PENDING
                "ACCEPTED" -> ACCEPTED
                "REJECTED" -> REJECTED
                "EXPIRED" -> EXPIRED
                "CANCELLED" -> CANCELLED
                else -> UNKNOWN
            }
        }
    }
    
    /**
     * 응답 가능한 상태인지 확인
     */
    fun isRespondable(): Boolean {
        return this == PENDING
    }
    
    /**
     * 완료된 상태인지 확인
     */
    fun isCompleted(): Boolean {
        return this in setOf(ACCEPTED, REJECTED, EXPIRED, CANCELLED)
    }
}