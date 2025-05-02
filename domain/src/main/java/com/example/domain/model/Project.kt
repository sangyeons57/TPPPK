// 경로: domain/model/Project.kt (HomeViewModel의 ProjectItem 기반)
package com.example.domain.model

data class Project(
    val id: String,
    val name: String,
    val description: String?, // 프로젝트 설명 추가
    val imageUrl: String?,    // 프로젝트 대표 이미지 URL 추가
    val memberCount: Int? = null, // 멤버 수 (선택적)
    val isPublic: Boolean = false // 공개 여부 (선택적)
    // 필요시 마지막 활동 시간, ownerId 등 추가
)