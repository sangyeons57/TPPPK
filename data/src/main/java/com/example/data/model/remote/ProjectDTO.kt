package com.example.data.model.remote

import com.example.domain.model.base.Project
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/**
 * 프로젝트 정보를 나타내는 DTO 클래스
 */
data class ProjectDTO(
    @DocumentId val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
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