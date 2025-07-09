package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.ProjectInvitationStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 초대 관련 데이터 처리를 위한 Repository 인터페이스
 */
interface ProjectInvitationRepository {
    
    /**
     * 프로젝트 초대를 보냅니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @param message 초대 메시지 (선택사항)
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 생성된 초대 객체
     */
    suspend fun sendInvitation(
        projectId: DocumentId,
        inviteeId: UserId,
        message: String? = null,
        expiresInHours: Long = 72L
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 프로젝트 초대를 수락합니다.
     * 
     * @param invitationId 초대 ID
     * @return 수락된 초대 객체
     */
    suspend fun acceptInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 프로젝트 초대를 거절합니다.
     * 
     * @param invitationId 초대 ID
     * @return 거절된 초대 객체
     */
    suspend fun rejectInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 프로젝트 초대를 취소합니다.
     * 
     * @param invitationId 초대 ID
     * @return 취소된 초대 객체
     */
    suspend fun cancelInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 받은 초대 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    suspend fun getReceivedInvitations(
        userId: UserId,
        status: ProjectInvitationStatus? = null
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>>
    
    /**
     * 보낸 초대 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID (null이면 모든 프로젝트)
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    suspend fun getSentInvitations(
        userId: UserId,
        projectId: DocumentId? = null,
        status: ProjectInvitationStatus? = null
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>>
    
    /**
     * 특정 프로젝트의 초대 목록을 조회합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    suspend fun getProjectInvitations(
        projectId: DocumentId,
        status: ProjectInvitationStatus? = null
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>>
    
    /**
     * 특정 초대를 조회합니다.
     * 
     * @param invitationId 초대 ID
     * @return 초대 객체
     */
    suspend fun getInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 중복 초대 확인 (같은 프로젝트에 같은 사용자가 이미 초대받았는지)
     * 
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @return 중복 초대 여부
     */
    suspend fun hasPendingInvitation(
        projectId: DocumentId,
        inviteeId: UserId
    ): CustomResult<Boolean, Exception>
}