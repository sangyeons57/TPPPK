package com.example.mapper

import com.example.data_core.model.local.ReactionsEntity
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface ReactionEntityMapper : BaseEntityMapper<Reaction, ReactionsEntity>

class ReactionEntityMapperImpl @Inject constructor() : ReactionEntityMapper {
    override fun toDomain(entity: ReactionsEntity): Reaction {
        return Reaction.fromDataSource(
            id = DocumentId(entity.id),
            messageId = DocumentId(entity.messageId),
            userId = UserId(entity.userId),
            emoji = Emoji(entity.emoji),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Reaction): ReactionsEntity {
        return ReactionsEntity(
            id = domain.id.value,
            messageId = domain.messageId.value,
            userId = domain.userId.value,
            emoji = domain.emoji.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
