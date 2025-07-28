package com.example.mapper

import com.example.data_core.model.local.FriendsEntity
import com.example.domain.model.base.Friend
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface FriendEntityMapper : BaseEntityMapper<Friend, FriendsEntity>

class FriendEntityMapperImpl @Inject constructor() : FriendEntityMapper {
    override fun toDomain(entity: FriendsEntity): Friend {
        return Friend.fromDataSource(
            id = DocumentId(entity.id),
            name = UserName(entity.name),
            profileImageUrl = ImageUrl(entity.profileImageUrl),
            status = FriendStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Friend): FriendsEntity {
        return FriendsEntity(
            id = domain.id.value,
            name = domain.name.value,
            profileImageUrl = domain.profileImageUrl.value,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
