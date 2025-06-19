package com.example.data.model.remote

import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.core_common.util.DateTimeUtil

import com.google.firebase.firestore.PropertyName

/*
 * 친구 관계 정보를 나타내는 DTO 클래스
 */
data class FriendDTO(
    @DocumentId val friendUid: String = "",
    // "requested", "accepted", "pending", "blocked"
    @get:PropertyName(STATUS)
    val status: FriendStatus = FriendStatus.UNKNOWN,
    @get:PropertyName(REQUESTED_AT)
    val requestedAt: Timestamp? = null,
    @get:PropertyName(ACCEPTED_AT)
    val acceptedAt: Timestamp? = null
) {

    companion object {
        const val COLLECTION_NAME = "friends"
        const val STATUS = "status" // "PENDING_SENT", "PENDING_RECEIVED", "ACCEPTED", "DECLINED", "BLOCKED"
        const val REQUESTED_AT = "requestedAt"
        const val ACCEPTED_AT = "acceptedAt"
    }
    /*
     * DTO를 도메인 모델로 변환
     * @return Friend 도메인 모델
     */
    fun toDomain(): Friend {
        return Friend(
            friendUid = friendUid,
            status = status,
            requestedAt = requestedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            acceptedAt = acceptedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/*
 * Friend 도메인 모델을 DTO로 변환하는 확장 함수
 * @return FriendDTO 객체
 */
fun Friend.toDto(): FriendDTO {
    return FriendDTO(
        friendUid = friendUid,
        status = status,
        requestedAt = requestedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        acceptedAt = acceptedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
