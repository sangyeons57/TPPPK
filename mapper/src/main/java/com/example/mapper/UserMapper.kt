package com.example.mapper

import com.example.data.model.remote.UserDTO
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserName
import com.example.mapper.base.BaseMapper
import java.time.Instant
import java.util.Date

interface UserMapper : BaseMapper<User, UserDTO>

class UserMapperImpl : UserMapper {
    override fun fromDto(dto: UserDTO): User {
        return User.fromDataSource(
            id = DocumentId(dto.id),
            email = UserEmail(dto.email), // Wrap in Value Object
            name = UserName(dto.name),   // Wrap in Value Object
            consentTimeStamp = dto.consentTimeStamp?.toInstant()
                ?: Instant.EPOCH, // Convert Date to Instant
            memo = dto.memo?.let { UserMemo(it) }, // Wrap in Value Object
            userStatus = dto.status,
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
            fcmToken = UserFcmToken(dto.fcmToken),
            accountStatus = dto.accountStatus
        )
    }

    override fun toDto(domain: User): UserDTO {
        return UserDTO(
            id = domain.id.value,
            email = domain.email.value, // Extract primitive value
            name = domain.name.value,   // Extract primitive value
            consentTimeStamp = Date.from(domain.consentTimeStamp), // Convert Instant to Date
            memo = domain.memo?.value,  // Extract primitive value if memo is not null
            status = domain.userStatus, // Corrected from 'status' to 'userStatus'
            fcmToken = domain.fcmToken?.value,
            accountStatus = domain.accountStatus
        )
    }

    override fun domainToMap(domain: User): Map<String, Any?> {
        return mapOf(
            User.KEY_EMAIL to domain.email.value,
            User.KEY_NAME to domain.name.value,
            User.KEY_CONSENT_TIMESTAMP to domain.consentTimeStamp,
            User.KEY_MEMO to domain.memo?.value,
            User.KEY_USER_STATUS to domain.userStatus,
            User.KEY_FCM_TOKEN to domain.fcmToken?.value,
            User.KEY_ACCOUNT_STATUS to domain.accountStatus
        )
    }

    override fun dataToMap(data: UserDTO): Map<String, Any?> {
        return mapOf(
            User.KEY_EMAIL to data.email,
            User.KEY_NAME to data.name,
            User.KEY_CONSENT_TIMESTAMP to data.consentTimeStamp,
            User.KEY_MEMO to data.memo,
            User.KEY_USER_STATUS to data.status,
            User.KEY_FCM_TOKEN to data.fcmToken,
            User.KEY_ACCOUNT_STATUS to data.accountStatus
        )
    }
}
