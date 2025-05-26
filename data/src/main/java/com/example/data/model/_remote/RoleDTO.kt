
package com.example.data.model._remote

import com.google.firebase.firestore.DocumentId

data class RoleDTO(
    @DocumentId val id: String = "",
    val name: String = "",
    val isDefault: Boolean = false // 기본 역할(Admin, Member 등)인지 커스텀 역할인지 구분
)

