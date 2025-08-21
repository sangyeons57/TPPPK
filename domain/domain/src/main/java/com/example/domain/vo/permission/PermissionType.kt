package com.example.domain.vo.permission

import com.example.domain.vo.DocumentId

/**
 * Enum representing the available permissions that can be assigned to roles.
 * These permissions control what actions users with a specific role can perform.
 */
enum class PermissionType(
    val displayName: String,
    val description: String,
    val category: PermissionCategory
) {
    // 1) 역할 수정 권한
    ROLE_EDIT(
        displayName = "역할 수정",
        description = "역할에 할당된 권한 수정",
        category = PermissionCategory.ROLE
    ),

    // 2) 멤버 초대 권한
    MEMBER_INVITE(
        displayName = "멤버 초대",
        description = "새 멤버 초대",
        category = PermissionCategory.MEMBER
    ),

    // 3) 멤버 관리 권한
    MEMBER_MANAGE(
        displayName = "멤버 관리",
        description = "내보내기, 밴, 멤버에게 역할 할당",
        category = PermissionCategory.MEMBER
    ),

    // 4) 구조 편집 권한
    STRUCTURE_EDIT(
        displayName = "구조 편집",
        description = "채널/카테고리 순서 및 이름 수정",
        category = PermissionCategory.STRUCTURE
    ),

    // 5) 프로젝트 설정 권한
    PROJECT_SETTINGS(
        displayName = "프로젝트 설정",
        description = "프로젝트 이름, 프로필 이미지 변경",
        category = PermissionCategory.PROJECT
    ),

    // 6) 채널 쓰기 권한
    CHANNEL_WRITE(
        displayName = "채널 쓰기",
        description = "채팅 및 테스크/할일 작성·수정",
        category = PermissionCategory.CHANNEL
    ),

    // 7) 채널 읽기 권한
    CHANNEL_READ(
        displayName = "채널 읽기",
        description = "채널 및 메시지 읽기",
        category = PermissionCategory.CHANNEL
    );
    
    companion object {
        fun from(value: String): PermissionType {
            return entries.find { it.name == value } 
                ?: throw IllegalArgumentException("Invalid PermissionType value: $value")
        }
        
        fun from(value: DocumentId): PermissionType {
            return entries.find { it.name == value.value } 
                ?: throw IllegalArgumentException("Invalid PermissionType value: $value")
        }
        
        /**
         * Returns a map with all permissions set to the specified value.
         */
        fun allPermissions(value: Boolean = true): Map<PermissionType, Boolean> =
            entries.associateWith { value }
        
        /**
         * Returns a map with all permissions set to false.
         */
        fun noPermissions(): Map<PermissionType, Boolean> = allPermissions(false)
        
        /**
         * Returns the default permissions for a role.
         */
        fun defaultPermissions(isAdmin: Boolean = false): Map<PermissionType, Boolean> =
            if (isAdmin) allPermissions(true) else mapOf(
                CHANNEL_READ to true,
                CHANNEL_WRITE to true
            ).withDefault { false }
        
        /**
         * Get permissions grouped by category for better UI organization
         */
        fun getPermissionsByCategory(): Map<PermissionCategory, List<PermissionType>> {
            return entries.groupBy { it.category }
        }
    }
}

/**
 * Categories for organizing permissions in the UI
 */
enum class PermissionCategory(val displayName: String) {
    ROLE("역할"),
    MEMBER("멤버"),
    STRUCTURE("구조"),
    PROJECT("프로젝트"),
    CHANNEL("채널")
}
