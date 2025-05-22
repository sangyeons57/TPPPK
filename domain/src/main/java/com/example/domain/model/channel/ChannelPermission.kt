package com.example.domain.model.channel

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import com.google.firebase.Timestamp // Firestore Timestamp import
import com.google.firebase.firestore.FieldValue
import java.lang.Exception

/**
 * 채널 권한을 나타내는 데이터 클래스입니다.
 * 프로젝트 역할 기반으로 권한을 확인합니다.
 */
data class ChannelPermission(
    /**
     * 대상 채널 ID입니다.
     */
    val channelId: String,

    /**
     * 사용자 ID입니다.
     */
    val userId: String,

    /**
     * 채널이 속한 프로젝트 ID입니다.
     * DM 채널 등 프로젝트에 속하지 않은 채널은 null 가능합니다.
     */
    val projectId: String?,

    /**
     * 사용자가 해당 프로젝트에서 가진 역할 ID 목록입니다.
     * 프로젝트에 속하지 않은 채널은 빈 리스트일 수 있습니다.
     */
    val roleIds: List<String>,

    /**
     * 채널별 권한 재정의입니다. null인 경우 역할의 기본 권한이 적용됩니다.
     * 특정 권한에 대해 역할과 관계없이 허용/거부를 설정할 수 있습니다.
     */
    val overridePermissions: Map<RolePermission, Boolean>? = null,

    /**
     * 레거시 호환을 위한 채널 역할입니다. 새 구현에서는 사용하지 않습니다.
     * @deprecated 역할 기반 권한으로 대체되었습니다. `roleIds`를 사용하세요.
     */
    @Deprecated("레거시 호환용. 새 코드에서 사용 금지")
    val role: ChannelRole? = null,

    /**
     * 레거시 호환용 커스텀 권한입니다. 새 구현에서는 사용하지 않습니다.
     * @deprecated `overridePermissions`로 대체되었습니다.
     */
    @Deprecated("레거시 호환용. 새 코드에서 사용 금지")
    val customPermissions: Map<ChannelPermissionType, Boolean>? = null
) {
    /**
     * 주어진 권한 유형에 대한 접근 가능 여부를 확인합니다.
     * 1. 채널별 권한 재정의가 있으면 해당 값 사용
     * 2. 없으면 사용자의 역할 기반 권한 확인
     * 3. 프로젝트 속성이 없는 채널(예: DM)은 기본 권한 정책 적용
     */
    suspend fun hasPermission(
        permission: RolePermission,
        roleRepository: ProjectRoleRepository // ProjectRoleRepository 주입
    ): Result<Boolean> {
        // 1. 채널별 권한 재정의 확인
        overridePermissions?.get(permission)?.let { 
            return Result.success(it) // Return override value wrapped in success
        }

        // 2. 프로젝트가 없는 채널(DM 등)은 기본 권한 정책 적용
        if (projectId == null) {
            val defaultPermissionValue = when (permission) {
                // DM 채널은 기본적으로 읽기/쓰기 허용 (파일, 언급 포함)
                RolePermission.READ_MESSAGES,
                RolePermission.SEND_MESSAGES,
                RolePermission.UPLOAD_FILES,
                RolePermission.MENTION_MEMBERS -> true
                // 다른 관리 권한은 기본적으로 거부
                else -> false
            }
            return Result.success(defaultPermissionValue) // Return default value wrapped in success
        }

        // 3. 역할 없으면 기본 거부
        if (roleIds.isEmpty()) return Result.success(false)

        // 4. 프로젝트 역할 기반 권한 확인 (Fetch all roles for the project)
        return try {
            val projectRolesResult = roleRepository.getRoles(projectId) // [ Modify ] Use getRoles
            if (projectRolesResult.isFailure) {
                // Failed to fetch roles, return failure
                return Result.failure(projectRolesResult.exceptionOrNull() ?: Exception("Failed to fetch project roles"))
            }
            
            val projectRoles = projectRolesResult.getOrThrow()
            
            // Filter roles the user actually has
            val userRoles = projectRoles.filter { it.id in roleIds }

            // Check if any of the user's roles grant the permission
            val hasPerm = userRoles.any { role ->
                role.permissions[permission] == true
            }
            Result.success(hasPerm)

        } catch (e: Exception) {
            // Catch any other unexpected errors during the process
            Result.failure(e)
        }
    }

    /**
     * 레거시 호환을 위한 메소드입니다.
     * @deprecated 새 구현의 `hasPermission(RolePermission, ProjectRoleRepository)`를 사용하세요.
     */
    @Deprecated("레거시 호환용. 새 메소드로 마이그레이션하세요.")
    fun hasPermission(permission: ChannelPermissionType): Boolean {
        // 레거시 메소드는 이전 방식대로 동작
        customPermissions?.get(permission)?.let { return it }

        return when (role) {
            ChannelRole.OWNER -> true
            ChannelRole.ADMIN -> permission != ChannelPermissionType.MANAGE_PERMISSIONS
            ChannelRole.MODERATOR -> when (permission) {
                ChannelPermissionType.READ_MESSAGES,
                ChannelPermissionType.SEND_MESSAGES,
                ChannelPermissionType.MANAGE_MESSAGES,
                ChannelPermissionType.MENTION_MEMBERS,
                ChannelPermissionType.UPLOAD_FILES,
                ChannelPermissionType.INVITE_MEMBERS -> true
                ChannelPermissionType.MANAGE_CHANNEL,
                ChannelPermissionType.MANAGE_PERMISSIONS -> false
            }
            ChannelRole.MEMBER -> when (permission) {
                ChannelPermissionType.READ_MESSAGES,
                ChannelPermissionType.SEND_MESSAGES,
                ChannelPermissionType.MENTION_MEMBERS,
                ChannelPermissionType.UPLOAD_FILES -> true
                ChannelPermissionType.MANAGE_MESSAGES,
                ChannelPermissionType.INVITE_MEMBERS,
                ChannelPermissionType.MANAGE_CHANNEL,
                ChannelPermissionType.MANAGE_PERMISSIONS -> false
            }
            ChannelRole.GUEST -> permission == ChannelPermissionType.READ_MESSAGES
            null -> false
        }
    }

    /**
     * ChannelPermission 객체를 Firestore에 저장하기 위한 Map으로 변환합니다.
     * 새로운 permissions_override 컬렉션용 변환 메소드입니다.
     */
    fun toFirestoreOverrideMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        if (overridePermissions != null && overridePermissions.isNotEmpty()) {
            val permMap = mutableMapOf<String, Boolean>()
            overridePermissions.forEach { (type, allowed) ->
                permMap[type.name] = allowed
            }
            map["permissions"] = permMap
        }

        // Firestore 저장을 위해 필드 추가
        map["userId"] = userId 
        map["updatedAt"] = FieldValue.serverTimestamp() // 서버 타임스탬프 사용

        return map
    }

    /**
     * 레거시 호환용 Firestore 변환 메소드입니다.
     * @deprecated 새 구현에서는 `toFirestoreOverrideMap()`을 사용하세요.
     */
    @Deprecated("레거시 호환용. 새 메소드로 마이그레이션하세요.")
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        // 레거시 필드 저장 (마이그레이션 기간 동안 필요 시)
        map["channelId"] = channelId 
        map["userId"] = userId
        role?.let { map["role"] = it.name }

        customPermissions?.let {
            val permMap = mutableMapOf<String, Boolean>()
            it.forEach { (type, allowed) -> permMap[type.name] = allowed }
            map["customPermissions"] = permMap
        }

        return map
    }

    /**
     * 프로젝트 역할 기반 채널 권한 생성 메소드
     */
    companion object {
        /**
         * DM 채널에 대한 기본 ChannelPermission 객체를 생성합니다.
         */
        fun createDefaultDmPermission(channelId: String, userId: String): ChannelPermission {
            return ChannelPermission(
                channelId = channelId,
                userId = userId,
                projectId = null, // DM은 프로젝트에 속하지 않음
                roleIds = emptyList(), // 역할 없음
                // DM 기본 권한 설정
                overridePermissions = mapOf(
                    RolePermission.READ_MESSAGES to true,
                    RolePermission.SEND_MESSAGES to true,
                    RolePermission.UPLOAD_FILES to true,
                    RolePermission.MENTION_MEMBERS to true
                )
            )
        }

        /**
         * 프로젝트 멤버의 기본 ChannelPermission 객체를 생성합니다. (역할 기반)
         */
        fun createDefaultProjectMemberPermission(
            channelId: String,
            userId: String,
            projectId: String,
            roleIds: List<String>
        ): ChannelPermission {
            return ChannelPermission(
                channelId = channelId,
                userId = userId,
                projectId = projectId,
                roleIds = roleIds,
                overridePermissions = null // 역할에서 권한 상속
            )
        }
    }
}

