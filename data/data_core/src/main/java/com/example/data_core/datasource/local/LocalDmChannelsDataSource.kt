package com.example.data_core.datasource.local

import com.example.domain.model.base.DMChannel
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 DM 채널 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalDMChannelsDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * DM 채널 ID로 단일 DM 채널 조회
     * @param channelId DM 채널 ID
     * @return DM 채널 정보 (없으면 null)
     */
    suspend fun getDMChannelById(channelId: String): DMChannel?

    /**
     * 사용자별 DM 채널 목록 조회
     * @param userId 사용자 ID
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByUser(userId: String): List<DMChannel>

    /**
     * 두 사용자 간의 DM 채널 조회
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return DM 채널 정보 (없으면 null)
     */
    suspend fun getDMChannelBetweenUsers(user1Id: String, user2Id: String): DMChannel?

    /**
     * 활성 상태별 DM 채널 조회
     * @param isActive 활성 상태
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByActiveStatus(isActive: Boolean): List<DMChannel>

    /**
     * 모든 DM 채널 목록 조회
     * @return DM 채널 목록
     */
    suspend fun getAllDMChannels(): List<DMChannel>

    /**
     * 단일 DM 채널 저장
     * @param dmChannel 저장할 DM 채널
     */
    suspend fun saveDMChannel(dmChannel: DMChannel)

    /**
     * DM 채널 목록 배치 저장
     * @param dmChannels 저장할 DM 채널 목록
     */
    suspend fun saveDMChannels(dmChannels: List<DMChannel>)

    /**
     * DM 채널 삭제
     * @param channelId 삭제할 DM 채널 ID
     */
    suspend fun deleteDMChannel(channelId: String)

    /**
     * 사용자별 DM 채널 일괄 삭제
     * @param userId 사용자 ID
     */
    suspend fun deleteDMChannelsByUser(userId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 DM 채널들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 DM 채널 목록
     */
    suspend fun getDMChannelsUpdatedAfter(timestamp: Instant): List<DMChannel>

    /**
     * 사용자별 DM 채널 실시간 관찰
     * @param userId 사용자 ID
     * @return DM 채널 목록 Flow
     */
    fun observeDMChannelsByUser(userId: String): Flow<List<DMChannel>>

    /**
     * 특정 DM 채널 실시간 관찰
     * @param channelId DM 채널 ID
     * @return DM 채널 정보 Flow
     */
    fun observeDMChannelById(channelId: String): Flow<DMChannel?>

    /**
     * 모든 DM 채널 실시간 관찰
     * @return DM 채널 목록 Flow
     */
    fun observeAllDMChannels(): Flow<List<DMChannel>>

    /**
     * 상태별 DM 채널을 실시간 관찰
     * @param status DM 채널 상태
     * @return DM 채널 목록 Flow
     */
    fun observeDMChannelsByStatus(status: DMChannelStatus): Flow<List<DMChannel>>

    /**
     * 특정 DM 채널의 updatedAt 필드 실시간 관찰
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

    /**
     * 상태별 DM 채널 조회
     * @param status DM 채널 상태
     * @return DM 채널 목록
     */
    suspend fun getDMChannelsByStatus(status: DMChannelStatus): List<DMChannel>

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

    /**
     * 두 사용자 간의 DM 채널 존재 여부 확인
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return 존재 여부
     */
    suspend fun dmChannelExistsBetweenUsers(user1Id: String, user2Id: String): Boolean

    /**
     * 상태별 DM 채널 수 조회
     * @param status DM 채널 상태
     * @return 해당 상태의 DM 채널 수
     */
    suspend fun getDMChannelCountByStatus(status: DMChannelStatus): Int

    // === Outbox 관리 ===

    /**
     * DM 채널 변경사항을 Outbox에 기록
     * @param channelId DM 채널 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(channelId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<DMChannelOutboxOperation>

    /**
     * Outbox 작업 완료 처리
     * @param operationId 작업 ID
     */
    suspend fun markOutboxOperationComplete(operationId: String)

    /**
     * Outbox 작업 재시도 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementOutboxRetries(operationId: String)

    // === 동기화 메타데이터 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    // === 유틸리티 ===

    /**
     * DM 채널 존재 여부 확인
     * @param channelId DM 채널 ID
     * @return 존재 여부
     */
    suspend fun dmChannelExists(channelId: String): Boolean

    /**
     * 사용자별 DM 채널 수 조회
     * @param userId 사용자 ID
     * @return DM 채널 수
     */
    suspend fun getDMChannelCount(userId: String): Int

    /**
     * 전체 DM 채널 수 조회
     * @return 전체 DM 채널 수
     */
    suspend fun getTotalDMChannelCount(): Int

    /**
     * 활성 DM 채널 수 조회
     * @return 활성 DM 채널 수
     */
    suspend fun getActiveDMChannelCount(): Int

    /**
     * 모든 DM 채널 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllDMChannels()
}

/**
 * DM 채널 Outbox 작업 정보
 */
data class DMChannelOutboxOperation(
    val id: String,
    val channelId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)