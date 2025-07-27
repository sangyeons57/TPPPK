package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.domain.model.base.DMWrapper
import com.example.domain.repository.base.DMWrapperRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote DMWrapper Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    override val factoryContext: DMWrapperRepositoryFactoryContext
) : DMWrapperRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        userId: String?
    ): CustomResult<SyncResult<DMWrapper>, Exception> {
        return dmWrapperRemoteDataSource.syncFromServer(lastSyncCursor, userId)
    }

    override suspend fun syncToServer(
        userId: String?
    ): CustomResult<Int, Exception> {
        return dmWrapperRemoteDataSource.syncToServer(userId)
    }

    override suspend fun forceSyncAll(
        userId: String?
    ): CustomResult<Int, Exception> {
        return dmWrapperRemoteDataSource.forceSyncAll(userId)
    }

    override suspend fun resolveConflicts(
        conflictedWrapperIds: List<String>
    ): CustomResult<Int, Exception> {
        return dmWrapperRemoteDataSource.resolveConflicts(conflictedWrapperIds)
    }
}
