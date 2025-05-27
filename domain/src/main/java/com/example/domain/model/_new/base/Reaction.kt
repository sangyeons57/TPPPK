package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Reaction(
    @DocumentId val id: String = "",
    val userId: String = "", // 리액션을 남긴 사용자 ID
    val emoji: String = "",  // 유니코드 이모지
    val createdAt: Instant? = null
)

