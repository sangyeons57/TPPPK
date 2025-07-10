package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 초대 링크 관련 데이터 처리를 위한 Repository 인터페이스
 * 
 * DDD DefaultRepository 패턴을 따릅니다:
 * - 통합된 CRUD 작업: save(), delete(), findById(), findAll(), observe(), observeAll()
 * - 도메인별 읽기 작업: 아래 메서드들로 제공
 * 
 * 글로벌 초대 링크 방식을 지원합니다.
 */
interface ProjectInvitationRepository : DefaultRepository {
    override val factoryContext: ProjectInvitationRepositoryFactoryContext
    
    
    /**
     * 초대 코드로 초대 정보를 조회합니다.
     * 
     * @param inviteCode 초대 코드
     * @return 초대 정보
     */
    suspend fun getInvitationByCode(
        inviteCode: InviteCode
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 초대 코드의 유효성을 검증합니다.
     * 
     * @param inviteCode 초대 코드
     * @param userId 검증하는 사용자 ID (선택사항)
     * @return 검증 결과
     */
    suspend fun validateInviteCode(
        inviteCode: InviteCode,
        userId: UserId? = null,
    ): CustomResult<Map<String, Any?>, Exception>
    
    
    /**
     * 특정 사용자가 생성한 초대 링크 목록을 조회합니다.
     * 
     * @param inviterId 초대를 생성한 사용자 ID
     * @param projectId 프로젝트 ID (null이면 모든 프로젝트)
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    suspend fun getInviteLinksByInviter(
        inviterId: UserId,
        projectId: DocumentId? = null,
        status: InviteStatus? = null
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>>
    
    /**
     * 특정 프로젝트의 모든 초대 링크를 조회합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    suspend fun getProjectInviteLinks(
        projectId: DocumentId,
        status: InviteStatus? = null
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>>
    
    
    /**
     * 프로젝트에 활성 상태의 초대 링크가 있는지 확인합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviterId 초대를 생성한 사용자 ID (선택사항)
     * @return 활성 초대 링크 존재 여부
     */
    suspend fun hasActiveInviteLink(
        projectId: DocumentId,
        inviterId: UserId? = null
    ): CustomResult<Boolean, Exception>

    /**
     * 프로젝트 초대 링크를 생성합니다.
     *
     * @param projectId 프로젝트 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 24시간)
     * @return 생성된 초대 링크 데이터 맵
     */
    suspend fun generateInviteLink(
        projectId: DocumentId,
        expiresInHours: Int = 24,
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 사용하여 프로젝트에 참여합니다.
     *
     * @param inviteCode 초대 코드
     * @return 참여 결과 데이터 맵
     */
    suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception>
}