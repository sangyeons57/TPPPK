package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class DMChannel(
    @DocumentId val id: String = "",
    // userId1, userId2 대신 참여자 목록으로 관리하면 확장성 및 쿼리에 유리합니다.
    val participants: List<String> = emptyList(),
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

