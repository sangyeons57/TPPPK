package com.example.data.model.remote

import com.example.domain.model._new.enum.InviteStatus
import com.example.domain.model.base.Invite
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/*
 * 초대 정보를 나타내는 DTO 클래스
 */
data class InviteDTO(
    @DocumentId val id: String = "",
    val inviteCode: String = "", // 고유한 초대 코드
    val status: String = "ACTIVE", // "ACTIVE", "INACTIVE", "EXPIRED"
    val createdBy: String = "", // 초대를 생성한 사용자의 ID
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null // 만료 시간 (null이면 무제한)
) {
    /*
     * DTO를 도메인 모델로 변환
     * @return Invite 도메인 모델
     */
    fun toDomain(): Invite {
        return Invite(
            id = id,
            inviteCode = inviteCode,
            status = try {
                InviteStatus.valueOf(status.uppercase())
            } catch (e: Exception) {
                InviteStatus.ACTIVE
            },
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
        status = status.name.lowercase(),
        createdBy = createdBy,
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        expiresAt = expiresAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
