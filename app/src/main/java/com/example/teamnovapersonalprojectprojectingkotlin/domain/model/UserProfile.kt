// 경로: domain/model/UserProfile.kt (신규 생성 또는 기존 User 모델 확장)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

data class UserProfile(
    val userId: String, // 사용자 고유 ID
    val email: String,
    val name: String,
    val profileImageUrl: String?,
    val statusMessage: String?
    // 필요시 다른 필드 추가
)