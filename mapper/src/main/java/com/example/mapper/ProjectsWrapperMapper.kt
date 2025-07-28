package com.example.mapper

import com.example.data_core.model.remote.ProjectsWrapperDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.example.mapper.base.BaseMapper
import java.util.Date

interface ProjectsWrapperMapper : BaseMapper<ProjectsWrapper, ProjectsWrapperDTO>

class ProjectsWrapperMapperImpl : ProjectsWrapperMapper {
    override fun toDomain(dto: ProjectsWrapperDTO): ProjectsWrapper {
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

    override fun mapToDto(map: Map<String, Any?>): ProjectsWrapperDTO {
        return ProjectsWrapperDTO(
            id = map["id"] as? String ?: "",
            order = map[ProjectsWrapper.KEY_ORDER] as? Double ?: 0.0,
            projectName = map[ProjectsWrapper.KEY_PROJECT_NAME] as? String ?: "",
            projectImageUrl = map[ProjectsWrapper.KEY_PROJECT_IMAGE_URL] as? String,
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}
