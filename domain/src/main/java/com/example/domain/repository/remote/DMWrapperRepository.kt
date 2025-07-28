package com.example.domain.repository.remote

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext

/**
 * Remote DM Wrapper Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 *
 * 🔒 제약사항:
 * - Room 접근 금지 (LocalDMWrapperRepository 사용)
 * - Flow/LiveData 반환 금지 (비동기 fetch-only)
 * - 직접적인 CRUD 작업 불가능
 *
 * ✅ 역할:
 * - 서버에서 증분 데이터 가져오기 (updatedAt > cursor)
 * - 로컬 변경사항을 서버에 반영 (Outbox → Firestore)
 * - 동기화 충돌 해결 (서버 vs 로컬)
 * - 커서 기반 동기화 메타데이터 관리
 * - DM Wrapper 메타데이터 동기화
 */
interface DMWrapperRepository {
    val factoryContext: DMWrapperRepositoryFactoryContext

    /**
     * 서버에서 증분 데이터 가져오기 (Client-driven Sync)
     * @param lastSyncCursor 마지막 동기화 커서 (null이면 전체 동기화)
     * @param userId 특정 사용자의 DM Wrapper만 동기화 (null이면 전체)
     * @return 새로운 DM Wrapper 목록과 다음 커서
     */
    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        userId: String? = null
    ): CustomResult<SyncResult<DMWrapper>, Exception>

    /**
     * 로컬 변경사항을 서버에 반영 (Outbox Processing)
     * @param userId 특정 사용자의 Outbox만 처리 (null이면 전체)
     * @return 처리된 Outbox 작업 수
     */
    suspend fun syncToServer(
        userId: String? = null
    ): CustomResult<Int, Exception>

    /**
     * 강제 전체 동기화 (예: 첫 로그인, 데이터 불일치 해결)
     * @param userId 특정 사용자만 동기화 (null이면 전체)
     * @return 동기화된 DM Wrapper 수
     */
    suspend fun forceSyncAll(
        userId: String? = null
    ): CustomResult<Int, Exception>

    /**
     * 동기화 충돌 해결 (서버 우선 정책)
     * @param conflictedWrapperIds 충돌이 발생한 DM Wrapper ID 목록
     * @return 해결된 충돌 수
     */
    suspend fun resolveConflicts(
        conflictedWrapperIds: List<String>
    ): CustomResult<Int, Exception>
}
