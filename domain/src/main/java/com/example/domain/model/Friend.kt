package com.example.domain.model

import com.google.firebase.firestore.DocumentId

data class Friend (
    var userId: String,
    var userName: String,
    var status: String,
    var profileImageUrl: String?
)
