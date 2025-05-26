
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class FriendDTO(
    @DocumentId val friendUid: String = "",
    val friendName: String = "",
    val friendProfileImageUrl: String? = null,
    // "requested", "accepted", "pending", "blocked"
    val status: String = "",
    val requestedAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null
)

