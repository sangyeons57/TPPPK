package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId

/**
 * Represents a role within a project with specific permissions.
 *
 * @property id The unique identifier for the role.
 * @property projectId The ID of the project this role belongs to.
 * @property name The display name of the role.
 * @property permissions A map of permission keys to their enabled/disabled status.
 * @property isDefault Whether this is a default role (e.g., Admin, Member).
 * @property createdAt Timestamp when the role was created (in milliseconds since epoch).
 * @property updatedAt Timestamp when the role was last updated (in milliseconds since epoch).
 */
data class Role(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val name: String = "",
    val permissions: Map<String, Boolean> = emptyMap(),
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this role has a specific permission.
     *
     * @param permission The permission key to check.
     * @return `true` if the permission is granted, `false` otherwise.
     */
    fun hasPermission(permission: String): Boolean = permissions[permission] ?: false

    /**
     * Creates a copy of this role with updated fields.
     *
     * @param name New name for the role.
     * @param permissions New permissions map.
     * @param isDefault New default status.
     * @return A new [Role] instance with the updated fields.
     */
    fun copyWith(
        name: String = this.name,
        permissions: Map<String, Boolean> = this.permissions,
        isDefault: Boolean = this.isDefault
    ): Role = copy(
        name = name,
        permissions = permissions,
        isDefault = isDefault,
        updatedAt = System.currentTimeMillis()
    )
}
