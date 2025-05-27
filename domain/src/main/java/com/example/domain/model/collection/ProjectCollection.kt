package com.example.domain.model.collection

import com.example.domain.model.Project
import com.example.domain.model.Member
import com.example.domain.model.Invite
// RoleCollection과 CategoryCollection은 같은 패키지 내에 있으므로 별도 import 불필요

data class ProjectCollection(
    val project: Project,
    val members: List<Member>? = null,
    val roles: List<RoleCollection>? = null, // RoleDetails -> RoleCollection
    val invites: List<Invite>? = null,
    val categories: List<CategoryCollection>? = null // CategoryDetails -> CategoryCollection
)
