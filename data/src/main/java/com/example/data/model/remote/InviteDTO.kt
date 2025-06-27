package com.example.data.model.remote


import com.example.domain.model.base.Invite
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.invite.InviteCode
import com.google.firebase.firestore.PropertyName

/*
 * 초대 정보를 나타내는 DTO 클래스
 */
data class InviteDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(INVITE_LINK) val inviteCode: String = "", // 고유한 초대 코드, 상수명은 INVITE_LINK
    @get:PropertyName(STATUS) val status: InviteStatus = InviteStatus.ACTIVE, // "ACTIVE", "INACTIVE", "EXPIRED"
    @get:PropertyName(CREATED_BY) val createdBy: String = "", // 초대를 생성한 사용자의 ID
    @get:PropertyName(EXPIRES_AT) val expiresAt: Timestamp? = null, // 만료 시간 (null이면 무제한)
    @get:PropertyName(CREATED_AT) val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT) val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
) : DTO {

    companion object {
        const val COLLECTION_NAME = Invite.COLLECTION_NAME
        const val INVITE_LINK = Invite.KEY_INVITE_LINK
        const val STATUS = Invite.KEY_STATUS
        const val CREATED_BY = Invite.KEY_CREATED_BY
        const val CREATED_AT = Invite.KEY_CREATED_AT
        const val UPDATED_AT = Invite.KEY_UPDATED_AT
        const val EXPIRES_AT = Invite.KEY_EXPIRES_AT

        fun from(invite: Invite): InviteDTO {
            return InviteDTO(
                id = invite.id.value,
                inviteCode = invite.inviteCode.value,
                status = invite.status,
                createdBy = invite.createdBy.value,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(invite.createdAt),
                expiresAt = invite.expiresAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(invite.updatedAt),
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Invite 도메인 모델
     */
    override fun toDomain(): Invite  {
        return Invite.fromDataSource (
            id = VODocumentId(id),
            inviteCode = InviteCode(inviteCode),
            status = status,
            createdBy = OwnerId(createdBy),
            createdAt = createdAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            expiresAt = expiresAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * Invite 도메인 모델을 DTO로 변환하는 확장 함수
 * @return InviteDTO 객체
 */
fun Invite.toDto(): InviteDTO {
    return InviteDTO(
        id = id.value,
        inviteCode = inviteCode.value,
        status = status,
        createdBy = createdBy.value,
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        expiresAt = expiresAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
