package com.example.data.model.remote

import com.example.domain.model.base.ProjectsWrapper
import com.google.firebase.firestore.DocumentId

data class ProjectsWrapperDTO(
    @DocumentId val projectId: String = ""
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectsWrapper 도메인 모델
     */
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectsWrapper 도메인 모델
     */
    fun toDomain(): ProjectsWrapper {
        return ProjectsWrapper(
            projectId = projectId
        )
    }
}

fun ProjectsWrapper.toDto(): ProjectsWrapperDTO {
    return ProjectsWrapperDTO(
        projectId = projectId
    )
}