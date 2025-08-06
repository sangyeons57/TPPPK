package com.example.mapper.dm

import com.example.data_model.remote.DMChannelDTO
import com.example.domain.model.base.DMChannel
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DMChannel 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class DMChannelMapper @Inject constructor() : DtoMapper<DMChannel, DMChannelDTO> {

    override fun dtoToDomain(dto: DMChannelDTO): DMChannel {
        return DMChannel.fromDataSource(
            id = DocumentId(dto.id),
            participants = dto.participants.map { UserId(it) },
            status = dto.status,
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: DMChannel): DMChannelDTO {
        return DMChannelDTO(
            id = domain.id.value,
            participants = domain.participants.map { it.value },
            status = domain.status,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}