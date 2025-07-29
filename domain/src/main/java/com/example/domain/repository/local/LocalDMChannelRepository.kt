package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local DM Channel Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote DMChannelRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - DMChannel 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeDMChannelById
 * - observeAllEntities -> observeAllDMChannels
 * - observeEntityUpdatedAt -> observeDMChannelUpdatedAt
 * - getEntityById -> getDMChannelById
 * - getEntitiesByIds -> getDMChannelsByIds
 * - getAllEntities -> getAllDMChannels
 * - saveEntity -> saveDMChannel
 * - saveEntities -> saveDMChannels
 * - deleteEntity -> deleteDMChannel
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalDMChannelRepository : BaseLocalRepository<DMChannel> {

    // === BaseLocalRepository 메서드 (구현체에서 DM 채널 전용 메서드로 매핑) ===
    // observeEntityById -> observeDMChannelById
    // observeAllEntities -> observeAllDMChannels  
    // observeEntityUpdatedAt -> observeDMChannelUpdatedAt
    // getEntityById -> getDMChannelById
    // getEntitiesByIds -> getDMChannelsByIds
    // getAllEntities -> getAllDMChannels
    // saveEntity -> saveDMChannel
    // saveEntities -> saveDMChannels
    // deleteEntity -> deleteDMChannel
    // getEntitiesUpdatedAfter -> getDMChannelsUpdatedAfter
    // clearAllEntities -> clearAllDMChannels
    // getTotalEntityCount -> getTotalDMChannelCount
    // entityExists -> dmChannelExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 DM 채널을 실시간 관찰
     * @param channelId DM 채널 ID
     * @return DM 채널 Flow (null 가능)
     */
    fun observeDMChannelById(channelId: String): Flow<DMChannel?>

    /**
     * 사용자의 모든 DM 채널을 실시간 관찰
     * @param userId 사용자 ID
     * @return DM 채널 목록 Flow
     */
    fun observeDMChannelsByUser(userId: String): Flow<List<DMChannel>>

    /**
     * 두 사용자 간의 DM 채널을 실시간 관찰
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return DM 채널 Flow (null 가능)
     */
    fun observeDMChannelBetweenUsers(user1Id: String, user2Id: String): Flow<DMChannel?>

    /**
     * 상태별 DM 채널을 실시간 관찰
     * @param status DM 채널 상태
     * @return DM 채널 목록 Flow
     */
    fun observeDMChannelsByStatus(status: DMChannelStatus): Flow<List<DMChannel>>

    /**
     * 활성 상태별 DM 채널을 실시간 관찰
     * @param isActive 활성 상태
     * @return DM 채널 목록 Flow
     */
    fun observeDMChannelsByActiveStatus(isActive: Boolean): Flow<List<DMChannel>>

    /**
     * 모든 DM 채널을 실시간 관찰
     * @return 전체 DM 채널 목록 Flow
     */
    fun observeAllDMChannels(): Flow<List<DMChannel>>

    /**
     * 특정 DM 채널의 updatedAt 필드 변경을 실시간 관찰
     * @param channelId DM 채널 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeDMChannelUpdatedAt(channelId: String): Flow<Long?>

    /**
     * 사용자가 차단한 DM 채널들을 실시간 관찰
     * @param userId 사용자 ID
     * @return 차단된 DM 채널 목록 Flow
     */
    fun observeBlockedDMChannelsByUser(userId: String): Flow<List<DMChannel>>

    // === 단순 읽기 작업 ===

    /**
     * DM 채널 ID로 조회
     * @param channelId DM 채널 ID
     * @return DM 채널 (없으면 null)
     */
    suspend fun getDMChannelById(channelId: String): DMChannel?

    /**
     * 사용자의 모든 DM 채널 조회
     * @param userId 사용자 ID
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByUser(userId: String): List<DMChannel>

    /**
     * 두 사용자 간의 DM 채널 조회
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return DM 채널 (없으면 null)
     */
    suspend fun getDMChannelBetweenUsers(user1Id: String, user2Id: String): DMChannel?

    /**
     * 상태별 DM 채널 조회
     * @param status DM 채널 상태
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByStatus(status: DMChannelStatus): List<DMChannel>

    /**
     * 활성 상태별 DM 채널 조회
     * @param isActive 활성 상태
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByActiveStatus(isActive: Boolean): List<DMChannel>

    /**
     * 모든 DM 채널 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return DM 채널 목록
     */
    suspend fun getAllDMChannels(limit: Int? = null): List<DMChannel>

    /**
     * 여러 DM 채널 ID로 조회
     * @param channelIds DM 채널 ID 목록
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByIds(channelIds: List<String>): List<DMChannel>

    /**
     * 사용자가 차단한 DM 채널들 조회
     * @param userId 사용자 ID
     * @return 차단된 DM 채널 목록
     */
    suspend fun getBlockedDMChannelsByUser(userId: String): List<DMChannel>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * DM 채널 저장 (생성/수정)
     * @param dmChannel 저장할 DM 채널
     * @return 성공 여부
     */
    suspend fun saveDMChannel(dmChannel: DMChannel): CustomResult<Unit, Exception>

    /**
     * DM 채널 대량 저장 (동기화용)
     * @param dmChannels 저장할 DM 채널 목록
     * @return 성공 여부
     */
    suspend fun saveDMChannels(dmChannels: List<DMChannel>): CustomResult<Unit, Exception>

    /**
     * DM 채널 삭제 (Soft Delete)
     * @param channelId DM 채널 ID
     * @return 성공 여부
     */
    suspend fun deleteDMChannel(channelId: String): CustomResult<Unit, Exception>

    /**
     * 사용자별 DM 채널 일괄 삭제
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    suspend fun deleteDMChannelsByUser(userId: String): CustomResult<Unit, Exception>

    /**
     * DM 채널 상태 업데이트
     * @param channelId DM 채널 ID
     * @param status 새로운 상태
     * @return 성공 여부
     */
    suspend fun updateDMChannelStatus(
        channelId: String,
        status: DMChannelStatus
    ): CustomResult<Unit, Exception>

    /**
     * DM 채널 아카이브
     * @param channelId DM 채널 ID
     * @return 성공 여부
     */
    suspend fun archiveDMChannel(channelId: String): CustomResult<Unit, Exception>

    /**
     * DM 채널 활성화
     * @param channelId DM 채널 ID
     * @return 성공 여부
     */
    suspend fun activateDMChannel(channelId: String): CustomResult<Unit, Exception>

    /**
     * DM 채널 차단
     * @param channelId DM 채널 ID
     * @param blockerUserId 차단하는 사용자 ID
     * @return 성공 여부
     */
    suspend fun blockDMChannel(
        channelId: String,
        blockerUserId: String
    ): CustomResult<Unit, Exception>

    /**
     * DM 채널 차단 해제
     * @param channelId DM 채널 ID
     * @param unblockerUserId 차단 해제하는 사용자 ID
     * @return 성공 여부
     */
    suspend fun unblockDMChannel(
        channelId: String,
        unblockerUserId: String
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * DM 채널 존재 여부 확인
     * @param channelId DM 채널 ID
     * @return 존재 여부
     */
    suspend fun dmChannelExists(channelId: String): Boolean

    /**
     * 두 사용자 간의 DM 채널 존재 여부 확인
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return 존재 여부
     */
    suspend fun dmChannelExistsBetweenUsers(user1Id: String, user2Id: String): Boolean

    /**
     * 사용자별 DM 채널 수 조회
     * @param userId 사용자 ID
     * @return DM 채널 수
     */
    suspend fun getDMChannelCountByUser(userId: String): Int

    /**
     * 전체 DM 채널 수 조회
     * @return DM 채널 수
     */
    suspend fun getTotalDMChannelCount(): Int

    /**
     * 활성 DM 채널 수 조회
     * @return 활성 DM 채널 수
     */
    suspend fun getActiveDMChannelCount(): Int

    /**
     * 상태별 DM 채널 수 조회
     * @param status DM 채널 상태
     * @return 해당 상태의 DM 채널 수
     */
    suspend fun getDMChannelCountByStatus(status: DMChannelStatus): Int

    /**
     * 모든 DM 채널 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllDMChannels(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 DM 채널 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 DM 채널 목록
     */
    suspend fun getDMChannelsUpdatedAfter(timestamp: Instant): List<DMChannel>
}