package com.example.data.repository

import com.example.data.datasource.local.projectrole.ProjectRoleLocalDataSource
import com.example.data.datasource.remote.projectrole.ProjectRoleRemoteDataSource
import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.Result

/**
 * 프로젝트 역할 관련 리포지토리 구현
 * 로컬 및 원격 데이터 소스를 활용하여 역할 관련 기능을 제공합니다.
 */
class ProjectRoleRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectRoleRemoteDataSource,
    private val localDataSource: ProjectRoleLocalDataSource
) : ProjectRoleRepository {

    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록 또는 에러
     */
    override suspend fun getRoles(projectId: String): Result<List<Role>> {
        // 로컬 데이터 소스에서 역할 목록 가져오기
        val localRoles = localDataSource.getRoles(projectId)
        
        // 로컬에 데이터가 있으면 반환
        if (localRoles.isNotEmpty()) {
            return Result.success(localRoles)
        }
        
        // 로컬에 데이터가 없으면 원격에서 가져와서 로컬에 저장
        return try {
            val remoteRoles = remoteDataSource.getRoles(projectId)
            if (remoteRoles.isSuccess) {
                val roles = remoteRoles.getOrNull() ?: emptyList()
                if (roles.isNotEmpty()) {
                    localDataSource.saveRoles(roles)
                }
                Result.success(roles)
            } else {
                remoteRoles // 원격 요청이 실패한 경우 그대로 반환
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 특정 프로젝트의 역할 목록 실시간 스트림을 가져옵니다.
     * 서버에서 새로운 역할 목록이 동기화되면 로컬에도 저장합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록의 Flow
     */
    override fun getRolesStream(projectId: String): Flow<List<Role>> {
        // 원격 데이터 소스의 실시간 스트림을 받아 로컬에 캐싱하고 반환
        return remoteDataSource.getRolesStream(projectId).onEach { roles ->
            // 변경이 감지되면 로컬에 저장
            localDataSource.saveRoles(roles)
        }
    }

    /**
     * 특정 프로젝트의 역할 목록을 새로고침합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun fetchRoles(projectId: String): Result<Unit> {
        // 원격 데이터 소스에서 역할 목록 새로고침
        val result = remoteDataSource.fetchRoles(projectId)
        
        // 성공 시 최신 역할 목록 가져와서 로컬에 저장
        if (result.isSuccess) {
            val roles = remoteDataSource.getRoles(projectId).getOrNull() ?: emptyList()
            localDataSource.saveRoles(roles)
        }
        
        return result
    }

    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * 
     * @param roleId 역할 ID
     * @return 역할 이름과 권한 맵 Pair 또는 에러
     */
    override suspend fun getRoleDetails(roleId: String): Result<Pair<String, Map<RolePermission, Boolean>>> {
        // 로컬 데이터 소스에서 역할 상세 정보 가져오기
        val localRoleDetails = localDataSource.getRoleDetails(roleId)
        
        // 로컬에 데이터가 있으면 반환
        if (localRoleDetails != null) {
            return Result.success(localRoleDetails)
        }
        
        // 로컬에 데이터가 없으면 원격에서 가져오기
        return remoteDataSource.getRoleDetails(roleId)
    }

    /**
     * 새 역할을 생성합니다.
     * 
     * @param projectId 역할이 생성될 프로젝트 ID
     * @param name 새 역할 이름
     * @param permissions 새 역할의 권한 맵
     * @return 성공/실패 결과
     */
    override suspend fun createRole(
        projectId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>
    ): Result<Unit> {
        // 원격 데이터 소스에서 역할 생성
        val result = remoteDataSource.createRole(projectId, name, permissions)
        
        // 성공 시 최신 역할 목록 가져와서 로컬에 저장
        if (result.isSuccess) {
            val roleId = result.getOrNull()
            if (roleId != null) {
                // 새로운 역할을 생성했으므로 최신 목록 가져오기
                val roles = remoteDataSource.getRolesStream(projectId).firstOrNull() ?: emptyList()
                localDataSource.saveRoles(roles)
            }
            return Result.success(Unit)
        }
        
        return Result.failure(result.exceptionOrNull() ?: Exception("역할 생성 실패"))
    }

    /**
     * 기존 역할을 업데이트합니다.
     * 
     * @param roleId 수정할 역할 ID
     * @param name 새 역할 이름
     * @param permissions 새 권한 맵
     * @return 성공/실패 결과
     */
    override suspend fun updateRole(
        roleId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>
    ): Result<Unit> {
        // 원격 데이터 소스에서 역할 업데이트
        val result = remoteDataSource.updateRole(roleId, name, permissions)
        
        // 성공 시 로컬 데이터 업데이트
        if (result.isSuccess) {
            // 역할 ID에서 프로젝트 ID 추출 (이 구현은 역할 ID 형식에 따라 달라질 수 있음)
            val parts = roleId.split("_")
            if (parts.size >= 2) {
                val projectId = parts[0]
                // 프로젝트의 최신 역할 목록 가져오기
                val roles = remoteDataSource.getRolesStream(projectId).firstOrNull() ?: emptyList()
                localDataSource.saveRoles(roles)
            } else {
                // 역할 ID에서 프로젝트 ID를 추출할 수 없는 경우 
                // 직접 해당 역할의 정보를 포함하는 객체를 생성하여 업데이트
                // (권장되지 않는 방법이지만 폴백 처리로 제공)
                val projectId = "unknown" // 이 경우 프로젝트 ID를 알 수 없으므로 임시 값 사용
                val role = Role(
                    id = roleId,
                    projectId = projectId,
                    name = name,
                    permissions = permissions
                )
                localDataSource.updateRole(role)
            }
        }
        
        return result
    }

    /**
     * 역할을 삭제합니다.
     * 
     * @param roleId 삭제할 역할 ID
     * @return 성공/실패 결과
     */
    override suspend fun deleteRole(roleId: String): Result<Unit> {
        // 원격 데이터 소스에서 역할 삭제
        val result = remoteDataSource.deleteRole(roleId)
        
        // 성공 시 로컬 데이터에서도 삭제
        if (result.isSuccess) {
            localDataSource.deleteRole(roleId)
        }
        
        return result
    }
}