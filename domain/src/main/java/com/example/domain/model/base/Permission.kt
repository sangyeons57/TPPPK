package com.example.domain.model.base


import com.example.domain.model.AggregateRoot
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

class Permission private constructor(
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    init {
        setOriginalState()
    }

    fun getPermissionRole() : RolePermission {
        return RolePermission.valueOf(this.id.value)
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_UPDATED_AT to this.updatedAt,
            KEY_CREATED_AT to this.createdAt,
        )
    }

    companion object {
        const val COLLECTION_NAME = "permissions" // Sub-collection of Role
        /**
         * Factory method for creating a new permission.
         */
        fun create(id: RolePermission): Permission {
            val permission = Permission(
                id = DocumentId.from(id),
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true
            )
            return permission
        }

        /**
         * Factory method to reconstitute a Permission from a data source.
         */
        fun fromDataSource(
            id: RolePermission,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Permission {
            return Permission(
                id = DocumentId.from(id),
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false
            )
        }
    }
}

