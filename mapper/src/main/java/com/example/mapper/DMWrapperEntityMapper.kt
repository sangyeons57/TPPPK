package com.example.mapper

import com.example.data_core.model.local.DmWrapperEntity
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface DMWrapperEntityMapper : BaseEntityMapper<DMWrapper, DmWrapperEntity>

class DMWrapperEntityMapperImpl @Inject constructor() : DMWrapperEntityMapper {
    override fun toDomain(entity: DmWrapperEntity): DMWrapper {
        return DMWrapper.fromDataSource(
            id = DocumentId(entity.id),
            dmChannelId = DocumentId(entity.dmChannelId),
            currentUserId = UserId(entity.currentUserId),
            otherUserId = UserId(entity.otherUserId),
            otherUserName = UserName(entity.otherUserName),
            otherUserImageUrl = entity.otherUserImageUrl?.let { ImageUrl(it) },
            lastMessagePreview = entity.lastMessagePreview?.let { DMChannelLastMessagePreview(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: DMWrapper): DmWrapperEntity {
        return DmWrapperEntity(
            id = domain.id.value,
            dmChannelId = domain.dmChannelId.value,
            currentUserId = domain.currentUserId.value,
            otherUserId = domain.otherUserId.value,
            otherUserName = domain.otherUserName.value,
            otherUserImageUrl = domain.otherUserImageUrl?.value,
            lastMessagePreview = domain.lastMessagePreview?.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
