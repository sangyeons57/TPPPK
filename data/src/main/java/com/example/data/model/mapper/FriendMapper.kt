package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.friend.FriendRelationshipDto
import com.example.domain.model.FriendRelationship
import com.example.domain.model.FriendRequestStatus
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import javax.inject.Inject

/**
 * FriendRelationshipDto와 FriendRelationship 도메인 모델 간의 변환을 담당하는 매퍼 클래스.
 */
class FriendMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {

    /**
     * Firestore DocumentSnapshot을 FriendRelationship 도메인 모델로 변환합니다.
     * ownerUserId는 이 친구 목록이 속한 사용자의 ID이며, 외부에서 제공되어야 합니다.
     * 문서 ID (snapshot.id)는 친구의 ID (friendUserId)로 사용됩니다.
     */
    fun mapToDomain(document: DocumentSnapshot, ownerUserId: String): FriendRelationship? {
        return try {
            val dto = document.toObject(FriendRelationshipDto::class.java)
            // dto.friendId is automatically populated with document.id
            dto?.toDomainModelWithTime(ownerUserId, dateTimeUtil)
        } catch (e: Exception) {
            // Log error
            null
        }
    }

    /**
     * FriendRelationshipDto를 FriendRelationship 도메인 모델로 변환합니다.
     * ownerUserId는 이 친구 목록이 속한 사용자의 ID이며, 외부에서 제공되어야 합니다.
     */
    fun mapToDomain(dto: FriendRelationshipDto, ownerUserId: String): FriendRelationship {
        return dto.toDomainModelWithTime(ownerUserId, dateTimeUtil)
    }

    /**
     * FriendRelationship 도메인 모델을 FriendRelationshipDto로 변환합니다.
     * DTO의 friendId는 도메인 모델의 friendUserId에서 가져옵니다.
     * ownerUserId는 DTO에 포함되지 않습니다 (컬렉션 경로의 일부).
     */
    fun mapToDto(domain: FriendRelationship): FriendRelationshipDto {
        return domain.toDtoWithTime(dateTimeUtil)
    }
}

/**
 * FriendRelationshipDto를 FriendRelationship 도메인 모델로 변환합니다.
 * ownerUserId는 DTO에 없으므로 매개변수로 받아야 합니다.
 * DateTimeUtil과 Enum.fromString을 사용합니다.
 */
fun FriendRelationshipDto.toDomainModelWithTime(ownerUserId: String, dateTimeUtil: DateTimeUtil): FriendRelationship {
    // DTO.toBasicDomainModel() maps this.friendId to domain.friendUserId
    val basicDomain = this.toBasicDomainModel(ownerUserId) 
    return basicDomain.copy(
        // ownerUserId and friendUserId are set by toBasicDomainModel
        status = this.status, // Uses FriendRequestStatus.fromString
        timestamp = this.timestamp?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        acceptedAt = this.acceptedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) }
    )
}

/**
 * FriendRelationship 도메인 모델을 FriendRelationshipDto로 변환합니다.
 * DateTimeUtil과 Enum.name을 사용합니다.
 * ownerUserId는 DTO에 포함되지 않습니다.
 */
fun FriendRelationship.toDtoWithTime(dateTimeUtil: DateTimeUtil): FriendRelationshipDto {
    // DTO.fromBasicDomainModel() maps domain.friendUserId to DTO.friendId
    val basicDto = FriendRelationshipDto.fromBasicDomainModel(this) 
    return basicDto.copy(
        // friendId is set by fromBasicDomainModel
        status = this.status, // Enum to String
        timestamp = dateTimeUtil.instantToFirebaseTimestamp(this.timestamp),
        acceptedAt = this.acceptedAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }
    )
} 