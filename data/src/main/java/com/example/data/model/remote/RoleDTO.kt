package com.example.data.model.remote

import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Role
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import java.util.Date

/**
 * 역할 정보를 나타내는 DTO 클래스
 */
data class RoleDTO(
    @DocumentId 
    override var id: String = "",
    @get:PropertyName(NAME)
    var name: String = "",
    @get:PropertyName(IS_DEFAULT)
    var isDefault: Boolean = false,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override var createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override var updatedAt: Date? = null
) : DTO {
    companion object {
        const val COLLECTION_NAME = Role.COLLECTION_NAME
        const val NAME = Role.KEY_NAME
        const val IS_DEFAULT = Role.KEY_IS_DEFAULT
    }

    override fun toDomain(): AggregateRoot {
        // Firestore에서 읽은 타임스탬프가 null일 경우를 대비하여 현재 시간으로 대체
        return Role.fromDataSource(
            id = VODocumentId(this.id),
            name = Name(this.name),
            isDefault = RoleIsDefault(this.isDefault),
            createdAt = this.createdAt?.toInstant(),
            updatedAt = this.updatedAt?.toInstant()
        )
    }
}

/**
 * Role 도메인 모델을 DTO로 변환하는 확장 함수
 * @return RoleDTO 객체
 */
fun Role.toDto(): RoleDTO {
    return RoleDTO(
        id = this.id.value,
        name = this.name.value,
        isDefault = this.isDefault.value,
        createdAt = Date.from(this.createdAt),
        updatedAt = Date.from(this.updatedAt)
    )
}
