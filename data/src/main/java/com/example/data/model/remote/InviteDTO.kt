package com.example.data.model.remote


import com.example.domain.model.base.Invite
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.OwnerId
import com.google.firebase.firestore.PropertyName
import java.util.Date

/*
 * 초대 정보를 나타내는 DTO 클래스
 */
data class InviteDTO(
    @DocumentId 
    override val id: String = "",
    @get:PropertyName(STATUS)
     val status: InviteStatus = InviteStatus.ACTIVE, // "ACTIVE", "INACTIVE", "EXPIRED"
    @get:PropertyName(CREATED_BY)
     val createdBy: String = "", // 초대를 생성한 사용자의 ID
    @get:PropertyName(EXPIRES_AT)
     val expiresAt: Timestamp? = null, // 만료 시간 (null이면 무제한)
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null,
) : DTO {

    companion object {
        const val COLLECTION_NAME = Invite.COLLECTION_NAME
        const val INVITE_LINK = Invite.KEY_INVITE_LINK
        const val STATUS = Invite.KEY_STATUS
        const val CREATED_BY = Invite.KEY_CREATED_BY
        const val EXPIRES_AT = Invite.KEY_EXPIRES_AT
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Invite 도메인 모델
     */
    override fun toDomain(): Invite  {
        return Invite.fromDataSource (
            id = VODocumentId(id),
            status = status,
            createdBy = OwnerId(createdBy),
            createdAt = createdAt?.toInstant(),
            expiresAt = expiresAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.toInstant()
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
        status = status,
        createdBy = createdBy.value,
        expiresAt = expiresAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        createdAt = null,
        updatedAt = null
    )
}
