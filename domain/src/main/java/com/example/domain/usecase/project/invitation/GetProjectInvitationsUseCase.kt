package com.example.domain.usecase.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.ProjectInvitationStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ProjectInvitationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 프로젝트 초대 목록 조회 UseCase
 * 
 * 사용자가 받은 초대 목록이나 보낸 초대 목록을 조회할 때 사용합니다.
 */
interface GetProjectInvitationsUseCase {
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
}

/**
 * 프로젝트 초대 목록 조회 UseCase 구현체
 */
class GetProjectInvitationsUseCaseImpl @Inject constructor(
    private val projectInvitationRepository: ProjectInvitationRepository
) : GetProjectInvitationsUseCase {

    /**
     * 받은 초대 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    override suspend fun getReceivedInvitations(
        userId: UserId,
        status: ProjectInvitationStatus?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        return projectInvitationRepository.getReceivedInvitations(userId, status)
    }
    
    /**
     * 보낸 초대 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID (null이면 모든 프로젝트)
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    override suspend fun getSentInvitations(
        userId: UserId,
        projectId: DocumentId?,
        status: ProjectInvitationStatus?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        return projectInvitationRepository.getSentInvitations(userId, projectId, status)
    }
    
    /**
     * 특정 프로젝트의 초대 목록을 조회합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 초대 목록 Flow
     */
    override suspend fun getProjectInvitations(
        projectId: DocumentId,
        status: ProjectInvitationStatus?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        return projectInvitationRepository.getProjectInvitations(projectId, status)
    }
}