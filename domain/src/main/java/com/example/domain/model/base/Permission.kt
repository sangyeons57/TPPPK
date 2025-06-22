package com.example.domain.model.base


import com.example.domain.event.AggregateRoot
import com.example.domain.event.permission.PermissionCreatedEvent
import com.example.domain.event.permission.PermissionDescriptionUpdatedEvent
import com.example.domain.event.permission.PermissionNameUpdatedEvent
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.permission.PermissionDescription
import java.time.Instant

class Permission private constructor(
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean
) : AggregateRoot() {

    val createdAt: Instant = initialCreatedAt


    var updatedAt: Instant = initialUpdatedAt
        private set

    fun getPermissionRole() : RolePermission {
        return RolePermission.valueOf(this.id.value)
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            Permission.KEY_UPDATED_AT to this.updatedAt,
            Permission.KEY_CREATED_AT to this.createdAt,
        )
    }

    companion object {
        const val COLLECTION_NAME = "permissions" // Sub-collection of Role
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        /**
         * Factory method for creating a new permission.
         */
        fun create(id: RolePermission): Permission {
            val now = Instant.now()
            val permission = Permission(
                id = DocumentId.from(id),
                initialCreatedAt = now,
                initialUpdatedAt = now,
                isNew = true
            )
            return permission
        }

        /**
         * Factory method to reconstitute a Permission from a data source.
         */
        fun fromDataSource(
            id: RolePermission,
            createdAt: Instant,
            updatedAt: Instant
        ): Permission {
            return Permission(
                id = DocumentId.from(id),
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                isNew = false
            )
        }
    }
}

