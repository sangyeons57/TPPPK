package com.example.data.model.remote

import com.example.domain.model.base.Role
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.core_common.constants.FirestoreConstants

/**
 * 역할 정보를 나타내는 DTO 클래스
 */
data class RoleDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(FirestoreConstants.Project.Roles.NAME)
    val name: String = "",
    @get:PropertyName(FirestoreConstants.Project.Roles.IS_DEFAULT)
    val isDefault: Boolean = false // 기본 역할(Admin, Member 등)인지 커스텀 역할인지 구분
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return Role 도메인 모델
     */
    fun toDomain(): Role {
        return Role(
            id = id,
            name = name,
            isDefault = isDefault
        )
    }
}

/**
 * Role 도메인 모델을 DTO로 변환하는 확장 함수
 * @return RoleDTO 객체
 */
fun Role.toDto(): RoleDTO {
    return RoleDTO(
        id = id,
        name = name,
        isDefault = isDefault
    )
}
