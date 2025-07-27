package com.example.mapper

import com.example.data.model.remote.ProjectsWrapperDTO
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.example.mapper.base.BaseMapper

interface ProjectsWrapperMapper : BaseMapper<ProjectsWrapper, ProjectsWrapperDTO>

class ProjectsWrapperMapperImpl : ProjectsWrapperMapper {
    override fun fromDto(dto: ProjectsWrapperDTO): ProjectsWrapper {
        return ProjectsWrapper.fromDataSource(
            id = DocumentId(dto.id),
            order = ProjectWrapperOrder.from(dto.order),
            projectName = ProjectName(dto.projectName),
            projectImageUrl = dto.projectImageUrl?.let { ImageUrl(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: ProjectsWrapper): ProjectsWrapperDTO {
        return ProjectsWrapperDTO(
            id = domain.id.value,
            order = domain.order.toDouble(),
            projectName = domain.projectName.value,
            projectImageUrl = domain.projectImageUrl?.value,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: ProjectsWrapper): Map<String, Any?> {
        return mapOf(
            ProjectsWrapper.KEY_ORDER to domain.order.value,
            ProjectsWrapper.KEY_PROJECT_NAME to domain.projectName.value,
            ProjectsWrapper.KEY_PROJECT_IMAGE_URL to domain.projectImageUrl?.value,
        )
    }

    override fun dataToMap(data: ProjectsWrapperDTO): Map<String, Any?> {
        return mapOf(
            ProjectsWrapper.KEY_ORDER to data.order,
            ProjectsWrapper.KEY_PROJECT_NAME to data.projectName,
            ProjectsWrapper.KEY_PROJECT_IMAGE_URL to data.projectImageUrl,
        )
    }
}
