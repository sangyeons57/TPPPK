package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.project.RolePermission
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext

/**
 * 프로젝트 내 역할(Role) 및 권한(Permission) 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRoleRepository : DefaultRepository {
    override val factoryContext: ProjectRoleRepositoryFactoryContext

    /**
     * Fetches the list of enabled permissions for a specific role.
     * Permissions are typically stored as a subcollection of the role.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role.
     * @return A [CustomResult] containing a list of enabled [RolePermission] enums on success, or an [Exception] on failure.
     */
    suspend fun getRolePermissions(projectId: String, roleId: String): CustomResult<List<RolePermission>, Exception>

}
