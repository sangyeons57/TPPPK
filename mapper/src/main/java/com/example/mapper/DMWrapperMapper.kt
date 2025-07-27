package com.example.mapper

import com.example.data.model.remote.DMWrapperDTO
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName
import com.example.mapper.base.BaseMapper

interface DMWrapperMapper : BaseMapper<DMWrapper, DMWrapperDTO>

class DMWrapperMapperImpl : DMWrapperMapper {
    override fun fromDto(dto: DMWrapperDTO): DMWrapper {
        return DMWrapper.fromDataSource(
            id = DocumentId(dto.id),
            otherUserId = UserId(dto.otherUserId),
            otherUserName = UserName(dto.otherUserName),
            otherUserImageUrl = dto.otherUserImageUrl?.let { ImageUrl(it) },
            lastMessagePreview = dto.lastMessagePreview?.let { DMChannelLastMessagePreview(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: DMWrapper): DMWrapperDTO {
        return DMWrapperDTO(
            id = domain.id.value,
            otherUserId = domain.otherUserId.value,
            otherUserName = domain.otherUserName.value,
            otherUserImageUrl = domain.otherUserImageUrl?.value,
            lastMessagePreview = domain.lastMessagePreview?.value,
        )
    }

    override fun domainToMap(domain: DMWrapper): Map<String, Any?> {
        return mapOf(
            DMWrapper.KEY_OTHER_USER_ID to domain.otherUserId.value,
            DMWrapper.KEY_OTHER_USER_NAME to domain.otherUserName.value,
            DMWrapper.KEY_OTHER_USER_IMAGE_URL to domain.otherUserImageUrl?.value,
            DMWrapper.KEY_LAST_MESSAGE_PREVIEW to domain.lastMessagePreview?.value,
        )
    }

    override fun dataToMap(data: DMWrapperDTO): Map<String, Any?> {
        return mapOf(
            DMWrapper.KEY_OTHER_USER_ID to data.otherUserId,
            DMWrapper.KEY_OTHER_USER_NAME to data.otherUserName,
            DMWrapper.KEY_OTHER_USER_IMAGE_URL to data.otherUserImageUrl,
            DMWrapper.KEY_LAST_MESSAGE_PREVIEW to data.lastMessagePreview,
        )
    }
}
