package com.example.domain.model

data class UserProfileData (
    val userId: String,
    val name: String,
    val email: String, // 이전 ProfileViewModel과 병합
    val statusMessage: String,
    val profileImageUrl: String?
)