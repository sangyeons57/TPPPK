package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Member(
    @DocumentId val userId: String = "",
    val joinedAt: Instant? = null,
    // 다이어그램의 관계를 바탕으로 멤버가 어떤 역할을 가졌는지 식별하기 위한 필드를 추가합니다.
    val roleId: String = ""
)

