package com.example.mapper

import com.example.data_model.local.DmChannelsEntity
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface DMChannelEntityMapper : BaseEntityMapper<DMChannel, DmChannelsEntity>

class DMChannelEntityMapperImpl @Inject constructor() : DMChannelEntityMapper {
    override fun toDomain(entity: DmChannelsEntity): DMChannel {
        return DMChannel.fromDataSource(
            id = DocumentId(entity.id),
            user1Id = UserId(entity.user1Id),
            user2Id = UserId(entity.user2Id),
            status = DMChannelStatus.valueOf(entity.status),
            lastMessageId = DocumentId(entity.lastMessageId),
            lastMessageTimestamp = entity.lastMessageTimestamp,
            isBlocked = entity.isBlocked,
            blockedByUserId = entity.blockedByUserId?.let { UserId(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: DMChannel): DmChannelsEntity {
        return DmChannelsEntity(
            id = domain.id.value,
            user1Id = domain.user1Id.value,
            user2Id = domain.user2Id.value,
            status = domain.status.name,
            lastMessageId = domain.lastMessageId.value,
            lastMessageTimestamp = domain.lastMessageTimestamp,
            isBlocked = domain.isBlocked,
            blockedByUserId = domain.blockedByUserId?.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
