package com.example.domain.model.data.project

import com.example.domain.vo.DocumentId

/**
 * Enum representing the available permissions that can be assigned to roles.
 * These permissions control what actions users with a specific role can perform.
 */
enum class RolePermission {
    // 1) 역할 수정 권한
    ROLE_EDIT,

    // 2) 멤버 초대 권한
    MEMBER_INVITE,

    // 3) 멤버 관리 권한
    MEMBER_MANAGE,

    // 4) 구조 편집 권한
    STRUCTURE_EDIT,

    // 5) 프로젝트 설정 권한
    PROJECT_SETTINGS,

    // 6) 채널 쓰기 권한
    CHANNEL_WRITE,

    // 7) 채널 읽기 권한
    CHANNEL_READ;
    
    companion object {
        fun from (value: String): RolePermission {
            return entries.find { it.name == value } ?: throw IllegalArgumentException("Invalid RolePermission value: $value")
        }
        fun from (value: DocumentId) : RolePermission {
            return entries.find { it.name == value.value } ?: throw IllegalArgumentException("Invalid RolePermission value: $value")
        }
        /**
         * Returns a map with all permissions set to the specified value.
         *
         * @param value The value to set for all permissions.
         * @return A map of all permissions to the specified value.
         */
        fun allPermissions(value: Boolean = true): Map<String, Boolean> =
            entries.associate { it.name to value }
        
        /**
         * Returns a map with all permissions set to false.
         *
         * @return A map of all permissions set to false.
         */
        fun noPermissions(): Map<String, Boolean> = allPermissions(false)
        
        /**
         * Returns the default permissions for a role.
         *
         * @param isAdmin Whether the role is an admin role.
         * @return A map of permissions with appropriate defaults.
         */
        fun defaultPermissions(isAdmin: Boolean = false): Map<String, Boolean> =
            if (isAdmin) allPermissions(true) else mapOf(
                CHANNEL_READ.name to true,
                CHANNEL_WRITE.name to true
            )
    }
}
