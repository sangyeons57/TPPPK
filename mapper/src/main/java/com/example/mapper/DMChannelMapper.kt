package com.example.mapper

import com.example.data.model.remote.DMChannelDTO
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.mapper.base.BaseMapper

interface DMChannelMapper : BaseMapper<DMChannel, DMChannelDTO>

class DMChannelMapperImpl : DMChannelMapper {
    override fun fromDto(dto: DMChannelDTO): DMChannel {
        return DMChannel.fromDataSource(
            id = DocumentId(dto.id),
            participants = dto.participants.map { UserId(it) },
            status = dto.status,
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: DMChannel): DMChannelDTO {
        return DMChannelDTO(
            id = domain.id.value,
            participants = domain.participants.map { it.value },
            status = domain.status
        )
    }

    override fun domainToMap(domain: DMChannel): Map<String, Any?> {
        return mapOf(
            DMChannel.KEY_PARTICIPANTS to domain.participants.map { it.value },
            DMChannel.KEY_STATUS to domain.status.value,
            DMChannel.KEY_BLOCKED_BY_MAP to domain.blockedByMap.mapKeys { it.key.value }
                .mapValues { it.value.value },
        )
    }

    override fun dataToMap(data: DMChannelDTO): Map<String, Any?> {
        return mapOf(
            DMChannel.KEY_PARTICIPANTS to data.participants,
            DMChannel.KEY_STATUS to data.status,
            DMChannel.KEY_BLOCKED_BY_MAP to emptyMap<String, String>(),
        )
    }
}
