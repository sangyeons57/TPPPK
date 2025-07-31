package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain_repository.DefaultRepository

/**
 * 프로젝트 내 역할(Role) 및 권한(Permission) 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRoleRepository : DefaultRepository<Role> {


    suspend fun getRolePermissions(projectId: String, roleId: String): CustomResult<List<RolePermission>, Exception>

}
