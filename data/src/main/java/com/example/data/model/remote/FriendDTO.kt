package com.example.data.model.remote

import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name

import com.google.firebase.firestore.PropertyName

/*
 * 친구 관계 정보를 나타내는 DTO 클래스
 */
data class FriendDTO(
    @DocumentId val id: String = "",
    // "requested", "accepted", "pending", "blocked"
    @get:PropertyName(STATUS)
    val status: FriendStatus = FriendStatus.UNKNOWN,
    @get:PropertyName(REQUESTED_AT)
    val requestedAt: Timestamp? = null,
    @get:PropertyName(ACCEPTED_AT)
    val acceptedAt: Timestamp? = null,
    @get:PropertyName(NAME)
    val name: String = "",
    @get:PropertyName(PROFILE_IMAGE_URL)
    val profileImageUrl: String? = null,
    @get:PropertyName(CREATED_AT)
    val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) {

    companion object {
        const val COLLECTION_NAME = Friend.COLLECTION_NAME
        const val STATUS = Friend.KEY_STATUS
        const val REQUESTED_AT = Friend.KEY_REQUESTED_AT
        const val ACCEPTED_AT = Friend.KEY_ACCEPTED_AT
        const val NAME = Friend.KEY_NAME
        const val PROFILE_IMAGE_URL = Friend.KEY_PROFILE_IMAGE_URL
        const val CREATED_AT = Friend.KEY_CREATED_AT
        const val UPDATED_AT = Friend.KEY_UPDATED_AT
    }
    /*
     * DTO를 도메인 모델로 변환
     * @return Friend 도메인 모델
     */
    fun toDomain(): Friend {
        return Friend.fromDataSource(
            id = VODocumentId(id),
            status = status,
            requestedAt = DateTimeUtil.firebaseTimestampToInstant(requestedAt),
            acceptedAt = DateTimeUtil.firebaseTimestampToInstant(acceptedAt),
            name = Name(name),
            profileImageUrl = profileImageUrl?.let{ ImageUrl(it) },
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt),
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt)
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
        requestedAt = requestedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        acceptedAt = acceptedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        name = name.value,
        profileImageUrl = profileImageUrl?.value,
        createdAt = DateTimeUtil.instantToFirebaseTimestamp(createdAt),
        updatedAt = DateTimeUtil.instantToFirebaseTimestamp(updatedAt)
    )
}
