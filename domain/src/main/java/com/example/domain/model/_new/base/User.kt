package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val name: String = "",
    val consentTimeStamp: Instant? = null,
    val profileImageUrl: String? = null,
    val memo: String? = null,
    val status: UserStatus = UserStatus.OFFLINE, // "online", "offline", "away" 등
    val createdAt: Instant = Instant.now(),
    val fcmToken: String? = null,
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE // "active", "suspended", "deleted" 등
)