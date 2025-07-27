package com.example.data.mapper

import com.example.data.model.local.ReactionsEntity
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji

/**
 * ReactionsEntity와 Reaction 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object ReactionsMapper {

    /**
     * Reaction 도메인 모델을 ReactionsEntity로 변환
     * @param reaction 변환할 Reaction 도메인 모델
     * @param messageId 반응이 속한 메시지 ID
     * @return ReactionsEntity
     */
    fun toEntity(reaction: Reaction, messageId: String): ReactionsEntity {
        return ReactionsEntity(
            id = reaction.id.value,
            messageId = messageId,
            userId = reaction.userId.value,
            emoji = reaction.emoji.value,
            createdAt = reaction.createdAt,
            updatedAt = reaction.updatedAt
        )
    }

    /**
     * ReactionsEntity를 Reaction 도메인 모델로 변환
     * @param entity 변환할 ReactionsEntity
     * @return Reaction 도메인 모델
     */
    fun toDomain(entity: ReactionsEntity): Reaction {
        return Reaction.fromDataSource(
            id = DocumentId(entity.id),
            userId = UserId(entity.userId),
            emoji = Emoji(entity.emoji),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Reaction 도메인 모델 리스트를 ReactionsEntity 리스트로 변환
     * @param reactions 변환할 Reaction 도메인 모델 리스트
     * @param messageId 반응들이 속한 메시지 ID
     * @return ReactionsEntity 리스트
     */
    fun toEntityList(reactions: List<Reaction>, messageId: String): List<ReactionsEntity> {
        return reactions.map { toEntity(it, messageId) }
    }

    /**
     * ReactionsEntity 리스트를 Reaction 도메인 모델 리스트로 변환
     * @param entities 변환할 ReactionsEntity 리스트
     * @return Reaction 도메인 모델 리스트
     */
    fun toDomainList(entities: List<ReactionsEntity>): List<Reaction> {
        return entities.map { toDomain(it) }
    }
}