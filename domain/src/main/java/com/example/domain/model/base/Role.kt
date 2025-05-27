package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId

data class Role(
    @DocumentId val id: String = "",
    val name: String = "",
    val isDefault: Boolean = false // 기본 역할(Admin, Member 등)인지 커스텀 역할인지 구분
)

