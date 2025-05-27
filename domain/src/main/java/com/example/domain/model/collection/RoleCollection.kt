package com.example.domain.model.collection

import com.example.domain.model.Role
import com.example.domain.model.Permission

data class RoleCollection(
    val role: Role,
    val permissions: List<Permission>? = null
)
