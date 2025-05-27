package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    // 순서를 소수점으로 관리하면 정수보다 유연하게 아이템 사이에 삽입할 수 있습니다.
    val order: Double = 0.0,
    val createdBy: String = "",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

