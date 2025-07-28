package com.example.mapper

import com.example.data_core.model.local.ProjectsEntity
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface ProjectEntityMapper : BaseEntityMapper<Project, ProjectsEntity>

class ProjectEntityMapperImpl @Inject constructor() : ProjectEntityMapper {
    override fun toDomain(entity: ProjectsEntity): Project {
        return Project.fromDataSource(
            id = DocumentId(entity.id),
            name = ProjectName(entity.name),
            ownerId = OwnerId(entity.ownerId),
            status = ProjectStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Project): ProjectsEntity {
        return ProjectsEntity(
            id = domain.id.value,
            name = domain.name.value,
            ownerId = domain.ownerId.value,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
