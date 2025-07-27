package com.example.data.mapper

import com.example.data.model.local.DmChannelsEntity
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.enum.DMChannelStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * DmChannelsEntity와 DMChannel 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object DmChannelsMapper {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * DMChannel 도메인 모델을 DmChannelsEntity로 변환
     * @param dmChannel 변환할 DMChannel 도메인 모델
     * @return DmChannelsEntity
     */
    fun toEntity(dmChannel: DMChannel): DmChannelsEntity {
        return DmChannelsEntity(
            id = dmChannel.id.value,
            participants = json.encodeToString(dmChannel.participants.map { it.value }),
            status = dmChannel.status.value,
            blockedByMap = if (dmChannel.blockedByMap.isNotEmpty()) {
                json.encodeToString(dmChannel.blockedByMap.mapKeys { it.key.value }
                    .mapValues { it.value.value })
            } else null,
            createdAt = dmChannel.createdAt,
            updatedAt = dmChannel.updatedAt
        )
    }

    /**
     * DmChannelsEntity를 DMChannel 도메인 모델로 변환
     * @param entity 변환할 DmChannelsEntity
     * @return DMChannel 도메인 모델
     */
    fun toDomain(entity: DmChannelsEntity): DMChannel {
        val participantsList = json.decodeFromString<List<String>>(entity.participants)
            .map { UserId(it) }

        val blockedByMap = entity.blockedByMap?.let { blockedMapJson ->
            json.decodeFromString<Map<String, String>>(blockedMapJson)
                .mapKeys { UserId(it.key) }
                .mapValues { UserId(it.value) }
        } ?: emptyMap()

        return DMChannel.fromDataSource(
            id = DocumentId(entity.id),
            participants = participantsList,
            status = DMChannelStatus.fromString(entity.status),
            blockedByMap = blockedByMap,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * DMChannel 도메인 모델 리스트를 DmChannelsEntity 리스트로 변환
     * @param dmChannels 변환할 DMChannel 도메인 모델 리스트
     * @return DmChannelsEntity 리스트
     */
    fun toEntityList(dmChannels: List<DMChannel>): List<DmChannelsEntity> {
        return dmChannels.map { toEntity(it) }
    }

    /**
     * DmChannelsEntity 리스트를 DMChannel 도메인 모델 리스트로 변환
     * @param entities 변환할 DmChannelsEntity 리스트
     * @return DMChannel 도메인 모델 리스트
     */
    fun toDomainList(entities: List<DmChannelsEntity>): List<DMChannel> {
        return entities.map { toDomain(it) }
    }
}