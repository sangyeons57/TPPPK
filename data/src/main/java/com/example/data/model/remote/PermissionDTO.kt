package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.base.Permission
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.permission.PermissionDescription
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * 권한 정보를 나타내는 DTO 클래스
 */
data class PermissionDTO(
    // 권한 이름 자체가 식별자로 사용되는 경우가 많습니다. 예: "CAN_EDIT_TASK"
    @DocumentId val id: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    @get:PropertyName(DESCRIPTION)
    val description: String = "",
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) : DTO {

    companion object {
        const val COLLECTION_NAME = Permission.COLLECTION_NAME// Sub-collection of Role
        const val NAME = Permission.KEY_NAME
        const val DESCRIPTION = Permission.KEY_DESCRIPTION
        const val CREATED_AT = Permission.KEY_CREATED_AT
        const val UPDATED_AT = Permission.KEY_UPDATED_AT

        fun from (domain: Permission): PermissionDTO {
            return PermissionDTO(
                id = domain.id.value,
                name = domain.name.value,
                description = domain.description.value,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(domain.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(domain.updatedAt)
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Permission 도메인 모델
     */
    override fun toDomain(): Permission {
        return Permission.fromDataSource(
            id = VODocumentId(id),
            name = Name(name),
            description = PermissionDescription(description),
            createdAt = createdAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)}
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
        name = name.value,
        description = description.value,
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
