package com.example.feature_main.ui

import com.example.domain.model.base.User
import com.example.domain.model.enum.UserStatus

/**
 * UI-specific data class for displaying user profile information.
 */
data class UserProfileData(
    val uid: String,
    val name: String,
    val email: String?,
    val profileImageUrl: String?,
    val memo: String?, // Mapped from User.memo
    val userStatus: UserStatus
)

fun User.toUserProfileData(): UserProfileData {
    return UserProfileData(
        uid = uid,
        name = name,
        email = email,
        profileImageUrl = profileImageUrl,
        memo = memo,
        userStatus = status
    )
}
