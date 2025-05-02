// 경로: domain/model/User.kt
package com.example.domain.model

data class User(
    val userId: String = "", // Firestore needs default values for toObject()
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val status: String? = null,
    val statusMessage: String? = null,
    // Add other fields stored in Firestore
)
