package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Message Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemoteMessageRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Message 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Paging 지원 (대량 메시지 처리)
 * - 로컬 필터링/정렬
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeMessageById
 * - observeAllEntities -> observeAllMessages
 * - observeEntityUpdatedAt -> observeMessageUpdatedAt
 * - getEntityById -> getMessageById
 * - getEntitiesByIds -> getMessagesByIds
 * - getAllEntities -> getAllMessages
 * - saveEntity -> saveMessage
 * - saveEntities -> saveMessages
 * - deleteEntity -> deleteMessage
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalMessageRepository : BaseLocalRepository<Message> {

    // === BaseLocalRepository 메서드 (구현체에서 메시지 전용 메서드로 매핑) ===
    // observeEntityById -> observeMessageById
    // observeAllEntities -> observeAllMessages  
    // observeEntityUpdatedAt -> observeMessageUpdatedAt
    // getEntityById -> getMessageById
    // getEntitiesByIds -> getMessagesByIds
    // getAllEntities -> getAllMessages
    // saveEntity -> saveMessage
    // saveEntities -> saveMessages
    // deleteEntity -> deleteMessage
    // getEntitiesUpdatedAfter -> getMessagesUpdatedAfter
    // clearAllEntities -> clearAllMessages
    // getTotalEntityCount -> getTotalMessageCount
    // entityExists -> messageExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 채널의 메시지를 실시간 관찰
     * @param channelId 채널 ID
     * @return 메시지 목록 Flow
     */
    fun observeMessagesByChannel(channelId: String): Flow<List<Message>>

    /**
     * 특정 메시지를 실시간 관찰
     * @param messageId 메시지 ID
     * @return 메시지 Flow (null 가능)
     */
    fun observeMessageById(messageId: String): Flow<Message?>

    /**
     * 읽지 않은 메시지 수 실시간 관찰
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @param lastReadTimestamp 마지막 읽음 시간
     * @return 읽지 않은 메시지 수 Flow
     */
    fun observeUnreadMessageCount(
        channelId: String,
        userId: String,
        lastReadTimestamp: Instant
    ): Flow<Int>

    // === 단순 읽기 작업 ===

    /**
     * 메시지 ID로 조회
     * @param messageId 메시지 ID
     * @return 메시지 (없으면 null)
     */
    suspend fun getMessageById(messageId: String): Message?

    /**
     * 채널의 메시지 목록 조회 (페이징 지원)
     * @param channelId 채널 ID
     * @param limit 제한 개수 (null이면 전체)
     * @return 메시지 목록
     */
    suspend fun getMessagesByChannel(channelId: String, limit: Int? = null): List<Message>

    /**
     * 메시지 타입별 조회
     * @param messageType 메시지 타입
     * @return 메시지 목록
     */
    suspend fun getMessagesByType(messageType: String): List<Message>

    /**
     * 사용자별 메시지 조회
     * @param userId 사용자 ID
     * @return 메시지 목록
     */
    suspend fun getMessagesByUser(userId: String): List<Message>

    /**
     * 메시지 검색
     * @param query 검색어
     * @param channelId 채널 ID (null이면 전체 검색)
     * @return 검색된 메시지 목록
     */
    suspend fun searchMessages(query: String, channelId: String? = null): List<Message>

    /**
     * 시간 범위별 메시지 조회
     * @param channelId 채널 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메시지 목록
     */
    suspend fun getMessagesByTimeRange(
        channelId: String,
        startTime: Instant,
        endTime: Instant
    ): List<Message>

    /**
     * 채널의 마지막 메시지 조회
     * @param channelId 채널 ID
     * @return 마지막 메시지 (없으면 null)
     */
    suspend fun getLastMessage(channelId: String): Message?

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 메시지 저장 (생성/수정)
     * @param message 저장할 메시지
     * @return 성공 여부
     */
    suspend fun saveMessage(message: Message): CustomResult<Unit, Exception>

    /**
     * 메시지 대량 저장 (동기화용)
     * @param messages 저장할 메시지 목록
     * @return 성공 여부
     */
    suspend fun saveMessages(messages: List<Message>): CustomResult<Unit, Exception>

    /**
     * 메시지 삭제 (Soft Delete)
     * @param messageId 메시지 ID
     * @return 성공 여부
     */
    suspend fun deleteMessage(messageId: String): CustomResult<Unit, Exception>

    /**
     * 채널의 모든 메시지 삭제
     * @param channelId 채널 ID
     * @return 성공 여부
     */
    suspend fun deleteMessagesByChannel(channelId: String): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 메시지 존재 여부 확인
     * @param messageId 메시지 ID
     * @return 존재 여부
     */
    suspend fun messageExists(messageId: String): Boolean

    /**
     * 채널의 메시지 수 조회
     * @param channelId 채널 ID
     * @return 메시지 수
     */
    suspend fun getMessageCount(channelId: String): Int

    /**
     * 전체 메시지 수 조회
     * @return 전체 메시지 수
     */
    suspend fun getTotalMessageCount(): Int

    /**
     * 읽지 않은 메시지 수 조회
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @param lastReadTimestamp 마지막 읽음 시간
     * @return 읽지 않은 메시지 수
     */
    suspend fun getUnreadMessageCount(
        channelId: String,
        userId: String,
        lastReadTimestamp: Instant
    ): Int

    /**
     * 모든 메시지 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllMessages(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 메시지 조회
     * @param timestamp 기준 시간
     * @param channelId 채널 ID (null이면 전체)
     * @return 업데이트된 메시지 목록
     */
    suspend fun getMessagesUpdatedAfter(
        timestamp: Instant,
        channelId: String? = null
    ): List<Message>

}