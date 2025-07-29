package com.example.mapper

import com.example.data_core.model.remote.ProjectInvitationDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.mapper.base.BaseMapper
import java.util.Date

interface ProjectInvitationMapper : BaseMapper<ProjectInvitation, ProjectInvitationDTO>

class ProjectInvitationMapperImpl : ProjectInvitationMapper {
    override fun toDomain(dto: ProjectInvitationDTO): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(dto.id),
            status = InviteStatus.fromString(dto.status),
            inviterId = UserId(dto.inviterId),
            projectId = DocumentId(dto.projectId),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
            expiresAt = dto.expiresAt?.toInstant()
        )
    }

    override fun toDto(domain: ProjectInvitation): ProjectInvitationDTO {
        return ProjectInvitationDTO(
            id = domain.id.value,
            inviterId = domain.inviterId.value,
            projectId = domain.projectId.value,
            status = domain.status.value,
            expiresAt = domain.expiresAt?.let { Date.from(it) },
            createdAt = Date.from(domain.createdAt),
            updatedAt = Date.from(domain.updatedAt)
        )
    }

    override fun domainToMap(domain: ProjectInvitation): Map<String, Any?> {
        return mapOf(
            ProjectInvitation.KEY_INVITE_CODE to domain.inviteCode.value,
            ProjectInvitation.KEY_STATUS to domain.status.value,
            ProjectInvitation.KEY_INVITER_ID to domain.inviterId.value,
            ProjectInvitation.KEY_PROJECT_ID to domain.projectId.value,
            ProjectInvitation.KEY_EXPIRES_AT to domain.expiresAt,
        )
    }

    override fun dataToMap(data: ProjectInvitationDTO): Map<String, Any?> {
        return mapOf(
            ProjectInvitation.KEY_INVITE_CODE to data.id,
            ProjectInvitation.KEY_STATUS to data.status,
            ProjectInvitation.KEY_INVITER_ID to data.inviterId,
            ProjectInvitation.KEY_PROJECT_ID to data.projectId,
            ProjectInvitation.KEY_EXPIRES_AT to data.expiresAt,
        )
    }

    override fun mapToDto(map: Map<String, Any?>): ProjectInvitationDTO {
        return ProjectInvitationDTO(
            id = map["id"] as? String ?: "",
            status = (map[ProjectInvitation.KEY_STATUS] as? String)?.let {
                InviteStatus.fromString(
                    it
                )
            } ?: InviteStatus.EXPIRED,
            inviterId = map[ProjectInvitation.KEY_INVITER_ID] as? String ?: "",
            projectId = map[ProjectInvitation.KEY_PROJECT_ID] as? String ?: "",
            expiresAt = (map[ProjectInvitation.KEY_EXPIRES_AT] as? Date),
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}
