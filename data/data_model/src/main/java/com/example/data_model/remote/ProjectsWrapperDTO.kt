package com.example.data_model.remote

import com.example.domain.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectsWrapper
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ProjectsWrapperDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(ORDER)
    val order: Double = Category.NO_CATEGORY_ORDER.toDouble(),
    @get:PropertyName(PROJECT_NAME)
    val projectName: String = "",
    @get:PropertyName(PROJECT_IMAGE_URL)
    val projectImageUrl: String? = null,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = ProjectsWrapper.COLLECTION_NAME
        const val ORDER = ProjectsWrapper.KEY_ORDER
        const val PROJECT_NAME = ProjectsWrapper.KEY_PROJECT_NAME
        const val PROJECT_IMAGE_URL = ProjectsWrapper.KEY_PROJECT_IMAGE_URL
    }
}

