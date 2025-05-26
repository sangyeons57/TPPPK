
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ReactionDTO(
    @DocumentId val id: String = "",
    val userId: String = "", // 리액션을 남긴 사용자 ID
    val emoji: String = "",  // 유니코드 이모지
    @ServerTimestamp val createdAt: Timestamp? = null
)

