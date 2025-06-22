package com.example.domain.model.base

import com.example.domain.model.data.project.RolePermission // Added import

import com.example.domain.event.AggregateRoot
import com.example.domain.event.role.RoleCreatedEvent
import com.example.domain.event.role.RoleDefaultStatusChangedEvent
import com.example.domain.event.role.RoleNameChangedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import java.time.Instant

class Role private constructor(
    // Constructor parameters to initialize the state
    initialName: Name,
    initialIsDefault: RoleIsDefault,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean,
) : AggregateRoot() {

    // Immutable properties
    val createdAt: Instant = initialCreatedAt

    // Mutable properties with private setters
    var name: Name = initialName
        private set
    var isDefault: RoleIsDefault = initialIsDefault
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    // Implementation of abstract method from AggregateRoot
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_NAME to this.name,
            KEY_IS_DEFAULT to this.isDefault,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_CREATED_AT to this.createdAt,
        )
    }

    /**
     * Changes the name of the role, firing a domain event if the name is actually different.
     */
    fun changeName(newName: Name) {
        if (this.name == newName) return // No change, no event

        val oldName = this.name
        this.name = newName
        this.updatedAt = Instant.now()
        pushDomainEvent(RoleNameChangedEvent(this.id, oldName, newName, this.updatedAt))
    }

    /**
     * Changes the default status of the role, firing a domain event if the status changes.
     */
    fun setDefault(newDefaultStatus: RoleIsDefault) {
        if (this.isDefault == newDefaultStatus) return // No change, no event

        this.isDefault = newDefaultStatus
        this.updatedAt = Instant.now()
        pushDomainEvent(RoleDefaultStatusChangedEvent(this.id, newDefaultStatus, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "roles"
        const val KEY_NAME = "name"
        const val KEY_IS_DEFAULT = "isDefault"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        /**
         * Factory method to create a new Role.
         * This method encapsulates the creation logic and fires a domain event.
         */
        fun create(
            name: Name,
            isDefault: RoleIsDefault
        ): Role {
            val now = Instant.now()
            val role = Role(
                id = DocumentId.EMPTY,
                initialName = name,
                initialIsDefault = isDefault,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                isNew = true
            )
            role.pushDomainEvent(RoleCreatedEvent(role.id, role.name, role.isDefault, now))
            return role
        }

        /**
         * Factory method to reconstitute a Role from a data source.
         * This method does not fire any domain events as it represents an existing entity.
         */
        fun fromDataSource(
            id: DocumentId,
            name: Name,
            isDefault: RoleIsDefault,
            createdAt: Instant,
            updatedAt: Instant
        ): Role {
            return Role(
                id = id,
                initialName = name,
                initialIsDefault = isDefault,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                isNew = false
            )
        }
    }
}
