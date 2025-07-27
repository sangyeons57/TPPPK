package com.example.mapper

import com.example.data.model.remote.FriendDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Friend
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.example.mapper.base.BaseMapper
import java.util.Date

interface FriendMapper : BaseMapper<Friend, FriendDTO>

class FriendMapperImpl : FriendMapper {
    override fun toDomain(dto: FriendDTO): Friend {
        return Friend.fromDataSource(
            id = DocumentId(dto.id),
            status = dto.status,
            requestedAt = dto.requestedAt?.toInstant(),
            acceptedAt = dto.acceptedAt?.toInstant(),
            name = UserName(dto.name),
            profileImageUrl = dto.profileImageUrl?.let { ImageUrl(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Friend): FriendDTO {
        return FriendDTO(
            id = domain.id.value,
            status = domain.status,
            requestedAt = domain.requestedAt?.let { Date.from(it) },
            acceptedAt = domain.acceptedAt?.let { Date.from(it) },
            name = domain.name.value,
            profileImageUrl = domain.profileImageUrl?.value,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Friend): Map<String, Any?> {
        return mapOf(
            Friend.KEY_STATUS to domain.status,
            Friend.KEY_REQUESTED_AT to domain.requestedAt,
            Friend.KEY_ACCEPTED_AT to domain.acceptedAt,
            Friend.KEY_NAME to domain.name.value,
            Friend.KEY_PROFILE_IMAGE_URL to domain.profileImageUrl?.value,
        )
    }

    override fun dataToMap(data: FriendDTO): Map<String, Any?> {
        return mapOf(
            Friend.KEY_STATUS to data.status,
            Friend.KEY_REQUESTED_AT to data.requestedAt,
            Friend.KEY_ACCEPTED_AT to data.acceptedAt,
            Friend.KEY_NAME to data.name,
            Friend.KEY_PROFILE_IMAGE_URL to data.profileImageUrl,
        )
    }

    override fun mapToDto(map: Map<String, Any?>): FriendDTO {
        return FriendDTO(
            id = map["id"] as? String ?: "",
            status = (map[Friend.KEY_STATUS] as? String)?.let { FriendStatus.valueOf(it) }
                ?: FriendStatus.PENDING,
            requestedAt = (map[Friend.KEY_REQUESTED_AT] as? Date),
            acceptedAt = (map[Friend.KEY_ACCEPTED_AT] as? Date),
            name = map[Friend.KEY_NAME] as? String ?: "",
            profileImageUrl = map[Friend.KEY_PROFILE_IMAGE_URL] as? String,
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}
