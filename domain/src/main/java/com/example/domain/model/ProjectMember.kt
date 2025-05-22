package com.example.domain.model

data class ProjectMember(
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val roleIds: List<String> // 멤버가 가진 역할 ID 목록
    // 필요시 상태(온라인/오프라인) 등 추가
)