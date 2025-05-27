package com.example.domain.model.base

import com.example.domain.model._new.enum.InviteStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

data class Invite(
    @DocumentId val id: String = "",
    val inviteCode: String = "", // 고유한 초대 코드
    val status: InviteStatus = InviteStatus.ACTIVE, // "ACTIVE", "INACTIVE", "EXPIRED"
    val createdBy: String = "", // 초대를 생성한 사용자의 ID
    val createdAt: Instant? = null,
    val expiresAt: Instant? = null // 만료 시간 (null이면 무제한)
)

