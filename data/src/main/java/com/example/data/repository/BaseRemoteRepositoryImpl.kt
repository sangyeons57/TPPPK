package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.BaseRemoteDataSource
import com.example.domain.repository.base.SyncResult

/**
 * Base Remote Repository Implementation for SSOT Pattern
 * 모든 Remote Repository의 공통 동기화 로직을 제공합니다.
 *
 * @param T 도메인 모델 타입
 * @param remoteDataSource 원격 데이터 소스
 */
abstract class BaseRemoteRepositoryImpl<T>(
    private val remoteDataSource: BaseRemoteDataSource<T>
) {

    /**
     * 서버에서 데이터를 가져옵니다 (Incremental Sync)
     * @param lastSyncCursor 마지막 동기화 커서 (null이면 전체 동기화)
     * @param scope 동기화 범위 (projectId, userId 등)
     * @return 새로운 데이터와 다음 커서
     */
    suspend fun fetch(
        lastSyncCursor: Long? = null,
        scope: String? = null
    ): CustomResult<SyncResult<T>, Exception> {
        return remoteDataSource.syncFromServer(lastSyncCursor, scope)
    }

    /**
     * 로컬 변경사항을 서버로 전송합니다 (Outbox Processing)
     * @param scope 동기화 범위 (projectId, userId 등)
     * @return 처리된 Outbox 작업 수
     */
    suspend fun push(scope: String? = null): CustomResult<Int, Exception> {
        return remoteDataSource.syncToServer(scope)
    }

    /**
     * 강제 전체 동기화 (fetch의 특별한 케이스)
     */
    suspend fun forceSyncAll(scope: String? = null): CustomResult<SyncResult<T>, Exception> {
        return fetch(lastSyncCursor = null, scope = scope)
    }

    /**
     * 동기화 충돌 해결 (fetch + push 조합)
     */
    suspend fun resolveConflicts(
        conflictedIds: List<String>,
        scope: String? = null
    ): CustomResult<Int, Exception> {
        // 충돌된 항목들을 서버에서 다시 가져온 후 로컬에 덮어쓰기
        return remoteDataSource.resolveConflicts(conflictedIds)
    }
}