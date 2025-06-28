package com.example.data.model

import com.example.domain.model.AggregateRoot
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

interface DTO {
    @get:com.google.firebase.firestore.DocumentId
    val id: String

    /** Standard timestamp fields for all DTOs - automatically managed by Firestore */
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp
    val createdAt: Date?

    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp
    val updatedAt: Date?

    fun toDomain() : AggregateRoot
}