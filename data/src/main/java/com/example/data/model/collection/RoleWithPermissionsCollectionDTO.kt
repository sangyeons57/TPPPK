package com.example.data.model.collection

data class RoleWithPermissionsCollectionDTO(
    val role: RoleDocumentDTO,
    val permissions: List<PermissionDocumentDTO>? = null
)
