package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    /**
     * 시스템에서 사용 가능한 모든 권한 목록을 가져옵니다.
     * (주로 역할 정의 시 사용될 수 있으며, 데이터 변경 빈도가 낮을 수 있음)
     */
    fun getAllPermissionsStream(): Flow<CustomResult<List<Permission>, Unit>>

    /**
     * 특정 ID의 권한 정보를 가져옵니다.
     */
    suspend fun getPermissionById(permissionId: String): CustomResult<Permission, Unit>

    // 참고: 일반적으로 Permission 자체에 대한 CUD(Create, Update, Delete)는 사용자 레벨이 아닌
    // 시스템 관리자 레벨에서 이루어지거나, 미리 정의된 값을 사용합니다.
    // 역할(Role)에 권한을 할당/해제하는 기능은 RoleRepository에서 관리합니다.
}
