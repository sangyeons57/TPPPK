package com.example.domain_usecase.usecase.project.member

import com.example.domain.model.data.project.RolePermission

/**
 * 권한 거부 메시지를 생성하는 UseCase
 * RolePermission을 UI에서 표시할 표준화된 거부 메시지로 매핑합니다.
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