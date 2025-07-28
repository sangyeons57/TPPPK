package com.example.mapper

import com.example.data_core.model.local.UsersEntity
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface UserEntityMapper : BaseEntityMapper<User, UsersEntity>

class UserEntityMapperImpl @Inject constructor() : UserEntityMapper {
    override fun toDomain(entity: UsersEntity): User {
        return User.fromDataSource(
            id = DocumentId(entity.id),
            email = UserEmail(entity.email),
            name = UserName(entity.name),
            profileImageUrl = entity.profileImageUrl?.let { ImageUrl(it) },
            memo = entity.memo,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: User): UsersEntity {
        return UsersEntity(
            id = domain.id.value,
            email = domain.email.value,
            name = domain.name.value,
            profileImageUrl = domain.profileImageUrl?.value,
            memo = domain.memo,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
