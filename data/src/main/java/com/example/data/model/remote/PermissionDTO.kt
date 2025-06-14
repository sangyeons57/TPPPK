package com.example.data.model.remote

import com.example.domain.model.base.Permission
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.core_common.constants.FirestoreConstants

/**
 * 권한 정보를 나타내는 DTO 클래스
 */
data class PermissionDTO(
    // 권한 이름 자체가 식별자로 사용되는 경우가 많습니다. 예: "CAN_EDIT_TASK"
    @DocumentId val id: String = "", 
    @get:PropertyName(FirestoreConstants.Project.Permissions.NAME)
    val name: String = "",
    @get:PropertyName(FirestoreConstants.Project.Permissions.DESCRIPTION)
    val description: String = ""
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return Permission 도메인 모델
     */
    fun toDomain(): Permission {
        return Permission(
            id = id,
            name = name,
            description = description
        )
    }
}

/**
 * Permission 도메인 모델을 DTO로 변환하는 확장 함수
 * @return PermissionDTO 객체
 */
fun Permission.toDto(): PermissionDTO {
    return PermissionDTO(
        id = id,
        name = name,
        description = description
    )
}
