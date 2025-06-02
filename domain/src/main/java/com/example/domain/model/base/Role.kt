package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId
import com.example.domain.model.data.project.RolePermission // Added import

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
    val name: String = "",
    val isDefault: Boolean = false // 기본 역할(Admin, Member 등)인지 커스텀 역할인지 구분
) {
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
        isDefault: Boolean = this.isDefault
    ): Role = copy(
        name = name,
        isDefault = isDefault,
    )
}
