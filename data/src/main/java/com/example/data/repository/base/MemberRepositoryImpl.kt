package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.domain.model.base.Member
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote Member Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class MemberRepositoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource,
    override val factoryContext: MemberRepositoryFactoryContext
) : MemberRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?
    ): CustomResult<SyncResult<Member>, Exception> {
        return memberRemoteDataSource.syncFromServer(lastSyncCursor, projectId)
    }

    override suspend fun syncToServer(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return memberRemoteDataSource.syncToServer(projectId)
    }

    override suspend fun forceSyncAll(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return memberRemoteDataSource.forceSyncAll(projectId)
    }

    override suspend fun resolveConflicts(
        conflictedMemberIds: List<String>
    ): CustomResult<Int, Exception> {
        return memberRemoteDataSource.resolveConflicts(conflictedMemberIds)
    }
}
