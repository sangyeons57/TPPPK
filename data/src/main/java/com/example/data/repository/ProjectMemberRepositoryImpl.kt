package com.example.data.repository

import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource
import com.example.data.datasource.remote.projectmember.ProjectMemberRemoteDataSource
import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import com.example.domain.util.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * ProjectMemberRepository 인터페이스의 구현체
 * 로컬 및 원격 데이터 소스를 조율하여 프로젝트 멤버 데이터를 관리합니다.
 * 
 * @param remoteDataSource 프로젝트 멤버 원격 데이터 소스
 * @param localDataSource 프로젝트 멤버 로컬 데이터 소스
 * @param networkMonitor 네트워크 연결 상태 모니터
 */
@Singleton
class ProjectMemberRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectMemberRemoteDataSource,
    private val localDataSource: ProjectMemberLocalDataSource,
    private val networkMonitor: NetworkConnectivityMonitor
) : ProjectMemberRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param forceRefresh 강제로 원격 데이터를 가져올지 여부
     * @return 프로젝트 멤버 목록 결과
     */
    override suspend fun getProjectMembers(
        projectId: String,
        forceRefresh: Boolean
    ): Result<List<ProjectMember>> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        // 오프라인이고 강제 새로고침이 아닌 경우 로컬 데이터 반환
        if (!isConnected && !forceRefresh) {
            val localMembers = localDataSource.getProjectMembers(projectId)
            return Result.success(localMembers)
        }
        
        // 네트워크 연결된 경우 또는 강제 새로고침인 경우
        return try {
            // 원격에서 데이터 가져오기
            val remoteResult = remoteDataSource.getProjectMembers(projectId)
            
            if (remoteResult.isSuccess) {
                val members = remoteResult.getOrThrow()
                // 로컬 캐시 업데이트
                localDataSource.saveProjectMembers(projectId, members)
                Result.success(members)
            } else {
                // 원격 요청 실패 시 로컬 데이터 반환
                val localMembers = localDataSource.getProjectMembers(projectId)
                if (localMembers.isNotEmpty()) {
                    Result.success(localMembers)
                } else {
                    Result.failure(remoteResult.exceptionOrNull() 
                        ?: Exception("멤버 목록을 가져오는데 실패했습니다."))
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 로컬 데이터 반환
            val localMembers = localDataSource.getProjectMembers(projectId)
            if (localMembers.isNotEmpty()) {
                Result.success(localMembers)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * 특정 프로젝트의 멤버 목록 실시간 스트림을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록의 Flow
     */
    override fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>> {
        // 별도의 코루틴으로 원격 데이터 스트림을 구독하고 로컬에 저장하는 과정 시작
        coroutineScope.launch {
            try {
                // 네트워크 연결된 경우에만 원격 스트림 구독
                if (networkMonitor.isNetworkAvailable.first()) {
                    // 원격 스트림 수신하여 로컬에 저장
                    remoteDataSource.getProjectMembersStream(projectId)
                        .collect { members ->
                            localDataSource.saveProjectMembers(projectId, members)
                        }
                }
            } catch (e: Exception) {
                // 예외 발생 시 로깅 등 처리 (여기서는 무시)
            }
        }
        
        // 로컬 데이터 스트림 반환
        return localDataSource.getProjectMembersStream(projectId)
    }

    /**
     * 특정 프로젝트 멤버 정보를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param forceRefresh 강제로 원격 데이터를 가져올지 여부
     * @return 프로젝트 멤버 또는 null
     */
    override suspend fun getProjectMember(
        projectId: String,
        userId: String,
        forceRefresh: Boolean
    ): Result<ProjectMember?> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        // 오프라인이고 강제 새로고침이 아닌 경우 로컬 데이터 반환
        if (!isConnected && !forceRefresh) {
            val localMember = localDataSource.getProjectMember(projectId, userId)
            return Result.success(localMember)
        }
        
        // 네트워크 연결된 경우 또는 강제 새로고침인 경우
        return try {
            // 전체 멤버 목록 가져오기
            val remoteResult = remoteDataSource.getProjectMembers(projectId)
            
            if (remoteResult.isSuccess) {
                val members = remoteResult.getOrThrow()
                // 로컬 캐시 업데이트
                localDataSource.saveProjectMembers(projectId, members)
                
                // 특정 멤버 찾기
                val member = members.find { it.userId == userId }
                Result.success(member)
            } else {
                // 원격 요청 실패 시 로컬 데이터 반환
                val localMember = localDataSource.getProjectMember(projectId, userId)
                Result.success(localMember)
            }
        } catch (e: Exception) {
            // 예외 발생 시 로컬 데이터 반환
            val localMember = localDataSource.getProjectMember(projectId, userId)
            Result.success(localMember)
        }
    }

    /**
     * 프로젝트에 새 멤버를 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @param roleIds 부여할 역할 ID 목록
     * @return 작업 성공 여부
     */
    override suspend fun addMemberToProject(
        projectId: String,
        userId: String,
        roleIds: List<String>
    ): Result<Unit> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에 멤버 추가
            val result = remoteDataSource.addMemberToProject(projectId, userId, roleIds)
            
            if (result.isSuccess) {
                // 성공 시 로컬 캐시 갱신을 위해 멤버 목록 동기화
                syncProjectMembers(projectId)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 프로젝트에서 멤버를 제거합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 제거할 사용자 ID
     * @return 작업 성공 여부
     */
    override suspend fun removeMemberFromProject(
        projectId: String,
        userId: String
    ): Result<Unit> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에서 멤버 제거
            val result = remoteDataSource.removeMemberFromProject(projectId, userId)
            
            if (result.isSuccess) {
                // 성공 시 로컬에서도 제거
                localDataSource.removeProjectMember(projectId, userId)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 멤버의 역할을 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleIds 새로운 역할 ID 목록
     * @return 작업 성공 여부
     */
    override suspend fun updateMemberRoles(
        projectId: String,
        userId: String,
        roleIds: List<String>
    ): Result<Unit> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에서 역할 업데이트
            val result = remoteDataSource.updateMemberRoles(projectId, userId, roleIds)
            
            if (result.isSuccess) {
                // 성공 시 로컬 캐시 갱신을 위해 멤버 목록 동기화
                syncProjectMembers(projectId)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 특정 프로젝트의 모든 멤버 데이터를 동기화합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun syncProjectMembers(projectId: String): Result<Unit> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에서 멤버 목록 가져오기
            val remoteResult = remoteDataSource.getProjectMembers(projectId)
            
            if (remoteResult.isSuccess) {
                val members = remoteResult.getOrThrow()
                // 로컬 저장소 업데이트
                localDataSource.saveProjectMembers(projectId, members)
                Result.success(Unit)
            } else {
                Result.failure(remoteResult.exceptionOrNull() 
                    ?: Exception("멤버 목록 동기화에 실패했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 멤버 목록 새로고침 (호환성 유지용)
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun fetchProjectMembers(projectId: String): Result<Unit> = syncProjectMembers(projectId)
}