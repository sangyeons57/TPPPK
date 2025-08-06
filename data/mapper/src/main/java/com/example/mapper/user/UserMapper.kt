package com.example.mapper.user

import com.example.data_model.remote.UserDTO
import com.example.domain.model.base.User
import com.example.domain.vo.DocumentId
import com.example.domain.vo.user.UserEmail
import com.example.domain.vo.user.UserFcmToken
import com.example.domain.vo.user.UserMemo
import com.example.domain.vo.user.UserName
import com.example.mapper.DtoMapper
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 * Entity가 없으므로 DtoMapper 인터페이스 구현
 */
@Singleton
class UserMapper @Inject constructor() : DtoMapper<User, UserDTO> {

    override fun dtoToDomain(dto: UserDTO): User {
        return User.fromDataSource(
            id = DocumentId(dto.id),
            email = UserEmail(dto.email),
            name = UserName(dto.name),
            consentTimeStamp = dto.consentTimeStamp?.toInstant() ?: Instant.EPOCH,
            memo = dto.memo?.let { UserMemo(it) },
            userStatus = dto.status,
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
            fcmToken = dto.fcmToken?.let { UserFcmToken(it) },
            accountStatus = dto.accountStatus
        )
    }

    override fun domainToDto(domain: User): UserDTO {
        return UserDTO(
            id = domain.id.value,
            email = domain.email.value,
            name = domain.name.value,
            consentTimeStamp = Date.from(domain.consentTimeStamp),
            memo = domain.memo?.value,
            status = domain.userStatus,
            fcmToken = domain.fcmToken?.value,
            accountStatus = domain.accountStatus,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}