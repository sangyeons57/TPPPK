package com.example.data_core.datasource.local

import com.example.domain.model.base.DMWrapper
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 DM 래퍼 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalDMWrapperDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * DM 래퍼 ID로 단일 DM 래퍼 조회
     * @param wrapperId DM 래퍼 ID
     * @return DM 래퍼 정보 (없으면 null)
     */
    suspend fun getDMWrapperById(wrapperId: String): DMWrapper?

    /**
     * 사용자별 DM 래퍼 목록 조회
     * @param userId 사용자 ID
     * @return DM 래퍼 목록
     */
    suspend fun getDMWrappersByUser(userId: String): List<DMWrapper>

    /**
     * DM 채널별 래퍼 조회
     * @param dmChannelId DM 채널 ID
     * @return DM 래퍼 목록
     */
    suspend fun getDMWrappersByChannel(dmChannelId: String): List<DMWrapper>

    /**
     * 사용자의 특정 DM 채널 래퍼 조회
     * @param userId 사용자 ID
     * @param dmChannelId DM 채널 ID
     * @return DM 래퍼 정보 (없으면 null)
     */
    suspend fun getDMWrapperByUserAndChannel(userId: String, dmChannelId: String): DMWrapper?

    /**
     * 사용자별 읽지 않은 DM 래퍼 조회
     * @param userId 사용자 ID
     * @return 읽지 않은 DM 래퍼 목록
     */
    suspend fun getUnreadDMWrappersByUser(userId: String): List<DMWrapper>

    /**
     * 모든 DM 래퍼 목록 조회
     * @return DM 래퍼 목록
     */
    suspend fun getAllDMWrappers(): List<DMWrapper>

    /**
     * 단일 DM 래퍼 저장
     * @param dmWrapper 저장할 DM 래퍼
     */
    suspend fun saveDMWrapper(dmWrapper: DMWrapper)

    /**
     * DM 래퍼 목록 배치 저장
     * @param dmWrappers 저장할 DM 래퍼 목록
     */
    suspend fun saveDMWrappers(dmWrappers: List<DMWrapper>)

    /**
     * DM 래퍼 삭제
     * @param wrapperId 삭제할 DM 래퍼 ID
     */
    suspend fun deleteDMWrapper(wrapperId: String)

    /**
     * 사용자별 DM 래퍼 일괄 삭제
     * @param userId 사용자 ID
     */
    suspend fun deleteDMWrappersByUser(userId: String)

    /**
     * DM 채널별 래퍼 일괄 삭제
     * @param dmChannelId DM 채널 ID
     */
    suspend fun deleteDMWrappersByChannel(dmChannelId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 DM 래퍼들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 DM 래퍼 목록
     */
    suspend fun getDMWrappersUpdatedAfter(timestamp: Instant): List<DMWrapper>

    /**
     * 사용자별 DM 래퍼 실시간 관찰
     * @param userId 사용자 ID
     * @return DM 래퍼 목록 Flow
     */
    fun observeDMWrappersByUser(userId: String): Flow<List<DMWrapper>>

    /**
     * 특정 DM 래퍼 실시간 관찰
     * @param wrapperId DM 래퍼 ID
     * @return DM 래퍼 정보 Flow
     */
    fun observeDMWrapperById(wrapperId: String): Flow<DMWrapper?>

    /**
     * 사용자별 읽지 않은 DM 래퍼 실시간 관찰
     * @param userId 사용자 ID
     * @return 읽지 않은 DM 래퍼 목록 Flow
     */
    fun observeUnreadDMWrappersByUser(userId: String): Flow<List<DMWrapper>>

    /**
     * 사용자 이름으로 DM 래퍼 검색을 실시간 관찰
     * @param userName 검색할 사용자 이름
     * @param limit 제한 개수
     * @return DM 래퍼 목록 Flow
     */
    fun observeDMWrappersByUserName(userName: String, limit: Int = 10): Flow<List<DMWrapper>>

    /**
     * 최근 메시지가 있는 DM 래퍼들을 실시간 관찰
     * @return 최근 메시지가 있는 DM 래퍼 목록 Flow
     */
    fun observeDMWrappersWithRecentMessages(): Flow<List<DMWrapper>>

    /**
     * 주어진 ID 목록에 해당하는 DM 래퍼 목록을 실시간 관찰
     * @param wrapperIds DM 래퍼 ID 목록
     * @return DM 래퍼 목록 Flow
     */
    fun observeDMWrappers(wrapperIds: List<String>): Flow<List<DMWrapper>>

    /**
     * 사용자 이름으로 DM 래퍼 검색
     * @param userName 검색할 사용자 이름
     * @param limit 제한 개수
     * @return DM 래퍼 목록
     */
    suspend fun searchDMWrappersByUserName(userName: String, limit: Int = 10): List<DMWrapper>

    /**
     * 여러 DM 래퍼 ID로 조회
     * @param wrapperIds DM 래퍼 ID 목록
     * @return DM 래퍼 목록
     */
    suspend fun getDMWrappersByIds(wrapperIds: List<String>): List<DMWrapper>

    /**
     * 최근 메시지가 있는 DM 래퍼들 조회
     * @return 최근 메시지가 있는 DM 래퍼 목록
     */
    suspend fun getDMWrappersWithRecentMessages(): List<DMWrapper>

    /**
     * 특정 사용자 ID들과의 DM 래퍼 조회
     * @param otherUserIds 다른 사용자 ID 목록
     * @return DM 래퍼 목록
     */
    suspend fun getDMWrappersByOtherUsers(otherUserIds: List<String>): List<DMWrapper>

    /**
     * 다른 사용자와의 DM 래퍼 존재 여부 확인
     * @param otherUserId 다른 사용자 ID
     * @return 존재 여부
     */
    suspend fun dmWrapperExistsWithOtherUser(otherUserId: String): Boolean

    /**
     * 최근 메시지가 있는 DM 래퍼 수 조회
     * @return 최근 메시지가 있는 DM 래퍼 수
     */
    suspend fun getDMWrapperCountWithRecentMessages(): Int

    // === Outbox 관리 ===

    /**
     * DM 래퍼 변경사항을 Outbox에 기록
     * @param wrapperId DM 래퍼 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(wrapperId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<DMWrapperOutboxOperation>

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
     * DM 래퍼 존재 여부 확인
     * @param wrapperId DM 래퍼 ID
     * @return 존재 여부
     */
    suspend fun dmWrapperExists(wrapperId: String): Boolean

    /**
     * 사용자별 DM 래퍼 수 조회
     * @param userId 사용자 ID
     * @return DM 래퍼 수
     */
    suspend fun getDMWrapperCount(userId: String): Int

    /**
     * 전체 DM 래퍼 수 조회
     * @return 전체 DM 래퍼 수
     */
    suspend fun getTotalDMWrapperCount(): Int

    /**
     * 사용자별 읽지 않은 DM 래퍼 수 조회
     * @param userId 사용자 ID
     * @return 읽지 않은 DM 래퍼 수
     */
    suspend fun getUnreadDMWrapperCount(userId: String): Int

    /**
     * DM 래퍼 읽음 상태 업데이트
     * @param wrapperId DM 래퍼 ID
     * @param lastReadAt 마지막 읽은 시간
     */
    suspend fun updateLastReadAt(wrapperId: String, lastReadAt: Instant)

    /**
     * 모든 DM 래퍼 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllDMWrappers()
}

/**
 * DM 래퍼 Outbox 작업 정보
 */
data class DMWrapperOutboxOperation(
    val id: String,
    val wrapperId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)