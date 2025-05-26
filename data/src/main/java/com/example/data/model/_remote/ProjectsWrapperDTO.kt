
package com.example.data.model._remote

import com.google.firebase.firestore.DocumentId

data class ProjectsWrapperDTO(
    @DocumentId val projectId: String = "",
    val projectName: String = "",
    val projectImageUrl: String? = null
)

