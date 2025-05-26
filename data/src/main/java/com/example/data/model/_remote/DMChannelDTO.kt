
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class DMChannelDTO(
    @DocumentId val id: String = "",
    // userId1, userId2 대신 참여자 목록으로 관리하면 확장성 및 쿼리에 유리합니다.
    val participants: List<String> = emptyList(),
    val lastMessagePreview: String? = null,
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

