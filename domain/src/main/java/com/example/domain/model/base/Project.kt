package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

data class Project(
    @DocumentId val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val ownerId: String = "",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

