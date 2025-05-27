package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Friend(
    @DocumentId val friendUid: String = "",
    val friendName: String = "",
    val friendProfileImageUrl: String? = null,
    // "requested", "accepted", "pending", "blocked"
    val status: FriendStatus = FriendStatus.PENDING,
    val requestedAt: Instant? = null,
    val acceptedAt: Instant? = null
)

