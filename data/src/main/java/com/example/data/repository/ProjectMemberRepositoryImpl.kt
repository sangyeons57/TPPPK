package com.example.data.repository

import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource
import com.example.data.datasource.remote.projectmember.ProjectMemberRemoteDataSource
import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result
import kotlinx.coroutines.flow.map

/**
 * ProjectMemberRepository 인터페이스의 구현체
 * 로컬 및 원격 데이터 소스를 조율하여 프로젝트 멤버 데이터를 관리합니다.
 * 
 * @param remoteDataSource 프로젝트 멤버 원격 데이터 소스
 */
@Singleton
class ProjectMemberRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectMemberRemoteDataSource,
    // private val localDataSource: ProjectMemberLocalDataSource, // Removed
    // private val networkMonitor: NetworkConnectivityMonitor // Removed, assuming Firestore cache handles offline
) : ProjectMemberRepository {

    // private val coroutineScope = CoroutineScope(Dispatchers.IO) // Removed

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
        return remoteDataSource.getProjectMembers(projectId)
    }

    /**
     * 특정 프로젝트의 멤버 목록 실시간 스트림을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록의 Flow
     */
    override fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>> {
        return remoteDataSource.getProjectMembersStream(projectId)
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
        // The forceRefresh parameter is ignored as the remote data source's
        // getProjectMember method does not support it directly.
        // Firestore's caching mechanism handles data freshness.
        return remoteDataSource.getProjectMember(projectId, userId)
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
        val result = remoteDataSource.addMemberToProject(projectId, userId, roleIds)
        return result
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
        val result = remoteDataSource.removeMemberFromProject(projectId, userId)
        return result
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
        val result = remoteDataSource.updateMemberRoles(projectId, userId, roleIds)
        return result
    }

    /**
     * 특정 프로젝트의 모든 멤버 데이터를 동기화합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun syncProjectMembers(projectId: String): Result<Unit> {
        return remoteDataSource.getProjectMembers(projectId).map { } 
    }

    /**
     * 멤버 목록 새로고침 (호환성 유지용)
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun fetchProjectMembers(projectId: String): Result<Unit> = syncProjectMembers(projectId)

    /**
     * 멤버에게 채널 접근 권한을 추가합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun addChannelAccessToMember(
        projectId: String, 
        userId: String, 
        channelId: String
    ): Result<Unit> {
        return try {
            val result = remoteDataSource.addChannelAccessToMember(projectId, userId, channelId)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 멤버의 채널 접근 권한을 제거합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun removeChannelAccessFromMember(
        projectId: String, 
        userId: String, 
        channelId: String
    ): Result<Unit> {
        return try {
            val result = remoteDataSource.removeChannelAccessFromMember(projectId, userId, channelId)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 채널에 접근 가능한 모든 멤버 ID 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 멤버 ID 목록
     */
    override suspend fun getMembersWithChannelAccess(
        projectId: String, 
        channelId: String
    ): Result<List<String>> {
        return remoteDataSource.getMembersWithChannelAccess(projectId, channelId)
    }
    
    /**
     * 멤버가 접근 가능한 모든 채널 ID 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 채널 ID 목록
     */
    override suspend fun getMemberChannelAccess(
        projectId: String, 
        userId: String
    ): Result<List<String>> {
        return remoteDataSource.getMemberChannelAccess(projectId, userId)
    }
    
    /**
     * 사용자가 특정 채널에 접근할 수 있는지 확인합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @return 접근 가능 여부
     */
    override suspend fun canAccessChannel(
        projectId: String, 
        channelId: String, 
        userId: String
    ): Result<Boolean> {
        return try {
            val channelsResult = remoteDataSource.getMemberChannelAccess(projectId, userId)
            if (channelsResult.isSuccess) {
                val channels = channelsResult.getOrThrow()
                Result.success(channels.contains(channelId))
            } else {
                Result.failure(channelsResult.exceptionOrNull() ?: Exception("채널 접근 권한을 확인하는데 실패했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}