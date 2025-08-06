package com.example.mapper.project

import com.example.data_model.remote.ProjectsWrapperDTO
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ImageUrl
import com.example.domain.vo.project.ProjectName
import com.example.domain.vo.projectwrapper.ProjectWrapperOrder
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectsWrapper 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class ProjectsWrapperMapper @Inject constructor() : DtoMapper<ProjectsWrapper, ProjectsWrapperDTO> {

    override fun dtoToDomain(dto: ProjectsWrapperDTO): ProjectsWrapper {
        return ProjectsWrapper.fromDataSource(
            id = DocumentId(dto.id),
            order = ProjectWrapperOrder.from(dto.order),
            projectName = ProjectName(dto.projectName),
            projectImageUrl = dto.projectImageUrl?.let { ImageUrl(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: ProjectsWrapper): ProjectsWrapperDTO {
        return ProjectsWrapperDTO(
            id = domain.id.value,
            order = domain.order.toDouble(),
            projectName = domain.projectName.value,
            projectImageUrl = domain.projectImageUrl?.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}