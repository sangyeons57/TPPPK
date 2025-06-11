package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId
import java.time.Instant

data class DMWrapper(
    @DocumentId val dmChannelId: String = "",
    val otherUserId: String = ""
)

