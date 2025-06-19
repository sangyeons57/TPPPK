package com.example.data.model.remote

import com.example.domain.model.base.ProjectsWrapper
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.google.firebase.firestore.ServerTimestamp
import com.example.domain.model.vo.DocumentId as VODocumentId

data class ProjectsWrapperDTO(
    @DocumentId val projectId: String = "",
    @get:PropertyName(ORDER)
    val order: Double = 0.0,
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) {

    companion object {
        const val COLLECTION_NAME = ProjectsWrapper.COLLECTION_NAME
        const val ORDER = ProjectsWrapper.KEY_ORDER
        const val CREATED_AT = ProjectsWrapper.KEY_CREATED_AT
        const val UPDATED_AT = ProjectsWrapper.KEY_UPDATED_AT
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectsWrapper 도메인 모델
     */
    fun toDomain(): ProjectsWrapper {
        return ProjectsWrapper.fromDataSource(
            id = VODocumentId(projectId),
            order = ProjectWrapperOrder(order),
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
        projectId = id.value,
        order = order.value,
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}