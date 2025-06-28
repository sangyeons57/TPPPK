package com.example.data.model.remote

import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/*
 * 친구 관계 정보를 나타내는 DTO 클래스
 */
data class FriendDTO(
    @DocumentId override val id: String = "",
    // "requested", "accepted", "pending", "blocked"
    @get:PropertyName(STATUS)
    val status: FriendStatus = FriendStatus.UNKNOWN,
    @get:PropertyName(REQUESTED_AT)
    @get:ServerTimestamp val requestedAt: Date? = null, 
    @get:PropertyName(ACCEPTED_AT)
    @get:ServerTimestamp val acceptedAt: Date? = null,
    @get:PropertyName(NAME)
    val name: String = "",
    @get:PropertyName(PROFILE_IMAGE_URL)
    val profileImageUrl: String? = null,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Friend.COLLECTION_NAME
        const val STATUS = Friend.KEY_STATUS
        const val REQUESTED_AT = Friend.KEY_REQUESTED_AT
        const val ACCEPTED_AT = Friend.KEY_ACCEPTED_AT
        const val NAME = Friend.KEY_NAME
        const val PROFILE_IMAGE_URL = Friend.KEY_PROFILE_IMAGE_URL
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Friend 도메인 모델
     */
    override fun toDomain(): Friend {
        return Friend.fromDataSource(
            id = VODocumentId(id),
            status = status,
            requestedAt = requestedAt?.toInstant(),
            acceptedAt = acceptedAt?.toInstant(),
            name = Name(name),
            profileImageUrl = profileImageUrl?.let{ ImageUrl(it) },
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/*
 * Friend 도메인 모델을 DTO로 변환하는 확장 함수
 * @return FriendDTO 객체
 */
fun Friend.toDto(): FriendDTO {
    return FriendDTO(
        id = id.value,
        status = status,
        requestedAt = requestedAt?.let{Date.from(it)},
        acceptedAt = acceptedAt?.let{Date.from(it)},
        name = name.value,
        profileImageUrl = profileImageUrl?.value,
        createdAt = Date.from(createdAt),
        updatedAt = Date.from(updatedAt)
    )
}