/**
 * 채널 내 역할을 나타내는 열거형입니다.
 * @deprecated 프로젝트 역할 기반 권한으로 대체되었습니다. RolePermission을 사용하세요.
 */
@Deprecated("프로젝트 역할 기반 권한으로 대체되었습니다.")
enum class ChannelRole {
    /**
     * 채널 소유자 (모든 권한)
     */
    OWNER,

    /**
     * 관리자 (대부분의 관리 권한)
     */
    ADMIN,

    /**
     * 중재자 (제한된 관리 권한)
     */
    MODERATOR,

    /**
     * 일반 멤버 (기본 참여 권한)
     */
    MEMBER,

    /**
     * 게스트 (읽기 전용 등 제한된 권한)
     */
    GUEST
}

/**
 * 채널 권한 유형을 나타내는 열거형입니다.
 * @deprecated RolePermission enum으로 통합되었습니다.
 */
@Deprecated("RolePermission으로 통합되었습니다.")
enum class ChannelPermissionType {
    /**
     * 메시지 읽기 권한
     */
    READ_MESSAGES,

    /**
     * 메시지 전송 권한
     */
    SEND_MESSAGES,

    /**
     * 메시지 관리(삭제, 수정) 권한
     */
    MANAGE_MESSAGES,

    /**
     * 멤버 언급(@) 권한
     */
    MENTION_MEMBERS,

