package com.example.data.mapper

import com.example.data.model.local.DmWrapperEntity
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName

/**
 * DmWrapperEntity와 DMWrapper 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object DmWrapperMapper {

    /**
     * DMWrapper 도메인 모델을 DmWrapperEntity로 변환
     * @param dmWrapper 변환할 DMWrapper 도메인 모델
     * @return DmWrapperEntity
     */
    fun toEntity(dmWrapper: DMWrapper): DmWrapperEntity {
        return DmWrapperEntity(
            id = dmWrapper.id.value,
            otherUserId = dmWrapper.otherUserId.value,
            otherUserName = dmWrapper.otherUserName.value,
            otherUserImageUrl = dmWrapper.otherUserImageUrl?.value,
            lastMessagePreview = dmWrapper.lastMessagePreview?.value,
            dmChannelId = "", // TODO: Add dmChannelId to DMWrapper domain model
            unreadCount = 0, // TODO: Add unreadCount to DMWrapper domain model
            lastReadAt = null, // TODO: Add lastReadAt to DMWrapper domain model
            createdAt = dmWrapper.createdAt,
            updatedAt = dmWrapper.updatedAt
        )
    }

    /**
     * DmWrapperEntity를 DMWrapper 도메인 모델로 변환
     * @param entity 변환할 DmWrapperEntity
     * @return DMWrapper 도메인 모델
     */
    fun toDomain(entity: DmWrapperEntity): DMWrapper {
        return DMWrapper.fromDataSource(
            id = DocumentId(entity.id),
            otherUserId = UserId(entity.otherUserId),
            otherUserName = UserName(entity.otherUserName),
            otherUserImageUrl = entity.otherUserImageUrl?.let { ImageUrl(it) },
            lastMessagePreview = entity.lastMessagePreview?.let { DMChannelLastMessagePreview(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * DMWrapper 도메인 모델 리스트를 DmWrapperEntity 리스트로 변환
     * @param dmWrappers 변환할 DMWrapper 도메인 모델 리스트
     * @return DmWrapperEntity 리스트
     */
    fun toEntityList(dmWrappers: List<DMWrapper>): List<DmWrapperEntity> {
        return dmWrappers.map { toEntity(it) }
    }

    /**
     * DmWrapperEntity 리스트를 DMWrapper 도메인 모델 리스트로 변환
     * @param entities 변환할 DmWrapperEntity 리스트
     * @return DMWrapper 도메인 모델 리스트
     */
    fun toDomainList(entities: List<DmWrapperEntity>): List<DMWrapper> {
        return entities.map { toDomain(it) }
    }
}