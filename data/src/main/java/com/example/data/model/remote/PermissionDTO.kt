package com.example.data.model.remote

import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 권한 정보를 나타내는 DTO 클래스
 */
data class PermissionDTO(
    // 권한 이름 자체가 식별자로 사용되는 경우가 많습니다. 예: "CAN_EDIT_TASK"
    @DocumentId override val id: String = "",
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Permission.COLLECTION_NAME// Sub-collection of Role
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Permission 도메인 모델
     */
    override fun toDomain(): Permission {
        return Permission.fromDataSource(
            id = RolePermission.from(id),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
        )
    }
}

/**
 * Permission 도메인 모델을 DTO로 변환하는 확장 함수
 * @return PermissionDTO 객체
 */
fun Permission.toDto(): PermissionDTO {
    return PermissionDTO(
        id = id.value,
        createdAt = Date.from(createdAt),
        updatedAt = Date.from(updatedAt)
    )
}
