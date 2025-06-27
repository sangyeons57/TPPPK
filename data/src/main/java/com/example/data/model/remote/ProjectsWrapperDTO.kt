package com.example.data.model.remote

import com.example.domain.model.base.ProjectsWrapper
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.google.firebase.firestore.ServerTimestamp
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.project.ProjectName

data class ProjectsWrapperDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(ORDER)
    val order: Double = 0.0,
    @get:PropertyName(PROJECT_NAME)
    val projectName: String = "",
    @get:PropertyName(PROJECT_IMAGE_URL)
    val projectImageUrl: String? = null,
    @get:PropertyName(CREATED_AT)
    val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) : DTO {

    companion object {
        const val COLLECTION_NAME = ProjectsWrapper.COLLECTION_NAME
        const val ORDER = ProjectsWrapper.KEY_ORDER
        const val PROJECT_NAME = ProjectsWrapper.KEY_PROJECT_NAME
        const val PROJECT_IMAGE_URL = ProjectsWrapper.KEY_PROJECT_IMAGE_URL
        const val CREATED_AT = ProjectsWrapper.KEY_CREATED_AT
        const val UPDATED_AT = ProjectsWrapper.KEY_UPDATED_AT

        fun from (projectsWrapper: ProjectsWrapper): ProjectsWrapperDTO {
            return ProjectsWrapperDTO(
                id = projectsWrapper.id.value,
                order = projectsWrapper.order.value,
                projectName = projectsWrapper.projectName.value,
                projectImageUrl = projectsWrapper.projectImageUrl?.value,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(projectsWrapper.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(projectsWrapper.updatedAt),
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectsWrapper 도메인 모델
     */
    override fun toDomain(): ProjectsWrapper {
        return ProjectsWrapper.fromDataSource(
            id = VODocumentId(id),
            order = ProjectWrapperOrder(order),
            projectName = ProjectName(projectName),
            projectImageUrl = projectImageUrl?.let{ImageUrl(it)},
            createdAt = createdAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * ProjectsWrapper 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectsWrapperDTO 객체
 */
fun ProjectsWrapper.toDto(): ProjectsWrapperDTO {
    return ProjectsWrapperDTO(
        id = id.value,
        order = order.value,
        projectName = projectName.value,
        projectImageUrl = projectImageUrl?.value,
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}