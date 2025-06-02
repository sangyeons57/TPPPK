package com.example.domain.model.ui.data

import java.time.Instant

data class MemberUiModel(
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val roleNames: List<String>, // List of role names for display
    val joinedAt: Instant? // Keep joinedAt if needed, or remove
)