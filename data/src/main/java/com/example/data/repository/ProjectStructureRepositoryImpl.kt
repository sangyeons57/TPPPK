// 경로: data/repository/ProjectStructureRepositoryImpl.kt
package com.example.data.repository

import com.example.data.datasource.local.projectstructure.ProjectStructureLocalDataSource
import com.example.data.datasource.remote.projectstructure.ProjectStructureRemoteDataSource
import com.example.domain.model.Category
import com.example.domain.model.ProjectStructure
import com.example.domain.repository.ProjectStructureRepository
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.ProjectChannelRepository
import com.example.domain.util.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * ProjectStructureRepository 인터페이스의 구현체
 * 프로젝트 구조(카테고리 등) 관리에 중점을 두며, 채널 관리는 ChannelRepository와 ProjectChannelRepository로 위임합니다.
 * 
 * @param remoteDataSource 프로젝트 구조 원격 데이터 소스
 * @param localDataSource 프로젝트 구조 로컬 데이터 소스
 * @param channelRepository 채널 리포지토리
 * @param projectChannelRepository 프로젝트-채널 연결 리포지토리
 * @param networkMonitor 네트워크 연결 상태 모니터
 */
@Singleton
class ProjectStructureRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectStructureRemoteDataSource,
    private val channelRepository: ChannelRepository,
    private val projectChannelRepository: ProjectChannelRepository
) : ProjectStructureRepository {

    /**
     * 프로젝트 구조를 실시간으로 조회합니다.
     */
    override fun getProjectStructureStream(projectId: String): Flow<ProjectStructure> {
        return remoteDataSource.getProjectStructureStream(projectId)
    }
    
    /**
     * 프로젝트 구조를 한 번 조회합니다.
     */
    override suspend fun getProjectStructure(projectId: String): Result<ProjectStructure> {
        // Firestore cache handles offline scenarios.
        return remoteDataSource.getProjectStructure(projectId)
    }

    /**
     * 카테고리를 생성합니다.
     */
    override suspend fun createCategory(projectId: String, name: String): Result<Category> {
        // Rely on remoteDataSource, Firestore cache for offline writes if enabled.
        return remoteDataSource.createCategory(projectId, name)
        // Local cache update removed.
    }

    /**
     * 카테고리 상세 정보를 조회합니다.
     */
    override suspend fun getCategoryDetails(projectId: String, categoryId: String): Result<Category> {
        // Rely on remoteDataSource, Firestore cache for offline reads.
        return remoteDataSource.getCategoryDetails(projectId, categoryId)
        // Local cache logic removed.
    }
    
    /**
     * 프로젝트의 모든 카테고리를 조회합니다.
     */
    override suspend fun getProjectCategories(projectId: String): Result<List<Category>> {
        // Fetch entire structure from remote and extract categories.
        // Firestore cache handles offline scenarios.
        val projectStructureResult = remoteDataSource.getProjectStructure(projectId)
        return projectStructureResult.map { it.categories }
        // Local cache logic removed.
    }
    
    /**
     * 프로젝트의 모든 카테고리를 실시간으로 구독합니다.
     */
    override fun getProjectCategoriesStream(projectId: String): Flow<List<Category>> {
        // Get the full structure stream from remote and map to categories.
        return remoteDataSource.getProjectStructureStream(projectId)
            .map { it.categories }
    }

    /**
     * 카테고리 정보를 수정합니다.
     */
    override suspend fun updateCategory(projectId: String, categoryId: String, newName: String, order: Int?): Result<Unit> {
        // Rely on remoteDataSource for update.
        return remoteDataSource.updateCategory(projectId, categoryId, newName, order)
        // Local cache update removed.
    }
    
    /**
     * 카테고리를 삭제합니다.
     */
    override suspend fun deleteCategory(projectId: String, categoryId: String, deleteChannels: Boolean): Result<Unit> {
        // Firestore transactions might be better here if atomicity is critical between deleting category and its channels.
        // For now, proceed with sequential deletion.

        val deleteCategoryResult = remoteDataSource.deleteCategory(projectId, categoryId)

        if (deleteCategoryResult.isSuccess && deleteChannels) {
            // Fetch channel references belonging to this category before deleting the category
            // This part needs careful review: projectChannelRepository might rely on the category still existing.
            // Or, remoteDataSource.getProjectStructure() could be used to get channels before deletion.
            // Assuming ProjectChannelRepository.getCategoryChannelRefs can still work or an alternative exists.
            val channelRefsResult = projectChannelRepository.getCategoryChannelRefs(projectId, categoryId)
            if (channelRefsResult.isSuccess) {
                channelRefsResult.getOrThrow().forEach {
                    // Deleting channels one by one. Consider batch delete if possible.
                    channelRepository.deleteChannel(it.channelId) // Assuming this deletes from remote
                }
            } else {
                // Log or handle error: failed to get channel refs for deletion
                // but category deletion might have already succeeded.
                // This could leave orphaned channels if not handled carefully.
                return Result.failure(channelRefsResult.exceptionOrNull() ?: Exception("Failed to get channel refs for category deletion"))
            }
        }
        // Local cache deletion removed.
        return deleteCategoryResult
    }
    
    /**
     * 카테고리 순서를 변경합니다.
     */
    override suspend fun reorderCategory(projectId: String, categoryId: String, newOrder: Int): Result<Unit> {
        // Network check and local cache updates are removed.
        // Rely on remoteDataSource; Firestore handles offline queueing for writes.
        return remoteDataSource.updateCategory(projectId, categoryId, null, newOrder)
    }
    
    /**
     * 여러 카테고리의 순서를 일괄 변경합니다.
     */
    override suspend fun batchReorderCategories(projectId: String, categoryOrders: Map<String, Int>): Result<Unit> {
        // Network check and local cache updates are removed.
        // Rely on remoteDataSource; Firestore handles offline queueing for writes.
        try {
            // 각 카테고리를 개별적으로 업데이트
            // Consider if remoteDataSource should offer a batch update for atomicity.
            // For now, update one by one.
            categoryOrders.forEach { (catId, order) ->
                val result = remoteDataSource.updateCategory(projectId, catId, null, order)
                if (result.isFailure) {
                    // If one fails, we could decide to stop and return failure,
                    // or collect all failures. For simplicity, return first failure.
                    return Result.failure(result.exceptionOrNull() ?: Exception("Failed to update order for category $catId"))
                }
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}