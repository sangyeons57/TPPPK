package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import com.example.domain.repository.DefaultRepository

interface PermissionRepository : DefaultRepository {

    // 참고: 일반적으로 Permission 자체에 대한 CUD(Create, Update, Delete)는 사용자 레벨이 아닌
    // 시스템 관리자 레벨에서 이루어지거나, 미리 정의된 값을 사용합니다.
    // 역할(Role)에 권한을 할당/해제하는 기능은 RoleRepository에서 관리합니다.


    suspend fun getAllPermissions(projectId: String): CustomResult<List<Permission>, Exception>
}
