package com.example.data.model.mapper

import com.example.data.remote.dto.user.UserDto
import com.example.domain.model.AccountStatus
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import java.util.Date
import javax.inject.Inject

/**
 * UserDto와 User 도메인 모델 간의 변환을 처리하는 매퍼 클래스
 */
class UserMapper @Inject constructor() {

    /**
     * UserDto를 User 도메인 모델로 변환합니다.
     *
     * @param dto 변환할 UserDto 객체
     * @return 변환된 User 도메인 모델
     */
    fun mapToDomain(dto: UserDto): User {
        return User(
            userId = dto.userId,
            email = dto.email,
            name = dto.name,
            profileImageUrl = dto.profileImageUrl,
            memo = dto.memo,
            status = try {
                UserStatus.valueOf(dto.status.uppercase())
            } catch (e: IllegalArgumentException) {
                UserStatus.OFFLINE
            },
            createdAt = Date(dto.createdAt.seconds * 1000),
            fcmToken = dto.fcmToken,
            participatingProjectIds = dto.participatingProjectIds,
            accountStatus = try {
                AccountStatus.valueOf(dto.accountStatus.uppercase())
            } catch (e: IllegalArgumentException) {
                AccountStatus.ACTIVE
            },
            activeDmIds = dto.activeDmIds,
            isEmailVerified = dto.isEmailVerified
        )
    }

    /**
     * User 도메인 모델을 UserDto로 변환합니다.
     *
     * @param domainModel 변환할 User 도메인 모델
     * @return 변환된 UserDto 객체
     */
    fun mapToDto(domainModel: User): UserDto {
        return UserDto(
            userId = domainModel.userId,
            email = domainModel.email,
            name = domainModel.name,
            profileImageUrl = domainModel.profileImageUrl,
            memo = domainModel.memo,
            status = domainModel.status.name,
            createdAt = com.google.firebase.Timestamp(domainModel.createdAt.time / 1000, 0),
            fcmToken = domainModel.fcmToken,
            participatingProjectIds = domainModel.participatingProjectIds,
            accountStatus = domainModel.accountStatus.name,
            activeDmIds = domainModel.activeDmIds,
            isEmailVerified = domainModel.isEmailVerified
        )
    }

    /**
     * UserDto 목록을 User 도메인 모델 목록으로 변환합니다.
     *
     * @param dtoList 변환할 UserDto 목록
     * @return 변환된 User 도메인 모델 목록
     */
    fun mapToDomainList(dtoList: List<UserDto>): List<User> {
        return dtoList.map { mapToDomain(it) }
    }
} 