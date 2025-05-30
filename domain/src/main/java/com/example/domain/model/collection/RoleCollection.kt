package com.example.domain.model.collection

import com.example.domain.model.base.Permission
import com.example.domain.model.base.Role

data class RoleCollection(
    val role: Role,
    val permissions: List<Permission>? = null
)
