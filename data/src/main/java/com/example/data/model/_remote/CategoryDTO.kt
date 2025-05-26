
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class CategoryDTO(
    @DocumentId val id: String = "",
    val name: String = "",
    // 순서를 소수점으로 관리하면 정수보다 유연하게 아이템 사이에 삽입할 수 있습니다.
    val order: Double = 0.0,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

