package com.example.domain.model.base

import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.google.firebase.firestore.DocumentId
import java.time.Instant

data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val name: String = "",
    val consentTimeStamp: Instant? = null,
    val profileImageUrl: String? = null,
    val memo: String? = null,
    val status: UserStatus = UserStatus.OFFLINE, // "online", "offline", "away" 등
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val fcmToken: String? = null,
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE // "active", "suspended", "deleted" 등
)