    /**
     * 파일 업로드 권한
     */
    UPLOAD_FILES,

    /**
     * 멤버 초대 권한
     */
    INVITE_MEMBERS,

    /**
     * 채널 설정 관리 권한
     */
    MANAGE_CHANNEL,

    /**
     * 권한 관리 권한
     */
    MANAGE_PERMISSIONS
}

/**
 * ChannelPermissionType을 RolePermission으로 변환하는 확장 함수입니다.
 * 마이그레이션에 사용됩니다.
 */
@Deprecated("마이그레이션 목적으로만 사용하세요")
fun ChannelPermissionType.toRolePermission(): RolePermission {
    return when (this) {
        ChannelPermissionType.READ_MESSAGES -> RolePermission.READ_MESSAGES
        ChannelPermissionType.SEND_MESSAGES -> RolePermission.SEND_MESSAGES
        // MANAGE_MESSAGES는 DELETE_MESSAGES 또는 DELETE_OTHERS_MESSAGES 중 선택 필요
        // 여기서는 자신의 메시지 삭제 권한으로 매핑
        ChannelPermissionType.MANAGE_MESSAGES -> RolePermission.DELETE_MESSAGES 
        ChannelPermissionType.MENTION_MEMBERS -> RolePermission.MENTION_MEMBERS
        ChannelPermissionType.UPLOAD_FILES -> RolePermission.UPLOAD_FILES
        ChannelPermissionType.INVITE_MEMBERS -> RolePermission.INVITE_MEMBERS
        ChannelPermissionType.MANAGE_CHANNEL -> RolePermission.MANAGE_CHANNELS
        ChannelPermissionType.MANAGE_PERMISSIONS -> RolePermission.MANAGE_ROLES // 또는 ASSIGN_ROLES 고려
    }
} 