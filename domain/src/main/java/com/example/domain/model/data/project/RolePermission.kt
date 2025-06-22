package com.example.domain.model.data.project

import com.example.domain.model.vo.DocumentId

/**
 * Enum representing the available permissions that can be assigned to roles.
 * These permissions control what actions users with a specific role can perform.
 */
enum class RolePermission {
    // Project permissions
    MANAGE_PROJECT,
    EDIT_PROJECT_DETAILS,
    DELETE_PROJECT,
    
    // Member management permissions
    MANAGE_MEMBERS,
    INVITE_MEMBERS,
    REMOVE_MEMBERS,
    EDIT_MEMBER_ROLES,
    
    // Role management permissions
    MANAGE_ROLES,
    CREATE_ROLES,
    EDIT_ROLES,
    DELETE_ROLES,
    
    // Channel permissions
    MANAGE_CHANNELS,
    CREATE_CHANNELS,
    EDIT_CHANNELS,
    DELETE_CHANNELS,
    
    // Category permissions
    MANAGE_CATEGORIES,
    CREATE_CATEGORIES,
    EDIT_CATEGORIES,
    DELETE_CATEGORIES,
    
    // Message permissions
    SEND_MESSAGES,
    MANAGE_MESSAGES,
    DELETE_MESSAGES,
    
    // File permissions
    UPLOAD_FILES,
    DOWNLOAD_FILES,
    DELETE_FILES,
    
    // Meeting permissions
    SCHEDULE_MEETINGS,
    MANAGE_MEETINGS,
    
    // Task permissions
    CREATE_TASKS,
    ASSIGN_TASKS,
    MANAGE_TASKS,
    
    // Admin permissions (full access)
    ADMINISTRATOR;
    
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
        fun allPermissions(value: Boolean = true): Map<String, Boolean> {
            return entries.associate { it.name to value }
        }
        
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
        fun defaultPermissions(isAdmin: Boolean = false): Map<String, Boolean> {
            return if (isAdmin) {
                allPermissions(true)
            } else {
                // Default non-admin permissions
                val permissions = mutableMapOf<String, Boolean>()
                
                // Basic permissions for all non-admin roles
                permissions[SEND_MESSAGES.name] = true
                permissions[UPLOAD_FILES.name] = true
                permissions[DOWNLOAD_FILES.name] = true
                permissions[CREATE_TASKS.name] = true
                
                permissions
            }
        }
    }
}
