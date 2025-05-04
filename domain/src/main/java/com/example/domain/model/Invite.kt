package com.example.domain.model

import java.time.LocalDateTime

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
    val type: String,
    
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
     */
    val expiresAt: LocalDateTime,
    
    /**
     * 초대 생성 시간
     */
    val createdAt: LocalDateTime
) 