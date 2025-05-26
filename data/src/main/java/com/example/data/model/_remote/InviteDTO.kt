
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class InviteDTO(
    @DocumentId val id: String = "",
    val inviteCode: String = "", // 고유한 초대 코드
    val status: String = "ACTIVE", // "ACTIVE", "INACTIVE", "EXPIRED"
    val createdBy: String = "", // 초대를 생성한 사용자의 ID
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null // 만료 시간 (null이면 무제한)
)

