package com.example.mapper

import com.example.data_core.model.local.ProjectsWrapperEntity
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface ProjectsWrapperEntityMapper : BaseEntityMapper<ProjectsWrapper, ProjectsWrapperEntity>

class ProjectsWrapperEntityMapperImpl @Inject constructor() : ProjectsWrapperEntityMapper {
    override fun toDomain(entity: ProjectsWrapperEntity): ProjectsWrapper {
        return ProjectsWrapper.fromDataSource(
            id = DocumentId(entity.id),
            userId = UserId(entity.userId),
            projectId = ProjectId(entity.projectId),
            projectName = ProjectName(entity.projectName),
            projectImageUrl = entity.projectImageUrl?.let { ImageUrl(it) },
            order = ProjectWrapperOrder(entity.order),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: ProjectsWrapper): ProjectsWrapperEntity {
        return ProjectsWrapperEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            projectId = domain.projectId.value,
            projectName = domain.projectName.value,
            projectImageUrl = domain.projectImageUrl?.value,
            order = domain.order.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
