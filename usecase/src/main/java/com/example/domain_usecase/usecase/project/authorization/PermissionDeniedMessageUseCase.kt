package com.example.domain_usecase.usecase.project.authorization

import com.example.domain.model.data.project.RolePermission

/**
 * Maps RolePermission to standardized denial messages for UI snackbars.
 */
interface PermissionDeniedMessageUseCase {
    operator fun invoke(required: RolePermission): String
}

class PermissionDeniedMessageUseCaseImpl : PermissionDeniedMessageUseCase {
    override fun invoke(required: RolePermission): String = when (required) {
        RolePermission.ROLE_EDIT -> "역할을 수정할 수 없습니다 (ROLE_EDIT)"
        RolePermission.MEMBER_INVITE -> "멤버 초대 권한이 없습니다 (MEMBER_INVITE)"
        RolePermission.MEMBER_MANAGE -> "멤버 관리 권한이 없습니다 (MEMBER_MANAGE)"
        RolePermission.STRUCTURE_EDIT -> "구조 편집 권한이 없습니다 (STRUCTURE_EDIT)"
        RolePermission.PROJECT_SETTINGS -> "프로젝트 설정 권한이 없습니다 (PROJECT_SETTINGS)"
        RolePermission.CHANNEL_WRITE -> "메시지 전송 권한이 없습니다 (CHANNEL_WRITE)"
        RolePermission.CHANNEL_READ -> "채널 열람 권한이 없습니다 (CHANNEL_READ)"
    }
}

