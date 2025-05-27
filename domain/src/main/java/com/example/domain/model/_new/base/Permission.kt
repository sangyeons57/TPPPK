package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId

data class Permission(
    // 권한 이름 자체가 식별자로 사용되는 경우가 많습니다. 예: "CAN_EDIT_TASK"
    @DocumentId val id: String = "", 
    val name: String = "",
    val description: String = ""
)

