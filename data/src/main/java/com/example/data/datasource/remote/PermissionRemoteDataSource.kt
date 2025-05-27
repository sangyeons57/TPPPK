
package com.example.data.datasource.remote

import com.example.data.model._remote.PermissionDTO
import kotlinx.coroutines.flow.Flow

interface PermissionRemoteDataSource {

    /**
     * 특정 역할이 가진 모든 권한 목록을 실시간으로 관찰합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param roleId 권한 목록을 가져올 역할의 ID
     */
    fun observePermissions(projectId: String, roleId: String): Flow<List<PermissionDTO>>

    /**
     * 특정 역할에 권한을 추가합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param roleId 권한을 추가할 역할의 ID
     * @param permission 추가할 권한 정보 DTO
     */
    suspend fun addPermissionToRole(projectId: String, roleId: String, permission: PermissionDTO): Result<Unit>

    /**
     * 특정 역할에서 권한을 제거합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param roleId 권한을 제거할 역할의 ID
     * @param permissionId 제거할 권한의 ID (보통 권한 이름)
     */
    suspend fun removePermissionFromRole(projectId: String, roleId: String, permissionId: String): Result<Unit>
}

