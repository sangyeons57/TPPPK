package com.example.data.mapper

import com.example.data.model.local.ProjectInvitationsEntity
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.enum.InviteStatus

/**
 * ProjectInvitationsEntity와 ProjectInvitation 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object ProjectInvitationsMapper {

    /**
     * ProjectInvitation 도메인 모델을 ProjectInvitationsEntity로 변환
     * @param projectInvitation 변환할 ProjectInvitation 도메인 모델
     * @return ProjectInvitationsEntity
     */
    fun toEntity(projectInvitation: ProjectInvitation): ProjectInvitationsEntity {
        return ProjectInvitationsEntity(
            id = projectInvitation.id.value,
            inviteCode = projectInvitation.inviteCode.value,
            status = projectInvitation.status.value,
            inviterId = projectInvitation.inviterId.value,
            projectId = projectInvitation.projectId.value,
            expiresAt = projectInvitation.expiresAt,
            createdAt = projectInvitation.createdAt,
            updatedAt = projectInvitation.updatedAt
        )
    }

    /**
     * ProjectInvitationsEntity를 ProjectInvitation 도메인 모델로 변환
     * @param entity 변환할 ProjectInvitationsEntity
     * @return ProjectInvitation 도메인 모델
     */
    fun toDomain(entity: ProjectInvitationsEntity): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(entity.id),
            status = InviteStatus.fromString(entity.status),
            inviterId = UserId(entity.inviterId),
            projectId = DocumentId(entity.projectId),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            expiresAt = entity.expiresAt
        )
    }

    /**
     * ProjectInvitation 도메인 모델 리스트를 ProjectInvitationsEntity 리스트로 변환
     * @param projectInvitations 변환할 ProjectInvitation 도메인 모델 리스트
     * @return ProjectInvitationsEntity 리스트
     */
    fun toEntityList(projectInvitations: List<ProjectInvitation>): List<ProjectInvitationsEntity> {
        return projectInvitations.map { toEntity(it) }
    }

    /**
     * ProjectInvitationsEntity 리스트를 ProjectInvitation 도메인 모델 리스트로 변환
     * @param entities 변환할 ProjectInvitationsEntity 리스트
     * @return ProjectInvitation 도메인 모델 리스트
     */
    fun toDomainList(entities: List<ProjectInvitationsEntity>): List<ProjectInvitation> {
        return entities.map { toDomain(it) }
    }
}