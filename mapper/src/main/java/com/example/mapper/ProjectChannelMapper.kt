package com.example.mapper

import com.example.data.model.remote.ProjectChannelDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.mapper.base.BaseMapper
import java.util.Date

interface ProjectChannelMapper : BaseMapper<ProjectChannel, ProjectChannelDTO>

class ProjectChannelMapperImpl : ProjectChannelMapper {
    override fun toDomain(dto: ProjectChannelDTO): ProjectChannel {
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

    override fun toDto(domain: ProjectChannel): ProjectChannelDTO {
        return ProjectChannelDTO(
            id = domain.id.value,
            channelName = domain.channelName.value,
            channelType = domain.channelType,
            order = domain.order.value.toDouble(),
            status = domain.status,
            categoryId = domain.categoryId.value,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: ProjectChannel): Map<String, Any?> {
        return mapOf(
            ProjectChannel.KEY_CHANNEL_NAME to domain.channelName.value,
            ProjectChannel.KEY_CHANNEL_TYPE to domain.channelType.value,
            ProjectChannel.KEY_ORDER to domain.order.value,
            ProjectChannel.KEY_STATUS to domain.status.value,
            ProjectChannel.KEY_CATEGORY_ID to domain.categoryId.value,
        )
    }

    override fun dataToMap(data: ProjectChannelDTO): Map<String, Any?> {
        return mapOf(
            ProjectChannel.KEY_CHANNEL_NAME to data.channelName,
            ProjectChannel.KEY_CHANNEL_TYPE to data.channelType,
            ProjectChannel.KEY_ORDER to data.order,
            ProjectChannel.KEY_STATUS to data.status,
            ProjectChannel.KEY_CATEGORY_ID to data.categoryId,
        )
    }

    override fun mapToDto(map: Map<String, Any?>): ProjectChannelDTO {
        return ProjectChannelDTO(
            id = map["id"] as? String ?: "",
            channelName = map[ProjectChannel.KEY_CHANNEL_NAME] as? String ?: "",
            channelType = (map[ProjectChannel.KEY_CHANNEL_TYPE] as? String)?.let {
                ProjectChannelType.valueOf(
                    it
                )
            } ?: ProjectChannelType.TEXT,
            order = map[ProjectChannel.KEY_ORDER] as? Double ?: 0.0,
            status = (map[ProjectChannel.KEY_STATUS] as? String)?.let {
                ProjectChannelStatus.valueOf(
                    it
                )
            } ?: ProjectChannelStatus.ACTIVE,
            categoryId = map[ProjectChannel.KEY_CATEGORY_ID] as? String ?: "",
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}
