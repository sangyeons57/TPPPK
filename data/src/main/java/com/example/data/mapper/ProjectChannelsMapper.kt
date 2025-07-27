package com.example.data.mapper

import com.example.data.model.local.ProjectChannelsEntity
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.enum.ProjectChannelStatus

/**
 * ProjectChannelsEntity와 ProjectChannel 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object ProjectChannelsMapper {

    /**
     * ProjectChannel 도메인 모델을 ProjectChannelsEntity로 변환
     * @param projectChannel 변환할 ProjectChannel 도메인 모델
     * @return ProjectChannelsEntity
     */
    fun toEntity(projectChannel: ProjectChannel): ProjectChannelsEntity {
        return ProjectChannelsEntity(
            id = projectChannel.id.value,
            channelName = projectChannel.channelName.value,
            channelType = projectChannel.channelType.value,
            order = projectChannel.order.value,
            status = projectChannel.status.value,
            categoryId = projectChannel.categoryId.value,
            createdAt = projectChannel.createdAt,
            updatedAt = projectChannel.updatedAt
        )
    }

    /**
     * ProjectChannelsEntity를 ProjectChannel 도메인 모델로 변환
     * @param entity 변환할 ProjectChannelsEntity
     * @return ProjectChannel 도메인 모델
     */
    fun toDomain(entity: ProjectChannelsEntity): ProjectChannel {
        return ProjectChannel.fromDataSource(
            id = DocumentId(entity.id),
            channelName = Name(entity.channelName),
            order = ProjectChannelOrder(entity.order),
            channelType = ProjectChannelType.fromString(entity.channelType),
            status = ProjectChannelStatus.fromString(entity.status),
            categoryId = DocumentId(entity.categoryId),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * ProjectChannel 도메인 모델 리스트를 ProjectChannelsEntity 리스트로 변환
     * @param projectChannels 변환할 ProjectChannel 도메인 모델 리스트
     * @return ProjectChannelsEntity 리스트
     */
    fun toEntityList(projectChannels: List<ProjectChannel>): List<ProjectChannelsEntity> {
        return projectChannels.map { toEntity(it) }
    }

    /**
     * ProjectChannelsEntity 리스트를 ProjectChannel 도메인 모델 리스트로 변환
     * @param entities 변환할 ProjectChannelsEntity 리스트
     * @return ProjectChannel 도메인 모델 리스트
     */
    fun toDomainList(entities: List<ProjectChannelsEntity>): List<ProjectChannel> {
        return entities.map { toDomain(it) }
    }
}