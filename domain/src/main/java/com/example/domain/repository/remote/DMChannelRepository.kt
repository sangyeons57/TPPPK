package com.example.domain.repository.remote

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext

/**
 * Remote DM Channel Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 *
 * 🔒 제약사항:
 * - Room 접근 금지 (LocalDMChannelRepository 사용)
 * - Flow/LiveData 반환 금지 (비동기 fetch-only)
 * - 직접적인 CRUD 작업 불가능
 *
 * ✅ 역할:
 * - 서버에서 증분 데이터 가져오기 (updatedAt > cursor)
 * - 로컬 변경사항을 서버에 반영 (Outbox → Firestore)
 * - 동기화 충돌 해결 (서버 vs 로컬)
 * - 커서 기반 동기화 메타데이터 관리
 * - Firebase Functions 호출 (DM 채널 관리)
 */
interface DMChannelRepository {
    val factoryContext: DMChannelRepositoryFactoryContext

    // === 동기화 메서드 ===

    /**
     * 서버에서 증분 데이터 가져오기 (Client-driven Sync)
     * @param lastSyncCursor 마지막 동기화 커서 (null이면 전체 동기화)
     * @param userId 특정 사용자의 DM 채널만 동기화 (null이면 전체)
     * @return 새로운 DM 채널 목록과 다음 커서
     */
    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        userId: String? = null
    ): CustomResult<SyncResult<DMChannel>, Exception>

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
     * @return 동기화된 DM 채널 수
     */
    suspend fun forceSyncAll(
        userId: String? = null
    ): CustomResult<Int, Exception>

    /**
     * 동기화 충돌 해결 (서버 우선 정책)
     * @param conflictedChannelIds 충돌이 발생한 DM 채널 ID 목록
     * @return 해결된 충돌 수
     */
    suspend fun resolveConflicts(
        conflictedChannelIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions 호출 (서버 작업) ===
    
    /**
     * 사용자 이름을 통해 DM 채널을 생성합니다.
     *
     * @param targetUserName 대상 사용자 이름
     * @return 성공 시 DM 채널 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun createDMChannel(targetUserName: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * DM 채널을 차단합니다.
     *
     * @param channelId 차단할 DM 채널 ID
     * @return 성공 시 차단 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * DM 채널 차단을 해제합니다.
     *
     * @param channelId 차단 해제할 DM 채널 ID
     * @return 성공 시 차단 해제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * 사용자 이름을 통해 DM 채널 차단을 해제합니다.
     *
     * @param targetUserName 차단 해제할 대상 사용자 이름
     * @return 성공 시 차단 해제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun unblockDMChannelByUserName(targetUserName: String): CustomResult<Map<String, Any?>, Exception>
}
