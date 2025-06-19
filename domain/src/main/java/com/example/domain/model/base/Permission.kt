package com.example.domain.model.base


import com.example.domain.event.AggregateRoot
import com.example.domain.event.permission.PermissionCreatedEvent
import com.example.domain.event.permission.PermissionDescriptionUpdatedEvent
import com.example.domain.event.permission.PermissionNameUpdatedEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

class Permission private constructor(
    initialName: String,
    initialDescription: String,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean
) : AggregateRoot() {

    val createdAt: Instant = initialCreatedAt


    var name: String = initialName
        private set
    var description: String = initialDescription
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            Permission.KEY_NAME to this.name,
            Permission.KEY_DESCRIPTION to this.description,
            Permission.KEY_UPDATED_AT to this.updatedAt,
            Permission.KEY_CREATED_AT to this.createdAt,
        )
    }

    /**
     * Updates the name of the permission.
     */
    fun updateName(newName: String) {
        if (this.name == newName) return

        this.name = newName
        this.updatedAt = Instant.now()
        pushDomainEvent(PermissionNameUpdatedEvent(this.id, this.name, this.updatedAt))
    }

    /**
     * Updates the description of the permission.
     */
    fun updateDescription(newDescription: String) {
        if (this.description == newDescription) return

        this.description = newDescription
        this.updatedAt = Instant.now()
        pushDomainEvent(PermissionDescriptionUpdatedEvent(this.id, this.description, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "permissions" // Sub-collection of Role
        const val KEY_NAME = "name"
        const val KEY_DESCRIPTION = "description"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        /**
         * Factory method for creating a new permission.
         */
        fun create(id: DocumentId, name: String, description: String): Permission {
            val now = Instant.now()
            val permission = Permission(
                id = id,
                initialName = name,
                initialDescription = description,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                isNew = true
            )
            permission.pushDomainEvent(PermissionCreatedEvent(permission.id, permission.name, permission.description, now))
            return permission
        }

        /**
         * Factory method to reconstitute a Permission from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            name: String,
            description: String,
            createdAt: Instant,
            updatedAt: Instant
        ): Permission {
            return Permission(
                id = id,
                initialName = name,
                initialDescription = description,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                isNew = false
            )
        }
    }
}

