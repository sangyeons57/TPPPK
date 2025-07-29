package com.example.data_core.model.remote

import com.example.domain.model.AggregateRoot
import com.example.domain.model.DTO
import com.example.domain.model.base.Project
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 프로젝트 정보를 나타내는 DTO 클래스
 */
data class ProjectDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    @get:PropertyName(IMAGE_URL)
    val imageUrl: String? = null,
    @get:PropertyName(STATUS)
    val status: String = "active",
    @get:PropertyName(OWNER_ID)
    val ownerId: String = "",
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Project.COLLECTION_NAME
        const val NAME = Project.KEY_NAME
        const val IMAGE_URL = Project.KEY_IMAGE_URL
        const val STATUS = Project.KEY_STATUS
        const val OWNER_ID = Project.KEY_OWNER_ID
    }
}