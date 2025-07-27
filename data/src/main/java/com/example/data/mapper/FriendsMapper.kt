package com.example.data.mapper

import com.example.data.model.local.FriendsEntity
import com.example.domain.model.base.Friend
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.enum.FriendStatus

/**
 * FriendsEntity와 Friend 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object FriendsMapper {

    /**
     * Friend 도메인 모델을 FriendsEntity로 변환
     * @param friend 변환할 Friend 도메인 모델
     * @return FriendsEntity
     */
    fun toEntity(friend: Friend): FriendsEntity {
        return FriendsEntity(
            id = friend.id.value,
            status = friend.status.value,
            requestedAt = friend.requestedAt,
            acceptedAt = friend.acceptedAt,
            name = friend.name.value,
            profileImageUrl = friend.profileImageUrl?.value,
            createdAt = friend.createdAt,
            updatedAt = friend.updatedAt
        )
    }

    /**
     * FriendsEntity를 Friend 도메인 모델로 변환
     * @param entity 변환할 FriendsEntity
     * @return Friend 도메인 모델
     */
    fun toDomain(entity: FriendsEntity): Friend {
        return Friend.fromDataSource(
            id = DocumentId(entity.id),
            name = UserName(entity.name),
            profileImageUrl = entity.profileImageUrl?.let { ImageUrl(it) },
            status = FriendStatus.fromString(entity.status),
            requestedAt = entity.requestedAt,
            acceptedAt = entity.acceptedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Friend 도메인 모델 리스트를 FriendsEntity 리스트로 변환
     * @param friends 변환할 Friend 도메인 모델 리스트
     * @return FriendsEntity 리스트
     */
    fun toEntityList(friends: List<Friend>): List<FriendsEntity> {
        return friends.map { toEntity(it) }
    }

    /**
     * FriendsEntity 리스트를 Friend 도메인 모델 리스트로 변환
     * @param entities 변환할 FriendsEntity 리스트
     * @return Friend 도메인 모델 리스트
     */
    fun toDomainList(entities: List<FriendsEntity>): List<Friend> {
        return entities.map { toDomain(it) }
    }
}