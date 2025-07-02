package com.example.domain.model.vo.permission

import com.example.domain.model.vo.DocumentId

/**
 * Enum representing the available permissions that can be assigned to roles.
 * These permissions control what actions users with a specific role can perform.
 */
enum class PermissionType(
    val displayName: String,
    val description: String,
    val category: PermissionCategory
) {
    // Project permissions
    MANAGE_PROJECT("프로젝트 관리", "프로젝트 전체 설정 관리", PermissionCategory.PROJECT),
    EDIT_PROJECT_DETAILS("프로젝트 정보 수정", "프로젝트 이름, 설명 등 수정", PermissionCategory.PROJECT),
    DELETE_PROJECT("프로젝트 삭제", "프로젝트 완전 삭제", PermissionCategory.PROJECT),
    
    // Member management permissions
    MANAGE_MEMBERS("멤버 관리", "멤버 전체 관리", PermissionCategory.MEMBER),
    INVITE_MEMBERS("멤버 초대", "새 멤버 초대", PermissionCategory.MEMBER),
    REMOVE_MEMBERS("멤버 제거", "멤버 내보내기", PermissionCategory.MEMBER),
    EDIT_MEMBER_ROLES("멤버 역할 편집", "멤버의 역할 변경", PermissionCategory.MEMBER),
    
    // Role management permissions
    MANAGE_ROLES("역할 관리", "역할 전체 관리", PermissionCategory.ROLE),
    CREATE_ROLES("역할 생성", "새 역할 생성", PermissionCategory.ROLE),
    EDIT_ROLES("역할 편집", "기존 역할 수정", PermissionCategory.ROLE),
    DELETE_ROLES("역할 삭제", "역할 삭제", PermissionCategory.ROLE),
    
    // Channel permissions
    MANAGE_CHANNELS("채널 관리", "채널 전체 관리", PermissionCategory.CHANNEL),
    CREATE_CHANNELS("채널 생성", "새 채널 생성", PermissionCategory.CHANNEL),
    EDIT_CHANNELS("채널 편집", "채널 설정 수정", PermissionCategory.CHANNEL),
    DELETE_CHANNELS("채널 삭제", "채널 삭제", PermissionCategory.CHANNEL),
    
    // Category permissions
    MANAGE_CATEGORIES("카테고리 관리", "카테고리 전체 관리", PermissionCategory.CHANNEL),
    CREATE_CATEGORIES("카테고리 생성", "새 카테고리 생성", PermissionCategory.CHANNEL),
    EDIT_CATEGORIES("카테고리 편집", "카테고리 설정 수정", PermissionCategory.CHANNEL),
    DELETE_CATEGORIES("카테고리 삭제", "카테고리 삭제", PermissionCategory.CHANNEL),
    
    // Message permissions
    SEND_MESSAGES("메시지 전송", "채널에 메시지 전송", PermissionCategory.MESSAGE),
    MANAGE_MESSAGES("메시지 관리", "메시지 관리 권한", PermissionCategory.MESSAGE),
    DELETE_MESSAGES("메시지 삭제", "다른 사용자 메시지 삭제", PermissionCategory.MESSAGE),
    
    // File permissions
    UPLOAD_FILES("파일 업로드", "파일 및 이미지 업로드", PermissionCategory.FILE),
    DOWNLOAD_FILES("파일 다운로드", "파일 다운로드", PermissionCategory.FILE),
    DELETE_FILES("파일 삭제", "업로드된 파일 삭제", PermissionCategory.FILE),
    
    // Meeting permissions
    SCHEDULE_MEETINGS("회의 예약", "회의 일정 생성", PermissionCategory.MEETING),
    MANAGE_MEETINGS("회의 관리", "회의 관리 권한", PermissionCategory.MEETING),
    
    // Task permissions
    CREATE_TASKS("작업 생성", "새 작업 생성", PermissionCategory.TASK),
    ASSIGN_TASKS("작업 할당", "작업을 멤버에게 할당", PermissionCategory.TASK),
    MANAGE_TASKS("작업 관리", "작업 관리 권한", PermissionCategory.TASK),
    
    // Admin permissions (full access)
    ADMINISTRATOR("관리자", "모든 권한", PermissionCategory.ADMIN);
    
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
        fun allPermissions(value: Boolean = true): Map<PermissionType, Boolean> {
            return entries.associateWith { value }
        }
        
        /**
         * Returns a map with all permissions set to false.
         */
        fun noPermissions(): Map<PermissionType, Boolean> = allPermissions(false)
        
        /**
         * Returns the default permissions for a role.
         */
        fun defaultPermissions(isAdmin: Boolean = false): Map<PermissionType, Boolean> {
            return if (isAdmin) {
                allPermissions(true)
            } else {
                // Default non-admin permissions
                mapOf(
                    SEND_MESSAGES to true,
                    UPLOAD_FILES to true,
                    DOWNLOAD_FILES to true,
                    CREATE_TASKS to true
                ).withDefault { false }
            }
        }
        
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
    PROJECT("프로젝트"),
    MEMBER("멤버"),
    ROLE("역할"),
    CHANNEL("채널"),
    MESSAGE("메시지"),
    FILE("파일"),
    MEETING("회의"),
    TASK("작업"),
    ADMIN("관리자")
}