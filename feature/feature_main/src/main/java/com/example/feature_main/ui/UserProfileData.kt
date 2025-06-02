package com.example.feature_main.ui

import com.example.domain.model.enum.UserStatus

/**
 * UI-specific data class for displaying user profile information.
 */
data class UserProfileData(
    val uid: String,
    val name: String,
    val email: String?,
    val profileImageUrl: String?,
    val statusMessage: String?, // Mapped from User.memo
    val userStatus: UserStatus
)

fun User.toUserProfileData(): UserProfileData {
    return UserProfileData(
        uid = this.uid,
        name = this.name,
        email = this.email.ifEmpty { null },
        profileImageUrl = this.profileImageUrl,
        statusMessage = this.memo, // Mapping 'memo' to 'statusMessage'
        userStatus = this.status
    )
}
