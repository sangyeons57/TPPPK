package com.example.data.repository

import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.datasource.remote.PermissionRemoteDataSource // 권한 목록 조회 시 필요할 수 있음
import com.example.data.model._remote.RoleDTO
import com.example.data.model.mapper.toDomain // RoleDTO -> Role
import com.example.domain.model.Role
import com.example.domain.repository.RoleRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class RoleRepositoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    private val permissionRemoteDataSource: PermissionRemoteDataSource // 사용 가능한 권한 목록 조회용
    // private val roleMapper: RoleMapper // 개별 매퍼 사용시
) : RoleRepository {

    override fun getProjectRolesStream(projectId: String): Flow<Result<List<Role>>> {
        // RoleRemoteDataSource에 getProjectRolesStream(projectId) 함수 필요
        return roleRemoteDataSource.getRolesStream(projectId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun createRole(
        projectId: String,
        name: String,
        permissions: List<String>,
        color: String?,
        isDefault: Boolean,
        currentUserId: String // DataSource에서 권한 확인용
    ): Result<Role> = resultTry {
        val roleDto = RoleDTO(
            // id는 Firestore에서 자동 생성
            projectId = projectId,
            name = name,
            permissions = permissions,
            color = color,
            isDefault = isDefault,
            // priority 등은 DataSource에서 설정 가능
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        // RoleRemoteDataSource의 createRole 함수는 생성된 RoleDTO (ID 포함)를 반환하거나, 생성된 Role의 ID를 반환할 수 있음.
        // 여기서는 DataSource가 생성된 DTO를 반환하고, 그것을 도메인 모델로 변환한다고 가정
        roleRemoteDataSource.createRole(roleDto, currentUserId).getOrThrow().toDomain()
    }

    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        permissions: List<String>?,
        color: String?,
        isDefault: Boolean?,
        currentUserId: String // DataSource에서 권한 확인용
    ): Result<Unit> = resultTry {
        // RoleRemoteDataSource에 부분 업데이트 또는 전체 DTO 업데이트 함수 필요
        // Firestore에서는 Map을 사용하여 특정 필드만 업데이트 가능
        // 여기서는 업데이트할 필드만 모아 Map으로 전달하거나, DataSource에서 DTO를 받아 처리한다고 가정.
        // 예시: DataSource에 updateRole(roleId, updates: Map<String, Any?>) 함수가 있다고 가정
        val updates = mutableMapOf<String, Any?>()
        name?.let { updates[\
name\] = it }
        permissions?.let { updates[\permissions\] = it }
        color?.let { updates[\color\] = it } // null로 색상 제거를 표현하려면 다른 방식 필요
        isDefault?.let { updates[\isDefault\] = it }
        if (updates.isNotEmpty()) {
            updates[\updatedAt\] = Timestamp.now()
            roleRemoteDataSource.updateRole(projectId, roleId, updates, currentUserId).getOrThrow()
        }
    }

    override suspend fun deleteRole(projectId: String, roleId: String, currentUserId: String): Result<Unit> = resultTry {
        // RoleRemoteDataSource에 deleteRole(projectId, roleId, currentUserId) 함수 필요
        // 삭제 시 해당 역할을 가진 멤버들의 역할 처리 방안은 UseCase 레벨에서 고민 필요
        roleRemoteDataSource.deleteRole(projectId, roleId, currentUserId).getOrThrow()
    }

    override suspend fun getRoleDetails(projectId: String, roleId: String): Result<Role> = resultTry {
        // RoleRemoteDataSource에 getRole(projectId, roleId) 함수 필요
        roleRemoteDataSource.getRole(projectId, roleId).getOrThrow().toDomain()
    }

    override suspend fun getAvailablePermissions(): Result<List<String>> = resultTry {
        // PermissionRemoteDataSource 또는 로컬 상수/설정에서 가져옴
        // 여기서는 PermissionRemoteDataSource에 getAvailablePermissions() 함수가 있다고 가정
        permissionRemoteDataSource.getAvailablePermissions().getOrThrow()
        // 또는 고정된 권한 목록 반환:
        // Result.success(listOf(\MANAGE_MEMBERS\, \EDIT_PROJECT_SETTINGS\, \CREATE_CHANNELS\, ...))
    }
}
