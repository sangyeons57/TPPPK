package com.example.mapper.project

import com.example.data_model.remote.ProjectDTO
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Project 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class ProjectMapper @Inject constructor() : DtoMapper<Project, ProjectDTO> {

    override fun dtoToDomain(dto: ProjectDTO): Project {
        return Project.fromDataSource(
            id = DocumentId(dto.id),
            name = ProjectName(dto.name),
            ownerId = OwnerId(dto.ownerId),
            status = ProjectStatus.fromValue(dto.status),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
        )
    }

    override fun domainToDto(domain: Project): ProjectDTO {
        return ProjectDTO(
            id = domain.id.value,
            name = domain.name.value,
            ownerId = domain.ownerId.value,
            status = domain.status.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}