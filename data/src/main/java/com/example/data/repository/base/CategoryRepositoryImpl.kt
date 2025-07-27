package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.repository.BaseRemoteRepositoryImpl
import com.example.domain.model.base.Category
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote Category Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class CategoryRepositoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    override val factoryContext: CategoryRepositoryFactoryContext,
) : BaseRemoteRepositoryImpl<Category>(categoryRemoteDataSource), CategoryRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?
    ): CustomResult<SyncResult<Category>, Exception> {
        return fetch(lastSyncCursor, projectId)
    }

    override suspend fun syncToServer(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return push(projectId)
    }

    override suspend fun forceSyncAll(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return push(projectId) // forceSyncAll은 사실상 push와 동일
    }

    override suspend fun resolveConflicts(
        conflictedCategoryIds: List<String>
    ): CustomResult<Int, Exception> {
        return resolveConflicts(conflictedCategoryIds, null)
    }
}
