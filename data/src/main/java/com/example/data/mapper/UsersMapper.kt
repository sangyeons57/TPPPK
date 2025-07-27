package com.example.data.mapper

import com.example.data.model.local.UsersEntity
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.enum.UserAccountStatus

/**
 * UsersEntity와 User 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object UsersMapper {

    /**
     * User 도메인 모델을 UsersEntity로 변환
     * @param user 변환할 User 도메인 모델
     * @return UsersEntity
     */
    fun toEntity(user: User): UsersEntity {
        return UsersEntity(
            id = user.id.value,
            email = user.email.value,
            name = user.name.value,
            consentTimeStamp = user.consentTimeStamp,
            memo = user.memo?.value,
            userStatus = user.userStatus.name,
            fcmToken = user.fcmToken?.value,
            accountStatus = user.accountStatus.name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }

    /**
     * UsersEntity를 User 도메인 모델로 변환
     * @param entity 변환할 UsersEntity
     * @return User 도메인 모델
     */
    fun toDomain(entity: UsersEntity): User {
        return User.fromDataSource(
            id = DocumentId(entity.id),
            email = UserEmail(entity.email),
            name = UserName(entity.name),
            consentTimeStamp = entity.consentTimeStamp,
            memo = entity.memo?.let { UserMemo(it) },
            userStatus = UserStatus.valueOf(entity.userStatus),
            fcmToken = entity.fcmToken?.let { UserFcmToken(it) },
            accountStatus = UserAccountStatus.valueOf(entity.accountStatus),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * User 도메인 모델 리스트를 UsersEntity 리스트로 변환
     * @param users 변환할 User 도메인 모델 리스트
     * @return UsersEntity 리스트
     */
    fun toEntityList(users: List<User>): List<UsersEntity> {
        return users.map { toEntity(it) }
    }

    /**
     * UsersEntity 리스트를 User 도메인 모델 리스트로 변환
     * @param entities 변환할 UsersEntity 리스트
     * @return User 도메인 모델 리스트
     */
    fun toDomainList(entities: List<UsersEntity>): List<User> {
        return entities.map { toDomain(it) }
    }
}