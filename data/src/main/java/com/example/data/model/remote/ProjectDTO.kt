package com.example.data.model.remote

import com.example.domain.model.base.Project
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

import com.google.firebase.firestore.PropertyName

/**
 * 프로젝트 정보를 나타내는 DTO 클래스
 */
data class ProjectDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    @get:PropertyName(IMAGE_URL)
    val imageUrl: String? = null,
    @get:PropertyName(OWNER_ID)
    val ownerId: String = "",
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null
) {

    companion object {
        const val COLLECTION_NAME = "projects"
        const val NAME = "name"
        const val IMAGE_URL = "imageUrl"
        const val OWNER_ID = "ownerId"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Project 도메인 모델
     */
    fun toDomain(): Project {
        return Project(
            id = id,
            name = name,
            imageUrl = imageUrl,
            ownerId = ownerId,
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * Project 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectDTO 객체
 */
fun Project.toDto(): ProjectDTO {
    return ProjectDTO(
        id = id,
        name = name,
        imageUrl = imageUrl,
        ownerId = ownerId,
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}