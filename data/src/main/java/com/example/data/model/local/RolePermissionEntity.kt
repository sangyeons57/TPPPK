package com.example.data.model.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.domain.model.RolePermission

/**
 * 역할에 대한 권한 정보를 저장하기 위한 Room 엔티티
 * 역할과 권한 간의 다대다 관계를 저장합니다.
 */
@Entity(
    tableName = "role_permissions",
    primaryKeys = ["roleId", "permission"],
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roleId")]
)
data class RolePermissionEntity(
    val roleId: String,
    val permission: String, // RolePermission enum의 name 값
    val isEnabled: Boolean
) {
    companion object {
        /**
         * 역할 ID와 권한 맵에서 RolePermissionEntity 목록 생성
         * 
         * @param roleId 역할 ID
         * @param permissions 권한 맵
         * @return RolePermissionEntity 목록
         */
        fun fromPermissionMap(roleId: String, permissions: Map<RolePermission, Boolean>): List<RolePermissionEntity> {
            return permissions.map { (permission, isEnabled) ->
                RolePermissionEntity(
                    roleId = roleId,
                    permission = permission.name,
                    isEnabled = isEnabled
                )
            }
        }

        /**
         * RolePermissionEntity 목록에서 권한 맵 생성
         * 
         * @param permissions RolePermissionEntity 목록
         * @return 권한 맵
         */
        fun toPermissionMap(permissions: List<RolePermissionEntity>): Map<RolePermission, Boolean> {
            return permissions.mapNotNull { entity ->
                try {
                    val permission = RolePermission.valueOf(entity.permission)
                    permission to entity.isEnabled
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toMap()
        }
    }
} 