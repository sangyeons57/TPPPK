package com.example.data.model.remote

import com.example.domain.model.AggregateRoot
import com.example.domain.model.DTO
import com.example.domain.model.base.Role
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
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
}
