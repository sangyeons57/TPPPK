// 경로: domain/model/Role.kt (신규 생성)
package com.example.domain.model

/**
 * 프로젝트 내 역할을 나타내는 Domain 모델
 */
data class Role(
    val id: String?, // 역할의 고유 ID (생성 시에는 null일 수 있음)
    val projectId: String, // 역할이 속한 프로젝트 ID
    val name: String, // 역할 이름
    val permissions: List<RolePermission>, // 새 필드: 보유한 권한 목록
    val memberCount: Int? = null, // 이 역할을 가진 멤버 수 (선택적)
    val isDefault: Boolean = false // 기본 역할 여부
    // 필요시 역할 색상 등 추가
)