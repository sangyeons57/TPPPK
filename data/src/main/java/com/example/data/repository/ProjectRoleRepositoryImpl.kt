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
     * @return 역할 정보 또는 에러
     */
    override suspend fun getRoleDetails(projectId: String, roleId: String): Result<Role?> {
        return remoteDataSource.getRoleDetails(projectId, roleId)
    }

    /**
     * 새 역할을 생성합니다.
     * 
     * @param projectId 역할이 생성될 프로젝트 ID
     * @param name 새 역할 이름
     * @param permissions 새 역할의 권한 맵
     * @param isDefault 기본 역할 여부
     * @return 생성된 역할 ID 또는 에러
     */
    override suspend fun createRole(
        projectId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>,
        isDefault: Boolean
    ): Result<String> {
        return remoteDataSource.createRole(projectId, name, permissions, isDefault)
    }

    /**
     * 기존 역할을 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 수정할 역할 ID
     * @param name 새 역할 이름
     * @param permissions 새 권한 맵
     * @param isDefault 기본 역할 여부 (null이면 변경하지 않음)
     * @return 성공/실패 결과
     */
    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>,
        isDefault: Boolean?
    ): Result<Unit> {
        // The remoteDataSource.updateRole expects non-nullable name and permissions.
        // The isDefault parameter is optional for the remote source.
        return remoteDataSource.updateRole(projectId, roleId, name, permissions, isDefault)
    }

    /**
     * 역할을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 삭제할 역할 ID
     * @return 성공/실패 결과
     */
    override suspend fun deleteRole(projectId: String, roleId: String): Result<Unit> {
        return remoteDataSource.deleteRole(projectId, roleId)
    }
}