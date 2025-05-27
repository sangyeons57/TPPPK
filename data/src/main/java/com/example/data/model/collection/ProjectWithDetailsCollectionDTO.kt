package com.example.data.model.collection

data class ProjectWithDetailsCollectionDTO(
    val project: ProjectDocumentDTO,
    val members: List<MemberDocumentDTO>? = null,
    val roles: List<RoleWithPermissionsCollectionDTO>? = null, // Role과 그 Permission을 함께 포함
    val invites: List<InviteDocumentDTO>? = null,
    val categories: List<CategoryWithChannelsCollectionDTO>? = null // Category와 그 Channel을 함께 포함
)
