package com.example.domain.model.base

import com.example.domain.model.enum.FriendStatus
import com.google.firebase.firestore.DocumentId
import java.time.Instant

data class Friend(
    @DocumentId val friendUid: String = "",
    val friendName: String = "",
    val friendProfileImageUrl: String? = null,
    // "requested", "accepted", "pending", "blocked"
    val status: FriendStatus = FriendStatus.PENDING,
    val requestedAt: Instant? = null,
    val acceptedAt: Instant? = null
)

