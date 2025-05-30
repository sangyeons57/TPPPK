package com.example.data.model.remote

import com.example.domain.model.base.ProjectsWrapper
import com.google.firebase.firestore.DocumentId

data class ProjectsWrapperDTO(
    @DocumentId val projectId: String = "",
    val projectName: String = "",
    val projectImageUrl: String? = null
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectsWrapper 도메인 모델
     */
    fun toDomain(): ProjectsWrapper {
        return ProjectsWrapper(
            projectId = projectId,
            projectName = projectName,
            projectImageUrl = projectImageUrl
        )
    }
}

fun ProjectsWrapper.toDto(): ProjectsWrapperDTO {
    return ProjectsWrapperDTO(
        projectId = projectId,
        projectName = projectName,
        projectImageUrl = projectImageUrl
    )
}