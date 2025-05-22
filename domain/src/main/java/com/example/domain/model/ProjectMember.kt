package com.example.domain.model

import java.time.Instant
import com.example.domain.model.Role

data class ProjectMember(
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val roles: List<Role>,
    val joinedAt: Instant
    // 필요시 상태(온라인/오프라인) 등 추가
)