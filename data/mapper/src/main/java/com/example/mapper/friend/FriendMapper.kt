package com.example.mapper.friend

import com.example.data_model.remote.FriendDTO
import com.example.domain.model.base.Friend
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ImageUrl
import com.example.domain.vo.user.UserName
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Friend 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class FriendMapper @Inject constructor() : DtoMapper<Friend, FriendDTO> {

    override fun dtoToDomain(dto: FriendDTO): Friend {
        return Friend.fromDataSource(
            id = DocumentId(dto.id),
            name = UserName(dto.name),
            profileImageUrl = dto.profileImageUrl?.let { ImageUrl(it) },
            status = dto.status,
            requestedAt = dto.requestedAt?.toInstant(),
            acceptedAt = dto.acceptedAt?.toInstant(),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Friend): FriendDTO {
        return FriendDTO(
            id = domain.id.value,
            name = domain.name.value,
            profileImageUrl = domain.profileImageUrl?.value,
            status = domain.status,
            requestedAt = null, // ServerTimestamp가 처리
            acceptedAt = null,  // ServerTimestamp가 처리
            createdAt = null,   // ServerTimestamp가 처리
            updatedAt = null    // ServerTimestamp가 처리
        )
    }
}