package com.example.data.model.collection

import com.example.data.model.remote.PermissionDTO
import com.example.data.model.remote.RoleDTO

data class RoleWithPermissionsCollectionDTO(
    val role: RoleDTO,
    val permissions: List<PermissionDTO>? = null
)
