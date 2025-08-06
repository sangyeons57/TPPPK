package com.example.mapper.project

import com.example.data_model.remote.ProjectChannelDTO
import com.example.domain.model.base.ProjectChannel
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.projectchannel.ProjectChannelOrder
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectChannel 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class ProjectChannelMapper @Inject constructor() : DtoMapper<ProjectChannel, ProjectChannelDTO> {

    override fun dtoToDomain(dto: ProjectChannelDTO): ProjectChannel {
        return ProjectChannel.fromDataSource(
            id = DocumentId(dto.id),
            channelName = Name(dto.channelName),
            channelType = dto.channelType,
            order = ProjectChannelOrder.fromDouble(dto.order),
            status = dto.status,
            categoryId = DocumentId(dto.categoryId),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: ProjectChannel): ProjectChannelDTO {
        return ProjectChannelDTO(
            id = domain.id.value,
            channelName = domain.channelName.value,
            channelType = domain.channelType,
            order = domain.order.value.toDouble(),
            status = domain.status,
            categoryId = domain.categoryId.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}