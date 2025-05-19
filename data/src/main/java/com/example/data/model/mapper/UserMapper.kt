package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.user.UserDto
import com.example.domain.model.AccountStatus // Ensure this import is correct
import com.example.domain.model.User
import com.example.domain.model.UserStatus // Ensure this import is correct
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import javax.inject.Inject

/**
 * UserDto와 User 도메인 모델 간의 변환을 처리하는 매퍼 클래스
 */
class UserMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {

    /**
     * Firestore DocumentSnapshot을 User 도메인 모델로 변환합니다.
     * DTO의 ID(@DocumentId)가 User 모델의 ID로 매핑됩니다.
     */
    fun mapToDomain(document: DocumentSnapshot): User? {
        return try {
            val dto = document.toObject(UserDto::class.java)
            // UserDto.id (@DocumentId) is automatically populated from snapshot's ID
            // User.id will be set from dto.id in toDomainModelWithTime
            dto?.toDomainModelWithTime(dateTimeUtil)
        } catch (e: Exception) {
            // Consider logging the exception, e.g., Log.e("UserMapper", "Error mapping snapshot to User: ${e.message}")
            null
        }
    }

    /**
     * UserDto를 User 도메인 모델로 변환합니다.
     * DTO의 ID가 User 모델의 ID로 매핑됩니다.
     */
    fun mapToDomain(dto: UserDto): User {
        return dto.toDomainModelWithTime(dateTimeUtil)
    }

    /**
     * User 도메인 모델을 UserDto로 변환합니다.
     * User 모델의 ID가 DTO의 ID로 매핑됩니다.
     */
    fun mapToDto(domainModel: User): UserDto {
        return domainModel.toDtoWithTime(dateTimeUtil)
    }

    /**
     * UserDto 목록을 User 도메인 모델 목록으로 변환합니다.
     */
    fun mapToDomainList(dtoList: List<UserDto>): List<User> {
        return dtoList.map { mapToDomain(it) }
    }
}

/**
 * UserDto를 User 도메인 모델로 변환합니다.
 * DateTimeUtil을 사용하여 Timestamp를 Instant로 변환하고, Enum 문자열을 Enum 타입으로 변환합니다.
 * UserDto.id (문서 ID)가 User.id로 매핑됩니다.
 */
fun UserDto.toDomainModelWithTime(dateTimeUtil: DateTimeUtil): User {
    // UserDto.toBasicDomainModel() correctly maps this.id (from DTO) to User.id
    val basicDomain = this.toBasicDomainModel() 
    return basicDomain.copy(
        status = this.status, // Uses UserStatus.fromString
        createdAt = this.createdAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } 
            ?: basicDomain.createdAt, // Use existing instant from domain if DTO's is null (e.g. Instant.EPOCH or User.kt default)
        updatedAt = this.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) }, // Nullable Instant
        accountStatus = this.accountStatus, // Uses AccountStatus.fromString
        consentTimeStamp = this.consentTimeStamp?.let { dateTimeUtil.firebaseTimestampToInstant(it) }
    )
}

/**
 * User 도메인 모델을 UserDto로 변환합니다.
 * DateTimeUtil을 사용하여 Instant를 Timestamp로 변환하고, Enum 타입을 문자열로 변환합니다.
 * User.id가 UserDto.id로 매핑됩니다.
 */
fun User.toDtoWithTime(dateTimeUtil: DateTimeUtil): UserDto {
    // UserDto.fromBasicDomainModel() correctly maps domain.id to UserDto.id
    val basicDto = UserDto.fromBasicDomainModel(this) 
    return basicDto.copy(
        status = this.status, // Convert enum to string for Firestore
        createdAt = this.createdAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }, // Respect domain's createdAt
        updatedAt = this.updatedAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }, // Nullable Timestamp
        accountStatus = this.accountStatus, // Convert enum to string for Firestore
        consentTimeStamp = this.consentTimeStamp?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }
    )
}
