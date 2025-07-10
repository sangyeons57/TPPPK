package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectInvitationRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.repository.base.ProjectInvitationRepository
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ProjectInvitation Repository 구현체
 * DDD DefaultRepositoryImpl을 확장하여 통합된 CRUD 작업을 제공합니다.
 */
class ProjectInvitationRepositoryImpl @Inject constructor(
    private val projectInvitationRemoteDataSource: ProjectInvitationRemoteDataSource,
    override val factoryContext: ProjectInvitationRepositoryFactoryContext,
) : DefaultRepositoryImpl(projectInvitationRemoteDataSource, factoryContext), ProjectInvitationRepository {

    /**
     * DDD save() 메서드 - 생성/수정 통합 처리
     * 
     * 새로운 ProjectInvitation: Firebase Functions로 초대 링크 생성
     * 기존 ProjectInvitation: Firestore 직접 업데이트
     */
    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectInvitation)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectInvitation"))
        
        ensureCollection()
        
        return if (entity.isNew) {
            // 새로운 초대 링크 생성 - Firebase Functions 사용
            val expiresInHours = entity.expiresAt?.let {
                val now = java.time.Instant.now()
                val hoursUntilExpiry = java.time.Duration.between(now, it).toHours()
                hoursUntilExpiry.toInt().coerceAtLeast(1) // 최소 1시간
            } ?: 24 // 기본 24시간
            
            projectInvitationRemoteDataSource.generateInviteLinkViaFunction(
                entity.projectId.value,
                expiresInHours
            ).map { data ->
                // Firebase Functions에서 반환된 ID 사용
                DocumentId(data["id"] as? String ?: entity.id.value)
            }
        } else {
            // 기존 초대 링크 업데이트 - Firestore 직접 업데이트
            projectInvitationRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

    /**
     * 초대 코드로 초대 정보를 조회합니다.
     */
    override suspend fun getInvitationByCode(
        inviteCode: InviteCode
    ): CustomResult<ProjectInvitation, Exception> {
        return projectInvitationRemoteDataSource.validateInviteCodeViaFunction(inviteCode.value)
            .map { data ->
                mapToProjectInvitation(data)
            }
    }

    /**
     * 초대 코드의 유효성을 검증합니다.
     */
    override suspend fun validateInviteCode(
        inviteCode: InviteCode,
        userId: UserId?
    ): CustomResult<Boolean, Exception> {
        return projectInvitationRemoteDataSource.validateInviteCodeViaFunction(inviteCode.value)
            .map { data ->
                data["isValid"] as? Boolean ?: false
            }
    }

    /**
     * 특정 사용자가 생성한 초대 링크 목록을 조회합니다.
     */
    override suspend fun getInviteLinksByInviter(
        inviterId: UserId,
        projectId: DocumentId?,
        status: InviteStatus?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        // 실제 구현에서는 Firestore 쿼리를 사용해야 합니다.
        // 현재는 placeholder로 TODO 처리
        return flowOf(CustomResult.Failure(Exception("Not yet implemented - 사용자별 초대 링크 목록 조회")))
    }

    /**
     * 특정 프로젝트의 모든 초대 링크를 조회합니다.
     */
    override suspend fun getProjectInviteLinks(
        projectId: DocumentId,
        status: InviteStatus?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        // 실제 구현에서는 Firestore 쿼리를 사용해야 합니다.
        // 현재는 placeholder로 TODO 처리
        return flowOf(CustomResult.Failure(Exception("Not yet implemented - 프로젝트별 초대 링크 목록 조회")))
    }

    /**
     * 프로젝트에 활성 상태의 초대 링크가 있는지 확인합니다.
     */
    override suspend fun hasActiveInviteLink(
        projectId: DocumentId,
        inviterId: UserId?
    ): CustomResult<Boolean, Exception> {
        // 실제 구현에서는 Firestore 쿼리를 사용해야 합니다.
        // 현재는 placeholder로 TODO 처리
        return CustomResult.Failure(Exception("Not yet implemented - 활성 초대 링크 존재 확인"))
    }

    /**
     * Firebase Functions의 응답을 ProjectInvitation 도메인 객체로 변환합니다.
     */
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