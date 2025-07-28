package com.example.mapper

import com.example.data_core.model.remote.ReactionDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.example.mapper.base.BaseMapper
import java.util.Date

interface ReactionMapper : BaseMapper<Reaction, ReactionDTO>

class ReactionMapperImpl : ReactionMapper {
    override fun toDomain(dto: ReactionDTO): Reaction {
        return Reaction.fromDataSource(
            id = DocumentId(dto.id),
            userId = UserId(dto.userId),
            emoji = Emoji(dto.emoji),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Reaction): ReactionDTO {
        return ReactionDTO(
            id = domain.id.value,
            userId = domain.userId.value,
            emoji = domain.emoji.value,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Reaction): Map<String, Any?> {
        return mapOf(
            Reaction.KEY_USER_ID to domain.userId.value,
            Reaction.KEY_EMOJI to domain.emoji.value,
        )
    }

    override fun dataToMap(data: ReactionDTO): Map<String, Any?> {
        return mapOf(
            Reaction.KEY_USER_ID to data.userId,
            Reaction.KEY_EMOJI to data.emoji,
        )
    }

    override fun mapToDto(map: Map<String, Any?>): ReactionDTO {
        return ReactionDTO(
            id = map["id"] as? String ?: "",
            userId = map[Reaction.KEY_USER_ID] as? String ?: "",
            emoji = map[Reaction.KEY_EMOJI] as? String ?: "",
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}
