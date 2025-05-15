package com.example.data.datasource.remote.invite

import com.example.domain.model.Invite
import com.example.domain.model.ProjectInfo
import java.time.Instant
import java.time.LocalDateTime

/**
 * 초대 관련 원격 데이터 소스 인터페이스
 * Firebase Firestore 작업을 정의합니다.
 */
interface InviteRemoteDataSource {
    /**
     * 새 초대 토큰을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviterId 초대자 ID (로그인된 사용자)
     * @param expiresAt 만료 시간 (null인 경우 기본값 사용)
     * @return 생성된 초대 토큰 결과
     */
    suspend fun createInviteToken(
        projectId: String,
        inviterId: String,
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
     * 초대를 수락하고 프로젝트에 사용자를 추가합니다.
     * 
     * @param token 초대 토큰
     * @param userId 수락하는 사용자 ID
     * @return 프로젝트 ID 결과
     */
    suspend fun acceptInvite(token: String, userId: String): Result<String>
    
    /**
     * 초대 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @return 초대 정보 결과
     */
    suspend fun getInviteDetails(token: String): Result<Invite>
    
    /**
     * 초대 토큰에서 프로젝트 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @return 프로젝트 정보 결과
     */
    suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo>
} 