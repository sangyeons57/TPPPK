package com.example.data.repository

// import com.example.data.datasource.local.projectrole.ProjectRoleLocalDataSource // 제거
import com.example.data.datasource.remote.projectrole.ProjectRoleRemoteDataSource
import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import kotlinx.coroutines.flow.Flow
// import kotlinx.coroutines.flow.firstOrNull // 필요 시 유지, 현재는 createRole에서만 사용
// import kotlinx.coroutines.flow.onEach // 제거
import javax.inject.Inject
import kotlin.Result

/**
 * 프로젝트 역할 관련 리포지토리 구현
 * 원격 데이터 소스를 활용하고 Firestore 캐시를 통해 역할 관련 기능을 제공합니다.
 */
class ProjectRoleRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectRoleRemoteDataSource
    // private val localDataSource: ProjectRoleLocalDataSource // 제거
) : ProjectRoleRepository {

    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록 또는 에러
     */
    override suspend fun getRoles(projectId: String): Result<List<Role>> {
        // 원격에서 직접 가져오기 (Firestore 캐시 활용)
        return remoteDataSource.getRoles(projectId)
    }

    /**
     * 특정 프로젝트의 역할 목록 실시간 스트림을 가져옵니다.
     * 서버에서 새로운 역할 목록이 동기화되면 로컬에도 저장합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록의 Flow
     */
    override fun getRolesStream(projectId: String): Flow<List<Role>> {
        // 원격 데이터 소스의 실시간 스트림을 직접 반환 (Firestore 캐시 활용)
        return remoteDataSource.getRolesStream(projectId)
        // .onEach 로컬 저장 로직 제거
    }

    /**
     * 특정 프로젝트의 역할 목록을 새로고침합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun fetchRoles(projectId: String): Result<Unit> {
        // 원격 데이터 소스에서 역할 목록 새로고침 (Firestore 캐시 업데이트 유도)
        return remoteDataSource.fetchRoles(projectId)
        // 로컬 저장 로직 제거
    }

    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * 
     * @param roleId 역할 ID
     * @return 역할 이름과 권한 맵 Pair 또는 에러
     */
    override suspend fun getRoleDetails(roleId: String): Result<Pair<String, Map<RolePermission, Boolean>>> {
        // 원격에서 직접 가져오기 (Firestore 캐시 활용)
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
        // 로컬 저장 로직 제거
        // createRole이 ID 대신 Unit을 반환하도록 remoteDataSource가 변경되었다고 가정, 또는 ID를 받아도 사용 안 함.
        // 성공 시 Unit을 반환하거나, remoteDataSource의 Result를 그대로 반환할 수 있음.
        return if (result.isSuccess) Result.success(Unit) 
               else Result.failure(result.exceptionOrNull() ?: Exception("역할 생성 실패"))
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
        roleId: String, // This is expected to be a composite ID "projectId_actualRoleId"
        name: String,
        permissions: Map<RolePermission, Boolean>
    ): Result<Unit> {
        // Parse roleId to get projectId and actualRoleId
        val parts = roleId.split('_')
        if (parts.size < 2) { // Basic validation for "projectId_roleId" format
            return Result.failure(IllegalArgumentException("Invalid roleId format. Expected 'projectId_roleId'."))
        }
        val projectId = parts[0]
        val actualRoleId = parts.subList(1, parts.size).joinToString("_") // Handle roleIds that might contain underscores

        // 원격 데이터 소스에서 역할 업데이트
        // 로컬 저장 로직 제거
        return remoteDataSource.updateRole(projectId, actualRoleId, name, permissions)
    }

    /**
     * 역할을 삭제합니다.
     * 
     * @param roleId 삭제할 역할 ID
     * @return 성공/실패 결과
     */
    override suspend fun deleteRole(roleId: String): Result<Unit> {
        // 원격 데이터 소스에서 역할 삭제
        // 로컬 삭제 로직 제거
        return remoteDataSource.deleteRole(roleId)
    }
}