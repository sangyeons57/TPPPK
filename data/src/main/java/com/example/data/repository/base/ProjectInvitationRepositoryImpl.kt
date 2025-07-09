package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.ProjectInvitationStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ProjectInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.time.Instant

/**
 * 프로젝트 초대 Repository 구현체
 */
class ProjectInvitationRepositoryImpl @Inject constructor(
    private val functionsRemoteDataSource: FunctionsRemoteDataSource
) : ProjectInvitationRepository {

    /**
     * Firebase Functions의 응답을 ProjectInvitation 도메인 객체로 변환합니다.
     */
    private fun mapToProjectInvitation(data: Map<String, Any?>): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(data["id"] as String),
            status = ProjectInvitationStatus.fromString(data["status"] as String?),
            inviterId = UserId(data["inviterId"] as String),
            projectId = DocumentId(data["projectId"] as String),
            inviteeId = UserId(data["inviteeId"] as String),
            message = data["message"] as String?,
            createdAt = (data["createdAt"] as? Long)?.let { Instant.ofEpochMilli(it) },
            updatedAt = (data["updatedAt"] as? Long)?.let { Instant.ofEpochMilli(it) },
            expiresAt = (data["expiresAt"] as? Long)?.let { Instant.ofEpochMilli(it) }
        )
    }

    /**
     * Firebase Functions의 응답 목록을 ProjectInvitation 도메인 객체 목록으로 변환합니다.
     */
    private fun mapToProjectInvitationList(data: Map<String, Any?>): List<ProjectInvitation> {
        val invitations = data["invitations"] as? List<Map<String, Any?>> ?: return emptyList()
        return invitations.map { mapToProjectInvitation(it) }
    }

    /**
     * 프로젝트 초대를 보냅니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @param message 초대 메시지 (선택사항)
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 생성된 초대 객체
     */
    override suspend fun sendInvitation(
        projectId: DocumentId,
        inviteeId: UserId,
        message: String?,
        expiresInHours: Long
    ): CustomResult<ProjectInvitation, Exception> {
        return functionsRemoteDataSource.sendProjectInvitation(
            projectId = projectId.value,
            inviteeId = inviteeId.value,
            message = message,
            expiresInHours = expiresInHours
        )
    }

    /**
     * 프로젝트 초대를 수락합니다.
     * 
     * @param invitationId 초대 ID
     * @return 수락된 초대 객체
     */
    override suspend fun acceptInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        return functionsRemoteDataSource.acceptProjectInvitation(invitationId.value)
    }

    /**
     * 프로젝트 초대를 거절합니다.
     * 
     * @param invitationId 초대 ID
     * @return 거절된 초대 객체
     */
    override suspend fun rejectInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        return functionsRemoteDataSource.rejectProjectInvitation(invitationId.value)
    }

    /**
     * 프로젝트 초대를 취소합니다.
     * 
     * @param invitationId 초대 ID
     * @return 취소된 초대 객체
     */
    override suspend fun cancelInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        return functionsRemoteDataSource.cancelProjectInvitation(invitationId.value)
    }

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
        return functionsRemoteDataSource.getReceivedInvitations(
            userId = userId.value,
            status = status?.name
        )
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
        return functionsRemoteDataSource.getSentInvitations(
            userId = userId.value,
            projectId = projectId?.value,
            status = status?.name
        )
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
        return functionsRemoteDataSource.getProjectInvitations(
            projectId = projectId.value,
            status = status?.name
        )
    }

    /**
     * 특정 초대를 조회합니다.
     * 
     * @param invitationId 초대 ID
     * @return 초대 객체
     */
    override suspend fun getInvitation(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        return functionsRemoteDataSource.getProjectInvitation(invitationId.value)
    }

    /**
     * 중복 초대 확인 (같은 프로젝트에 같은 사용자가 이미 초대받았는지)
     * 
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @return 중복 초대 여부
     */
    override suspend fun hasPendingInvitation(
        projectId: DocumentId,
        inviteeId: UserId
    ): CustomResult<Boolean, Exception> {
        return functionsRemoteDataSource.hasPendingInvitation(
            projectId = projectId.value,
            inviteeId = inviteeId.value
        )
    }
}