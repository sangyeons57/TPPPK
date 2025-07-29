package com.example.mapper

import com.example.data_core.model.local.ProjectInvitationsEntity
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface ProjectInvitationEntityMapper : BaseEntityMapper<ProjectInvitation, ProjectInvitationsEntity>

class ProjectInvitationEntityMapperImpl @Inject constructor() : ProjectInvitationEntityMapper {
    override fun toDomain(entity: ProjectInvitationsEntity): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(entity.id),
            projectId = ProjectId(entity.projectId),
            inviterId = UserId(entity.inviterId),
            inviteCode = InviteCode(entity.inviteCode),
            status = InviteStatus.valueOf(entity.status),
            expiresAt = entity.expiresAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: ProjectInvitation): ProjectInvitationsEntity {
        return ProjectInvitationsEntity(
            id = domain.id.value,
            projectId = domain.projectId.value,
            inviterId = domain.inviterId.value,
            inviteCode = domain.inviteCode.value,
            status = domain.status.name,
            expiresAt = domain.expiresAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
