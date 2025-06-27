package com.example.data.model.remote

import com.example.domain.model.base.Project
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName

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
    @get:PropertyName(CREATED_AT)
    val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) : DTO {

    companion object {
        const val COLLECTION_NAME = Project.COLLECTION_NAME
        const val NAME = Project.KEY_NAME
        const val IMAGE_URL = Project.KEY_IMAGE_URL
        const val OWNER_ID = Project.KEY_OWNER_ID
        const val CREATED_AT = Project.KEY_CREATED_AT
        const val UPDATED_AT = Project.KEY_UPDATED_AT
        fun from (project: Project): ProjectDTO {
            return ProjectDTO(
                id = project.id.value,
                name = project.name.value,
                imageUrl = project.imageUrl?.value,
                ownerId = project.ownerId.value,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(project.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(project.updatedAt),
            )
        }
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
            createdAt = createdAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)}
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
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}