
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class DMWrapper(
    @DocumentId val dmChannelId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserProfileImageUrl: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Instant? = null
)

