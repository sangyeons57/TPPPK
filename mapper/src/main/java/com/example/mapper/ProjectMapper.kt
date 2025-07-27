package com.example.mapper

import com.example.data.model.remote.ProjectDTO
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.mapper.base.BaseMapper

interface ProjectMapper : BaseMapper<Project, ProjectDTO>

class ProjectMapperImpl : ProjectMapper {
    override fun fromDto(dto: ProjectDTO): Project {
        return Project.fromDataSource(
            id = DocumentId(dto.id),
            name = ProjectName(dto.name),
            imageUrl = dto.imageUrl?.let { ImageUrl(it) },
            ownerId = OwnerId(dto.ownerId),
            status = ProjectStatus.fromValue(dto.status),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Project): ProjectDTO {
        return ProjectDTO(
            id = domain.id.value,
            name = domain.name.value,
            imageUrl = domain.imageUrl?.value,
            status = domain.status.value,
            ownerId = domain.ownerId.value,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Project): Map<String, Any?> {
        return mapOf(
            Project.KEY_NAME to domain.name.value,
            Project.KEY_STATUS to domain.status.value,
            Project.KEY_OWNER_ID to domain.ownerId.value
        )
    }

    override fun dataToMap(data: ProjectDTO): Map<String, Any?> {
        return mapOf(
            Project.KEY_NAME to data.name,
            Project.KEY_STATUS to data.status,
            Project.KEY_OWNER_ID to data.ownerId
        )
    }
}
