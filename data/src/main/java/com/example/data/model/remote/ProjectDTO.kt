package com.example.data.model.remote

import com.example.domain.model.base.Project
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName
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
        const val OWNER_ID = Project.KEY_OWNER_ID


    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Project 도메인 모델
     */
    override fun toDomain(): Project {
        return Project.fromDataSource(
            id = VODocumentId(id),
            name = ProjectName(name),
            imageUrl = imageUrl?.let{ImageUrl(it)},
            ownerId = OwnerId(ownerId),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * Project 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectDTO 객체
 */
fun Project.toDto(): ProjectDTO {
    return ProjectDTO(
        id = id.value,
        name = name.value,
        imageUrl = imageUrl?.value,
        ownerId = ownerId.value,
        createdAt = Date.from(createdAt),
        updatedAt = Date.from(updatedAt)
    )
}