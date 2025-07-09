package com.example.data.model.collection

import com.example.data.model.remote.MemberDTO
import com.example.data.model.remote.ProjectDTO

data class ProjectWithDetailsCollectionDTO(
    val project: ProjectDTO,
    val members: List<MemberDTO>? = null,
    val roles: List<RoleWithPermissionsCollectionDTO>? = null, // Role과 그 Permission을 함께 포함
    val categories: List<CategoryWithChannelsCollectionDTO>? = null // Category와 그 Channel을 함께 포함
)
