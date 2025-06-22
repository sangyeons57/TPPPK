package com.example.data.model

import com.example.domain.event.AggregateRoot

interface DTO {
    @get:com.google.firebase.firestore.DocumentId
    val id: String

    fun toDomain() : AggregateRoot
}