// 경로: domain/model/Project.kt (HomeViewModel의 ProjectItem 기반)
package com.example.domain.model

import java.time.Instant

/**
 * 프로젝트 정보를 나타내는 도메인 모델
 */
data class Project(
    val id: String,
    val name: String,
    val description: String?, // 프로젝트 설명
    val imageUrl: String?,    // 프로젝트 대표 이미지 URL
    // val memberCount: Int? = null, // 멤버 수는 memberIds.size로 계산 가능
    val isPublic: Boolean, // 공개 여부
    // [ Add ] Dto와 맞추기 위한 필드 추가
    val ownerId: String, // 소유자 ID
    val memberIds: List<String>, // 멤버 ID 목록
    val createdAt: Instant, // 생성 시간 (Instant 사용)
    val updatedAt: Instant // 마지막 업데이트 시간 (Instant 사용)
)