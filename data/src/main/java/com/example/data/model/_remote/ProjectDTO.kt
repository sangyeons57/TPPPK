
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ProjectDTO(
    @DocumentId val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val isPublic: Boolean = false
)

