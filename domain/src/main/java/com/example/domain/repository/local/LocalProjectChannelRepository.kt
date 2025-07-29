package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelStatus
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Project Channel Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote ProjectChannelRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - ProjectChannel 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeChannelById
 * - observeAllEntities -> observeAllChannels
 * - observeEntityUpdatedAt -> observeChannelUpdatedAt
 * - getEntityById -> getChannelById
 * - getEntitiesByIds -> getChannelsByIds
 * - getAllEntities -> getAllChannels
 * - saveEntity -> saveChannel
 * - saveEntities -> saveChannels
 * - deleteEntity -> deleteChannel
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalProjectChannelRepository : BaseLocalRepository<ProjectChannel> {

    // === BaseLocalRepository 메서드 (구현체에서 채널 전용 메서드로 매핑) ===
    // observeEntityById -> observeChannelById
    // observeAllEntities -> observeAllChannels  
    // observeEntityUpdatedAt -> observeChannelUpdatedAt
    // getEntityById -> getChannelById
    // getEntitiesByIds -> getChannelsByIds
    // getAllEntities -> getAllChannels
    // saveEntity -> saveChannel
    // saveEntities -> saveChannels
    // deleteEntity -> deleteChannel
    // getEntitiesUpdatedAfter -> getChannelsUpdatedAfter
    // clearAllEntities -> clearAllChannels
    // getTotalEntityCount -> getTotalChannelCount
    // entityExists -> channelExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 프로젝트 채널을 실시간 관찰
     * @param channelId 채널 ID
     * @return 채널 Flow (null 가능)
     */
    fun observeChannelById(channelId: String): Flow<ProjectChannel?>

    /**
     * 주어진 이름과 정확히 일치하는 채널을 실시간 관찰
     * @param name 채널 이름
     * @return 채널 Flow
     */
    fun observeByName(name: Name): Flow<ProjectChannel?>

    /**
     * 주어진 이름을 포함하는 채널 목록을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 채널 목록 Flow
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<List<ProjectChannel>>

    /**
     * 특정 카테고리의 채널들을 순서대로 실시간 관찰
     * @param categoryId 카테고리 ID
     * @return 채널 목록 Flow (order 순서)
     */
    fun observeChannelsByCategory(categoryId: String): Flow<List<ProjectChannel>>

    /**
     * 특정 채널 타입의 채널들을 실시간 관찰
     * @param channelType 채널 타입
     * @return 채널 목록 Flow
     */
    fun observeChannelsByType(channelType: ProjectChannelType): Flow<List<ProjectChannel>>

    /**
     * 특정 상태의 채널들을 실시간 관찰
     * @param status 채널 상태
     * @return 채널 목록 Flow
     */
    fun observeChannelsByStatus(status: ProjectChannelStatus): Flow<List<ProjectChannel>>

    /**
     * 주어진 ID 목록에 해당하는 채널 목록을 실시간 관찰
     * @param channelIds 채널 ID 목록
     * @return 채널 목록 Flow
     */
    fun observeChannels(channelIds: List<String>): Flow<List<ProjectChannel>>

    /**
     * 특정 채널의 updatedAt 필드 변경을 실시간 관찰
     * @param channelId 채널 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeChannelUpdatedAt(channelId: String): Flow<Long?>

    /**
     * 모든 채널을 실시간 관찰
     * @return 전체 채널 목록 Flow
     */
    fun observeAllChannels(): Flow<List<ProjectChannel>>

    /**
     * 특정 순서 범위의 채널들을 실시간 관찰
     * @param categoryId 카테고리 ID
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 채널 목록 Flow
     */
    fun observeChannelsByOrderRange(
        categoryId: String,
        minOrder: Int,
        maxOrder: Int
    ): Flow<List<ProjectChannel>>

    // === 단순 읽기 작업 ===

    /**
     * 채널 ID로 조회
     * @param channelId 채널 ID
     * @return 채널 (없으면 null)
     */
    suspend fun getChannelById(channelId: String): ProjectChannel?

    /**
     * 채널 이름으로 조회 (정확히 일치)
     * @param name 채널 이름
     * @return 채널 (없으면 null)
     */
    suspend fun getChannelByName(name: Name): ProjectChannel?

    /**
     * 채널 이름으로 검색 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 채널 목록
     */
    suspend fun searchChannelsByName(name: String, limit: Int = 10): List<ProjectChannel>

    /**
     * 여러 채널 ID로 조회
     * @param channelIds 채널 ID 목록
     * @return 채널 목록
     */
    suspend fun getChannelsByIds(channelIds: List<String>): List<ProjectChannel>

    /**
     * 전체 채널 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 채널 목록
     */
    suspend fun getAllChannels(limit: Int? = null): List<ProjectChannel>

    /**
     * 특정 카테고리의 채널들을 순서대로 조회
     * @param categoryId 카테고리 ID
     * @return 채널 목록 (order 순서)
     */
    suspend fun getChannelsByCategory(categoryId: String): List<ProjectChannel>

    /**
     * 특정 채널 타입의 채널들 조회
     * @param channelType 채널 타입
     * @return 채널 목록
     */
    suspend fun getChannelsByType(channelType: ProjectChannelType): List<ProjectChannel>

    /**
     * 특정 상태의 채널들 조회
     * @param status 채널 상태
     * @return 채널 목록
     */
    suspend fun getChannelsByStatus(status: ProjectChannelStatus): List<ProjectChannel>

    /**
     * 특정 순서 범위의 채널들 조회
     * @param categoryId 카테고리 ID
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 채널 목록
     */
    suspend fun getChannelsByOrderRange(
        categoryId: String,
        minOrder: Int,
        maxOrder: Int
    ): List<ProjectChannel>

    /**
     * 특정 순서 이후의 채널들 조회
     * @param categoryId 카테고리 ID
     * @param order 기준 순서
     * @return 채널 목록
     */
    suspend fun getChannelsAfterOrder(categoryId: String, order: Int): List<ProjectChannel>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 채널 저장 (생성/수정)
     * @param channel 저장할 채널
     * @return 성공 여부
     */
    suspend fun saveChannel(channel: ProjectChannel): CustomResult<Unit, Exception>

    /**
     * 채널 대량 저장 (동기화용)
     * @param channels 저장할 채널 목록
     * @return 성공 여부
     */
    suspend fun saveChannels(channels: List<ProjectChannel>): CustomResult<Unit, Exception>

    /**
     * 채널 삭제 (Soft Delete)
     * @param channelId 채널 ID
     * @return 성공 여부
     */
    suspend fun deleteChannel(channelId: String): CustomResult<Unit, Exception>

    /**
     * 채널 정보 업데이트 (로컬)
     * @param channelId 채널 ID
     * @param name 새로운 이름 (nullable)
     * @param order 새로운 순서 (nullable)
     * @param status 새로운 상태 (nullable)
     * @param categoryId 새로운 카테고리 ID (nullable)
     * @return 성공 여부
     */
    suspend fun updateChannel(
        channelId: String,
        name: Name? = null,
        order: ProjectChannelOrder? = null,
        status: ProjectChannelStatus? = null,
        categoryId: String? = null
    ): CustomResult<Unit, Exception>

    /**
     * 채널 순서 재정렬
     * @param channelOrderMap 채널 ID와 새로운 순서 매핑
     * @return 성공 여부
     */
    suspend fun reorderChannels(channelOrderMap: Map<String, Int>): CustomResult<Unit, Exception>

    /**
     * 채널을 다른 카테고리로 이동
     * @param channelId 채널 ID
     * @param newCategoryId 새로운 카테고리 ID
     * @param newOrder 새로운 순서 (nullable)
     * @return 성공 여부
     */
    suspend fun moveChannelToCategory(
        channelId: String,
        newCategoryId: String,
        newOrder: ProjectChannelOrder? = null
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 채널 존재 여부 확인
     * @param channelId 채널 ID
     * @return 존재 여부
     */
    suspend fun channelExists(channelId: String): Boolean

    /**
     * 채널 이름 중복 확인
     * @param name 채널 이름
     * @param excludeChannelId 제외할 채널 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun nameExists(name: Name, excludeChannelId: String? = null): Boolean

    /**
     * 전체 채널 수 조회
     * @return 채널 수
     */
    suspend fun getTotalChannelCount(): Int

    /**
     * 특정 카테고리의 채널 수 조회
     * @param categoryId 카테고리 ID
     * @return 채널 수
     */
    suspend fun getChannelCountByCategory(categoryId: String): Int

    /**
     * 특정 채널 타입의 채널 수 조회
     * @param channelType 채널 타입
     * @return 채널 수
     */
    suspend fun getChannelCountByType(channelType: ProjectChannelType): Int

    /**
     * 특정 상태의 채널 수 조회
     * @param status 채널 상태
     * @return 채널 수
     */
    suspend fun getChannelCountByStatus(status: ProjectChannelStatus): Int

    /**
     * 카테고리 내 다음 채널 순서 조회
     * @param categoryId 카테고리 ID
     * @return 다음 순서 번호
     */
    suspend fun getNextChannelOrder(categoryId: String): Int

    /**
     * 모든 채널 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllChannels(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 채널 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 채널 목록
     */
    suspend fun getChannelsUpdatedAfter(timestamp: Instant): List<ProjectChannel>

}