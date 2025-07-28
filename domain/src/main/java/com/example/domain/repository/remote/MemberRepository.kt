package com.example.domain.repository.remote

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext

/**
 * Remote Member Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface MemberRepository {
    val factoryContext: MemberRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectId: String? = null
    ): CustomResult<SyncResult<Member>, Exception>

    suspend fun syncToServer(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedMemberIds: List<String>
    ): CustomResult<Int, Exception>
}
