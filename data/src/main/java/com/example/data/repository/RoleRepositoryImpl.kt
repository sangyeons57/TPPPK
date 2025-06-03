package com.example.data.repository

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.datasource.remote.PermissionRemoteDataSource // 권한 목록 조회 시 필요할 수 있음
import com.example.data.model.remote.RoleDTO
import com.example.domain.model.base.Role
import com.example.domain.repository.RoleRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class RoleRepositoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    private val permissionRemoteDataSource: PermissionRemoteDataSource // 사용 가능한 권한 목록 조회용
    // private val roleMapper: RoleMapper // 개별 매퍼 사용시
) : RoleRepository {

    /**
     * 프로젝트의 역할 목록을 스트림으로 가져옵니다.
     * Firebase 캐싱을 활용하여 실시간으로 업데이트된 역할 목록을 제공합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 역할 목록 스트림
     */
    override fun getProjectRolesStream(projectId: String): Flow<CustomResult<List<Role>, Exception>> {
        return roleRemoteDataSource.observeRoles(projectId).map { roleList ->
            when (roleList) {
                is CustomResult.Success -> {
                    try {
                        // DTO를 도메인 모델로 변환
                        val roles = roleList.data.map { it.toDomain() }
                        CustomResult.Success(roles)
                    } catch (e: Exception) {
                        // 변환 중 오류 처리
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(roleList.error)
                else -> CustomResult.Failure(Exception("Unknown error getting roles"))
            }
        }
    }

    /**
     * 새 역할을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param name 역할 이름
     * @param isDefault 기본 역할 여부
     * @return 생성된 역할 ID를 포함한 CustomResult
     */
    override suspend fun createRole(
        projectId: String,
        name: String,
        isDefault: Boolean,
    ): CustomResult<String, Exception> {
        val roleDto = RoleDTO(
            // id는 Firestore에서 자동 생성
            name = name,
            isDefault = isDefault,
            // 추가 필드는 실제 구현에서 필요에 따라 추가
        )
        
        // 역할 추가 후 결과 반환
        return roleRemoteDataSource.addRole(projectId, roleDto)
    }

    /**
     * 역할 정보를 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @param name 새 역할 이름 (선택적)
     * @param isDefault 기본 역할 여부 (선택적)
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        isDefault: Boolean?,
    ): CustomResult<Unit, Exception> = resultTry {
        // 업데이트할 필드만 맵으로 구성
        val updates = mutableMapOf<String, Any?>() // Value type changed to Any?
        
        // Add to map if parameter is not null.
        // If parameter is null, it means "no change" for that field.
        // The remote data source will filter out these nulls before sending to Firestore.
        name?.let { updates[FirestoreConstants.Project.Roles.NAME] = it }
        isDefault?.let { updates[FirestoreConstants.Project.Roles.IS_DEFAULT] = it }
        
        // 업데이트할 내용이 있는 경우에만 데이터소스 호출
        if (updates.isNotEmpty()) {
            // Pass the updates map directly
            roleRemoteDataSource.updateRole(projectId, roleId, updates)
        }
        // If updates is empty, resultTry will return CustomResult.Success(Unit) implicitly.
    }

    /**
     * 프로젝트에서 역할을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 삭제할 역할 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun deleteRole(projectId: String, roleId: String): CustomResult<Unit, Exception> = resultTry {
        // 삭제 시 해당 역할을 가진 멤버들의 역할 처리 방안은 UseCase 레벨에서 처리
        roleRemoteDataSource.deleteRole(projectId, roleId)
    }

    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @return 역할 상세 정보
     */
    override suspend fun getRoleDetails(projectId: String, roleId: String): CustomResult<Role, Exception> = resultTry {
        // 역할 상세 정보를 스트림으로 가져와서 처음 발행된 값을 반환
        roleRemoteDataSource.observeRole(projectId, roleId).map { result ->
            when (result) {
                is CustomResult.Success -> result.data.toDomain()
                is CustomResult.Failure -> throw result.error
                else -> throw Exception("Unknown error getting role details")
            }
        }.first()
    }

    override suspend fun getRolePermissions(projectId: String, roleId: String): CustomResult<List<com.example.domain.model.data.project.RolePermission>, Exception> {
        return resultTry {
            when (val result = roleRemoteDataSource.getRolePermissionNames(projectId, roleId)) { // Needs to be in RoleRemoteDataSource
                is CustomResult.Success -> {
                    result.data.mapNotNull { name ->
                        try {
                            com.example.domain.model.data.project.RolePermission.valueOf(name)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }
                }
                is CustomResult.Failure -> throw result.error // re-throw
                else -> {
                    throw Exception("Unkown error")
                }
            }
        }
    }

    override suspend fun setRolePermissions(projectId: String, roleId: String, permissions: List<com.example.domain.model.data.project.RolePermission>): CustomResult<Unit, Exception> = resultTry {
        val permissionNames = permissions.map { it.name }
        roleRemoteDataSource.setRolePermissions(projectId, roleId, permissionNames) // Assumes this method in RoleRemoteDataSource
    }
}
