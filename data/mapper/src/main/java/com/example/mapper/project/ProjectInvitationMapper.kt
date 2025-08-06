package com.example.mapper.project

import com.example.data_model.remote.ProjectInvitationDTO
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ProjectId
import com.example.domain.vo.UserId
import com.example.mapper.DtoMapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectInvitation 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class ProjectInvitationMapper @Inject constructor() :
    DtoMapper<ProjectInvitation, ProjectInvitationDTO> {

    override fun dtoToDomain(dto: ProjectInvitationDTO): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(dto.id),
            inviterId = UserId(dto.inviterId),
            projectId = ProjectId(dto.projectId),
            status = InviteStatus.fromString(dto.status),
            expiresAt = dto.expiresAt?.toInstant(),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: ProjectInvitation): ProjectInvitationDTO {
        return ProjectInvitationDTO(
            id = domain.id.value,
            inviterId = domain.inviterId.value,
            projectId = domain.projectId.value,
            status = domain.status.value,
            expiresAt = domain.expiresAt?.let { Date.from(it) },
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}