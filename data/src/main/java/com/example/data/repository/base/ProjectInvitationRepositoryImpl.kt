package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectInvitationRemoteDataSource
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.repository.base.ProjectInvitationRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote ProjectInvitation Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class ProjectInvitationRepositoryImpl @Inject constructor(
    private val projectInvitationRemoteDataSource: ProjectInvitationRemoteDataSource,
    override val factoryContext: ProjectInvitationRepositoryFactoryContext,
) : ProjectInvitationRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?
    ): CustomResult<SyncResult<ProjectInvitation>, Exception> {
        return projectInvitationRemoteDataSource.syncFromServer(lastSyncCursor, projectId)
    }

    override suspend fun syncToServer(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return projectInvitationRemoteDataSource.syncToServer(projectId)
    }

    override suspend fun forceSyncAll(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return projectInvitationRemoteDataSource.forceSyncAll(projectId)
    }

    override suspend fun resolveConflicts(
        conflictedInvitationIds: List<String>
    ): CustomResult<Int, Exception> {
        return projectInvitationRemoteDataSource.resolveConflicts(conflictedInvitationIds)
    }

    // === Firebase Functions (서버 작업) ===

    override suspend fun getInvitationByCode(
        inviteCode: InviteCode
    ): CustomResult<ProjectInvitation, Exception> {
        return when (val result = projectInvitationRemoteDataSource.validateInviteCodeViaFunction(inviteCode.value)) {
            is CustomResult.Success -> {
                try {
                    val invitation = mapToProjectInvitation(result.data)
                    CustomResult.Success(invitation)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            else -> result as CustomResult<ProjectInvitation, Exception>
        }
    }

    override suspend fun validateInviteCode(
        inviteCode: InviteCode,
        userId: UserId?,
    ): CustomResult<Map<String, Any?>, Exception> {
        return projectInvitationRemoteDataSource.validateInviteCodeViaFunction(inviteCode.value)
    }

    override suspend fun generateInviteLink(
        projectId: DocumentId,
        expiresInHours: Int
    ): CustomResult<Map<String, Any?>, Exception> {
        return projectInvitationRemoteDataSource.generateInviteLinkViaFunction(
            projectId.value,
            expiresInHours,
        )
    }

    override suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception> {
        return projectInvitationRemoteDataSource.joinProjectWithInviteViaFunction(inviteCode)
    }

    private fun mapToProjectInvitation(data: Map<String, Any?>): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(data["id"] as String),
            status = InviteStatus.fromString(data["status"] as String?),
            inviterId = UserId(data["inviterId"] as String),
            projectId = DocumentId(data["projectId"] as String),
            createdAt = (data["createdAt"] as? Long)?.let { java.time.Instant.ofEpochMilli(it) },
            updatedAt = (data["updatedAt"] as? Long)?.let { java.time.Instant.ofEpochMilli(it) },
            expiresAt = (data["expiresAt"] as? Long)?.let { java.time.Instant.ofEpochMilli(it) }
        )
    }
}