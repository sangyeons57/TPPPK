package com.example.data.datasource.local

import com.example.domain.model.base.ProjectChannel
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 프로젝트 채널 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalProjectChannelsDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 채널 ID로 단일 채널 조회
     * @param channelId 채널 ID
     * @return 채널 정보 (없으면 null)
     */
    suspend fun getChannelById(channelId: String): ProjectChannel?

    /**
     * 모든 채널 목록 조회
     * @return 채널 목록
     */
    suspend fun getAllChannels(): List<ProjectChannel>

    /**
     * 카테고리 ID로 채널 목록 조회 (order별 정렬)
     * @param categoryId 카테고리 ID
     * @return 채널 목록 (order순 정렬)
     */
    suspend fun getChannelsByCategory(categoryId: String): List<ProjectChannel>

    /**
     * 채널 타입별 채널 목록 조회
     * @param channelType 채널 타입 (TEXT, VOICE, VIDEO 등)
     * @return 채널 목록
     */
    suspend fun getChannelsByType(channelType: String): List<ProjectChannel>

    /**
     * 채널 상태별 채널 목록 조회
     * @param status 채널 상태 (ACTIVE, ARCHIVED, DISABLED, DELETED)
     * @return 채널 목록
     */
    suspend fun getChannelsByStatus(status: String): List<ProjectChannel>

    /**
     * 채널 이름으로 검색
     * @param nameQuery 검색할 이름 (부분 일치)
     * @return 채널 목록
     */
    suspend fun searchChannelsByName(nameQuery: String): List<ProjectChannel>

    /**
     * 카테고리 내에서 특정 order 이후의 채널들 조회
     * @param categoryId 카테고리 ID
     * @param order 기준 order 값
     * @return 채널 목록
     */
    suspend fun getChannelsAfterOrder(categoryId: String, order: Int): List<ProjectChannel>

    /**
     * 단일 채널 정보 저장
     * @param channel 저장할 채널 정보
     */
    suspend fun saveChannel(channel: ProjectChannel)

    /**
     * 채널 목록 배치 저장
     * @param channels 저장할 채널 목록
     */
    suspend fun saveChannels(channels: List<ProjectChannel>)

    /**
     * 채널 삭제
     * @param channelId 삭제할 채널 ID
     */
    suspend fun deleteChannel(channelId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 채널들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 채널 목록
     */
    suspend fun getChannelsUpdatedAfter(timestamp: Instant): List<ProjectChannel>

    /**
     * 채널 실시간 관찰
     * @param channelId 채널 ID
     * @return 채널 정보 Flow
     */
    fun observeChannelById(channelId: String): Flow<ProjectChannel?>

    /**
     * 모든 채널 실시간 관찰
     * @return 채널 목록 Flow
     */
    fun observeAllChannels(): Flow<List<ProjectChannel>>

    /**
     * 카테고리별 채널 실시간 관찰
     * @param categoryId 카테고리 ID
     * @return 채널 목록 Flow
     */
    fun observeChannelsByCategory(categoryId: String): Flow<List<ProjectChannel>>

    // === Outbox 관리 ===

    /**
     * 채널 변경사항을 Outbox에 기록
     * @param channelId 채널 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(channelId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<ProjectChannelOutboxOperation>

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
     * 채널 존재 여부 확인
     * @param channelId 채널 ID
     * @return 존재 여부
     */
    suspend fun channelExists(channelId: String): Boolean

    /**
     * 전체 채널 수 조회
     * @return 채널 수
     */
    suspend fun getChannelCount(): Int

    /**
     * 카테고리별 채널 수 조회
     * @param categoryId 카테고리 ID
     * @return 채널 수
     */
    suspend fun getChannelCountByCategory(categoryId: String): Int

    /**
     * 카테고리 내 다음 사용 가능한 order 값 조회
     * @param categoryId 카테고리 ID
     * @return 다음 order 값
     */
    suspend fun getNextOrderInCategory(categoryId: String): Int

    /**
     * 모든 채널 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllChannels()
}

/**
 * 프로젝트 채널 Outbox 작업 정보
 */
data class ProjectChannelOutboxOperation(
    val id: String,
    val channelId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)