package com.example.mapper

import com.example.data_core.model.local.ProjectChannelsEntity
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelStatus
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface ProjectChannelEntityMapper : BaseEntityMapper<ProjectChannel, ProjectChannelsEntity>

class ProjectChannelEntityMapperImpl @Inject constructor() : ProjectChannelEntityMapper {
    override fun toDomain(entity: ProjectChannelsEntity): ProjectChannel {
        return ProjectChannel.fromDataSource(
            id = DocumentId(entity.id),
            categoryId = DocumentId(entity.categoryId),
            name = Name(entity.name),
            order = ProjectChannelOrder(entity.order),
            type = ProjectChannelType.valueOf(entity.type),
            status = ProjectChannelStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: ProjectChannel): ProjectChannelsEntity {
        return ProjectChannelsEntity(
            id = domain.id.value,
            categoryId = domain.categoryId.value,
            name = domain.name.value,
            order = domain.order.value,
            type = domain.type.name,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
