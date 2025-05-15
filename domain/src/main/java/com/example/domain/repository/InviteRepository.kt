package com.example.domain.repository

import com.example.domain.model.Invite
import com.example.domain.model.ProjectInfo
import java.time.Instant

/**
 * 초대 관련 저장소 인터페이스
 * 로컬 및 원격 데이터 소스를 조율하여 초대 데이터를 관리합니다.
 */
interface InviteRepository {
    /**
     * 새 초대 토큰을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param expiresAt 만료 시간 (null인 경우 기본값 사용)
     * @return 생성된 초대 토큰 결과
     */
    suspend fun createInviteToken(
        projectId: String,
        expiresAt: Instant? = null
    ): Result<String>
    
    /**
     * 초대 토큰의 유효성을 검사합니다.
     * 
     * @param token 초대 토큰
     * @return 유효성 검사 결과 (true: 유효, false: 무효)
     */
    suspend fun validateInviteToken(token: String): Result<Boolean>
    
    /**
     * 초대를 수락하고, 사용자를 프로젝트에 추가합니다.
     * 
     * @param token 초대 토큰
     * @return 프로젝트 ID 결과
     */
    suspend fun acceptInvite(token: String): Result<String>
    
    /**
     * 초대 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @param forceRefresh 원격 데이터를 강제로 가져올지 여부
     * @return 초대 정보 결과
     */
    suspend fun getInviteDetails(token: String, forceRefresh: Boolean = false): Result<Invite>
    
    /**
     * 초대 토큰에서 프로젝트 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @param forceRefresh 원격 데이터를 강제로 가져올지 여부
     * @return 프로젝트 정보 결과
     */
    suspend fun getProjectInfoFromToken(token: String, forceRefresh: Boolean = false): Result<ProjectInfo>
    
    /**
     * 만료된 초대 정보를 정리합니다.
     * 
     * @return 작업 성공 여부
     */
    suspend fun cleanupExpiredInvites(): Result<Unit>
} 