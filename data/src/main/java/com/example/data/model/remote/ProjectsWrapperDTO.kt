package com.example.data.model.remote

import com.example.domain.model.base.ProjectsWrapper
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName


data class ProjectsWrapperDTO(
    @DocumentId val projectId: String = "",
    @get:PropertyName(ORDER)
    val order: String = ""
) {

    companion object {
        const val COLLECTION_NAME = "projects_wrapper"
        const val ORDER = "order"
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectsWrapper 도메인 모델
     */
    fun toDomain(): ProjectsWrapper {
        return ProjectsWrapper(
            projectId = projectId,
            order = order
        )
    }
}

/**
 * ProjectsWrapper 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectsWrapperDTO 객체
 */
fun ProjectsWrapper.toDto(): ProjectsWrapperDTO {
    return ProjectsWrapperDTO(
        projectId = projectId,
        order = order
    )
}