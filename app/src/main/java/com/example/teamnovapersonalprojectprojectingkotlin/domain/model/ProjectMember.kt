package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

data class ProjectMember(
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val roleNames: List<String> // 멤버가 가진 역할 이름 목록 (예시)
    // 필요시 상태(온라인/오프라인) 등 추가
)