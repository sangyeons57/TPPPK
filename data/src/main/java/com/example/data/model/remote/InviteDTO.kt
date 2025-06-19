package com.example.data.model.remote


import com.example.domain.model._new.enum.InviteStatus
import com.example.domain.model.base.Invite
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.google.firebase.firestore.PropertyName

/*
 * 초대 정보를 나타내는 DTO 클래스
 */
data class InviteDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(INVITE_LINK) val inviteCode: String = "", // 고유한 초대 코드, 상수명은 INVITE_LINK
    @get:PropertyName(STATUS) val status: InviteStatus = InviteStatus.ACTIVE, // "ACTIVE", "INACTIVE", "EXPIRED"
    @get:PropertyName(CREATED_BY) val createdBy: String = "", // 초대를 생성한 사용자의 ID
    @get:PropertyName(CREATED_AT) @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(EXPIRES_AT) val expiresAt: Timestamp? = null // 만료 시간 (null이면 무제한)
) {

    companion object {
        const val COLLECTION_NAME = "invites"
        const val INVITE_LINK = "inviteCode"
        const val STATUS = "status"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val EXPIRES_AT = "expiresAt"
    }
    /*
     * DTO를 도메인 모델로 변환
     * @return Invite 도메인 모델
     */
    fun toDomain(): Invite {
        return Invite(
            id = id,
            inviteCode = inviteCode,
            status = status,
            createdBy = createdBy,
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            expiresAt = expiresAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/*
 * Invite 도메인 모델을 DTO로 변환하는 확장 함수
 * @return InviteDTO 객체
 */
fun Invite.toDto(): InviteDTO {
    return InviteDTO(
        id = id,
        inviteCode = inviteCode,
        status = status,
        createdBy = createdBy,
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        expiresAt = expiresAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
