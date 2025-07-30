package com.example.mapper.reaction

import com.example.data_model.remote.ReactionDTO
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reaction 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class ReactionMapper @Inject constructor() : DtoMapper<Reaction, ReactionDTO> {

    override fun dtoToDomain(dto: ReactionDTO): Reaction {
        return Reaction.fromDataSource(
            id = DocumentId(dto.id),
            userId = UserId(dto.userId),
            emoji = Emoji(dto.emoji),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Reaction): ReactionDTO {
        return ReactionDTO(
            id = domain.id.value,
            userId = domain.userId.value,
            emoji = domain.emoji.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}