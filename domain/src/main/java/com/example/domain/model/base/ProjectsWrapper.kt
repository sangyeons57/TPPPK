package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId

data class ProjectsWrapper(
    @DocumentId val projectId: String = "",
    val order: String = ""
)

