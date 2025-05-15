package com.example.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 초대 토큰 도메인 모델
 * 프로젝트 초대를 위한 임시 토큰 정보를 나타냅니다.
 */
data class Invite(
    /**
     * 초대 토큰 ID (자동 생성된 고유 토큰)
     */
    val token: String,
    
    /**
     * 초대 유형 (예: "project_invite")
     */
    val type: InviteType,
    
    /**
     * 초대를 생성한 사용자 ID
     */
    val inviterId: String,
    
    /**
     * 초대를 생성한 사용자 이름
     */
    val inviterName: String,
    
    /**
     * 대상 프로젝트 ID (프로젝트 초대의 경우)
     */
    val projectId: String?,
    
    /**
     * 대상 프로젝트 이름
     */
    val projectName: String,
    
    /**
     * 초대 만료 시간
     * UTC 기준 시간으로 저장됩니다.
     */
    val expiresAt: Instant,
    
    /**
     * 초대 생성 시간
     * UTC 기준 시간으로 저장됩니다.
     */
    val createdAt: Instant
) {
    /**
     * 만료 시간을 UI 표시용 LocalDateTime으로 변환합니다.
     */
    // fun getExpiresAtLocal(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    //     return LocalDateTime.ofInstant(expiresAt, zoneId)
    // }
    
    /**
     * 생성 시간을 UI 표시용 LocalDateTime으로 변환합니다.
     */
    // fun getCreatedAtLocal(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    //     return LocalDateTime.ofInstant(createdAt, zoneId)
    // }
} 