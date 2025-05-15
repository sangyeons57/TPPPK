package com.example.domain.usecase.permission

import com.example.domain.model.RolePermission
import kotlin.Result

/**
 * 사용자가 특정 채널에서 특정 권한을 가지고 있는지 확인하는 유스케이스 인터페이스입니다.
 * 
 * 이 유스케이스는 사용자의 역할 기반 권한과 채널별 권한 재정의를 모두 고려하여 최종 권한을 결정합니다.
 */
interface CheckChannelPermissionUseCase {

    /**
     * 지정된 사용자가 특정 채널에서 요구되는 권한을 가지고 있는지 비동기적으로 확인합니다.
     *
     * @param userId 권한을 확인할 사용자의 ID.
     * @param channelId 권한을 확인할 채널의 ID.
     * @param permission 확인할 [RolePermission].
     * @return 성공 시 사용자의 권한 보유 여부(Boolean)를 담은 [Result]. 실패 시 에러 정보를 포함한 [Result].
     */
    suspend operator fun invoke(userId: String, channelId: String, permission: RolePermission): Result<Boolean>
} 