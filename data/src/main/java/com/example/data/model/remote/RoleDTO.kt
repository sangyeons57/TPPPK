package com.example.data.model.remote

import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

/**
 * 역할 정보를 나타내는 DTO 클래스
 */
data class RoleDTO(
    @DocumentId var id: String = "",
    @get:PropertyName(NAME) var name: String = "",
    @get:PropertyName(IS_DEFAULT) var isDefault: Boolean = false,
    @ServerTimestamp @get:PropertyName(CREATED_AT) var createdAt: Timestamp? = null,
    @ServerTimestamp @get:PropertyName(UPDATED_AT) var updatedAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_NAME = "roles"
        const val NAME = "name"
        const val IS_DEFAULT = "isDefault"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}

/**
 * RoleDTO를 Role 도메인 모델로 변환하는 확장 함수
 * @return Role 도메인 모델
 */
fun RoleDTO.toDomain(): Role {
    // Firestore에서 읽은 타임스탬프가 null일 경우를 대비하여 현재 시간으로 대체
    val now = Instant.now()
    return Role.fromDataSource(
        id = DocumentId(this.id),
        name = this.name,
        isDefault = this.isDefault,
        createdAt = this.createdAt?.toDate()?.toInstant() ?: now,
        updatedAt = this.updatedAt?.toDate()?.toInstant() ?: now
    )
}

/**
 * Role 도메인 모델을 DTO로 변환하는 확장 함수
 * @return RoleDTO 객체
 */
fun Role.toDto(): RoleDTO {
    return RoleDTO(
        id = this.id.value,
        name = this.name,
        isDefault = this.isDefault,
        // Timestamps are handled by the server, so they are null when creating/updating from the client.
        createdAt = null,
        updatedAt = null
    )
}
