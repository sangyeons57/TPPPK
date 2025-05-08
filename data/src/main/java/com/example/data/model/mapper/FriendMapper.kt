package com.example.data.model.mapper

import com.example.data.model.remote.friend.FriendRelationshipDto
import com.example.domain.model.FriendRelationship
import java.util.Date
import com.google.firebase.Timestamp

/**
 * FriendRelationshipDto와 FriendRelationship 도메인 모델 간의 변환을 담당하는 매퍼입니다.
 */
object FriendMapper {

    /**
     * FriendRelationshipDto를 FriendRelationship 도메인 모델로 변환합니다.
     * Firestore 문서 ID인 friendId는 이 함수 외부에서 DTO와 함께 받아와서 설정해야 합니다.
     *
     * @param dto 변환할 FriendRelationshipDto 객체.
     * @param friendId 이 관계에 해당하는 친구의 ID (Firestore 문서 ID).
     * @return 변환된 FriendRelationship 도메인 모델.
     */
    fun dtoToDomain(dto: FriendRelationshipDto, friendId: String): FriendRelationship {
        val timestampDate = dto.timestamp?.toDate() ?: Date(0)
        val acceptedAtDate = dto.acceptedAt?.toDate()

        return FriendRelationship(
            friendId = friendId,
            status = dto.status ?: "",
            timestamp = timestampDate,
            acceptedAt = acceptedAtDate
        )
    }

    /**
     * FriendRelationship 도메인 모델을 FriendRelationshipDto로 변환합니다.
     * Firestore에 저장하기 위한 형태입니다. friendId는 DTO에 포함되지 않습니다 (문서 ID로 사용).
     *
     * @param domain 변환할 FriendRelationship 도메인 모델.
     * @return 변환된 FriendRelationshipDto 객체.
     */
    fun domainToDto(domain: FriendRelationship): FriendRelationshipDto {
        val firestoreTimestamp = Timestamp(domain.timestamp)
        val firestoreAcceptedAt = domain.acceptedAt?.let { Timestamp(it) }

        return FriendRelationshipDto(
            status = domain.status,
            timestamp = firestoreTimestamp,
            acceptedAt = firestoreAcceptedAt
        )
    }
} 