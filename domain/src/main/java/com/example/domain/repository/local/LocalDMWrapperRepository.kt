package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local DM Wrapper Repository Interface (SSOT)
 * Room Database 전용 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote DMWrapperRepository 사용)
 *
 * ✅ 역할:
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - 로컬 CRUD 작업 (Insert/Update/Delete)
 * - 로컬 검색 및 필터링
 * - Outbox 관리 (동기화 대상 저장)
 */
interface LocalDMWrapperRepository {

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 DM Wrapper를 실시간 관찰
     * @param wrapperId DM Wrapper ID
     * @return DM Wrapper Flow (null 가능)
     */
    fun observeDMWrapperById(wrapperId: String): Flow<DMWrapper?>

    /**
     * 사용자의 모든 DM Wrapper를 실시간 관찰
     * @param currentUserId 현재 사용자 ID
     * @return DM Wrapper 목록 Flow
     */
    fun observeDMWrappersByUser(currentUserId: String): Flow<List<DMWrapper>>

    /**
     * 다른 사용자와의 DM Wrapper를 실시간 관찰
     * @param otherUserId 다른 사용자 ID
     * @return DM Wrapper Flow (null 가능)
     */
    fun observeDMWrapperByOtherUser(otherUserId: String): Flow<DMWrapper?>

    /**
     * 사용자 이름으로 DM Wrapper 검색을 실시간 관찰
     * @param userName 검색할 사용자 이름
     * @param limit 제한 개수
     * @return DM Wrapper 목록 Flow
     */
    fun observeDMWrappersByUserName(userName: String, limit: Int = 10): Flow<List<DMWrapper>>

    /**
     * 모든 DM Wrapper를 실시간 관찰
     * @return 전체 DM Wrapper 목록 Flow
     */
    fun observeAllDMWrappers(): Flow<List<DMWrapper>>

    /**
     * 특정 DM Wrapper의 updatedAt 필드 변경을 실시간 관찰
     * @param wrapperId DM Wrapper ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeDMWrapperUpdatedAt(wrapperId: String): Flow<Long?>

    /**
     * 최근 메시지가 있는 DM Wrapper들을 실시간 관찰
     * @return 최근 메시지가 있는 DM Wrapper 목록 Flow
     */
    fun observeDMWrappersWithRecentMessages(): Flow<List<DMWrapper>>

    /**
     * 주어진 ID 목록에 해당하는 DM Wrapper 목록을 실시간 관찰
     * @param wrapperIds DM Wrapper ID 목록
     * @return DM Wrapper 목록 Flow
     */
    fun observeDMWrappers(wrapperIds: List<String>): Flow<List<DMWrapper>>

    // === 단순 읽기 작업 ===

    /**
     * DM Wrapper ID로 조회
     * @param wrapperId DM Wrapper ID
     * @return DM Wrapper (없으면 null)
     */
    suspend fun getDMWrapperById(wrapperId: String): DMWrapper?

    /**
     * 사용자의 모든 DM Wrapper 조회
     * @param currentUserId 현재 사용자 ID
     * @return DM Wrapper 목록
     */
    suspend fun getDMWrappersByUser(currentUserId: String): List<DMWrapper>

    /**
     * 다른 사용자와의 DM Wrapper 조회
     * @param otherUserId 다른 사용자 ID
     * @return DM Wrapper (없으면 null)
     */
    suspend fun getDMWrapperByOtherUser(otherUserId: String): DMWrapper?

    /**
     * 사용자 이름으로 DM Wrapper 검색
     * @param userName 검색할 사용자 이름
     * @param limit 제한 개수
     * @return DM Wrapper 목록
     */
    suspend fun searchDMWrappersByUserName(userName: String, limit: Int = 10): List<DMWrapper>

    /**
     * 모든 DM Wrapper 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return DM Wrapper 목록
     */
    suspend fun getAllDMWrappers(limit: Int? = null): List<DMWrapper>

    /**
     * 여러 DM Wrapper ID로 조회
     * @param wrapperIds DM Wrapper ID 목록
     * @return DM Wrapper 목록
     */
    suspend fun getDMWrappersByIds(wrapperIds: List<String>): List<DMWrapper>

    /**
     * 최근 메시지가 있는 DM Wrapper들 조회
     * @return 최근 메시지가 있는 DM Wrapper 목록
     */
    suspend fun getDMWrappersWithRecentMessages(): List<DMWrapper>

    /**
     * 특정 사용자 ID들과의 DM Wrapper 조회
     * @param otherUserIds 다른 사용자 ID 목록
     * @return DM Wrapper 목록
     */
    suspend fun getDMWrappersByOtherUsers(otherUserIds: List<String>): List<DMWrapper>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * DM Wrapper 저장 (생성/수정)
     * @param dmWrapper 저장할 DM Wrapper
     * @return 성공 여부
     */
    suspend fun saveDMWrapper(dmWrapper: DMWrapper): CustomResult<Unit, Exception>

    /**
     * DM Wrapper 대량 저장 (동기화용)
     * @param dmWrappers 저장할 DM Wrapper 목록
     * @return 성공 여부
     */
    suspend fun saveDMWrappers(dmWrappers: List<DMWrapper>): CustomResult<Unit, Exception>

    /**
     * DM Wrapper 삭제 (Soft Delete)
     * @param wrapperId DM Wrapper ID
     * @return 성공 여부
     */
    suspend fun deleteDMWrapper(wrapperId: String): CustomResult<Unit, Exception>

    /**
     * 사용자별 DM Wrapper 일괄 삭제
     * @param currentUserId 현재 사용자 ID
     * @return 성공 여부
     */
    suspend fun deleteDMWrappersByUser(currentUserId: String): CustomResult<Unit, Exception>

    /**
     * DM Wrapper 정보 업데이트 (로컬)
     * @param wrapperId DM Wrapper ID
     * @param otherUserName 새로운 사용자 이름 (nullable)
     * @param otherUserImageUrl 새로운 사용자 이미지 URL (nullable)
     * @param lastMessagePreview 새로운 마지막 메시지 미리보기 (nullable)
     * @return 성공 여부
     */
    suspend fun updateDMWrapper(
        wrapperId: String,
        otherUserName: UserName? = null,
        otherUserImageUrl: ImageUrl? = null,
        lastMessagePreview: DMChannelLastMessagePreview? = null
    ): CustomResult<Unit, Exception>

    /**
     * DM Wrapper의 다른 사용자 정보 업데이트
     * @param wrapperId DM Wrapper ID
     * @param newOtherUserId 새로운 다른 사용자 ID
     * @return 성공 여부
     */
    suspend fun updateDMWrapperOtherUser(
        wrapperId: String,
        newOtherUserId: UserId
    ): CustomResult<Unit, Exception>

    /**
     * DM Wrapper의 마지막 메시지 미리보기 업데이트
     * @param wrapperId DM Wrapper ID
     * @param lastMessagePreview 새로운 마지막 메시지 미리보기
     * @return 성공 여부
     */
    suspend fun updateLastMessagePreview(
        wrapperId: String,
        lastMessagePreview: DMChannelLastMessagePreview?
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * DM Wrapper 존재 여부 확인
     * @param wrapperId DM Wrapper ID
     * @return 존재 여부
     */
    suspend fun dmWrapperExists(wrapperId: String): Boolean

    /**
     * 다른 사용자와의 DM Wrapper 존재 여부 확인
     * @param otherUserId 다른 사용자 ID
     * @return 존재 여부
     */
    suspend fun dmWrapperExistsWithOtherUser(otherUserId: String): Boolean

    /**
     * 사용자별 DM Wrapper 수 조회
     * @param currentUserId 현재 사용자 ID
     * @return DM Wrapper 수
     */
    suspend fun getDMWrapperCountByUser(currentUserId: String): Int

    /**
     * 전체 DM Wrapper 수 조회
     * @return DM Wrapper 수
     */
    suspend fun getTotalDMWrapperCount(): Int

    /**
     * 최근 메시지가 있는 DM Wrapper 수 조회
     * @return 최근 메시지가 있는 DM Wrapper 수
     */
    suspend fun getDMWrapperCountWithRecentMessages(): Int

    /**
     * 모든 DM Wrapper 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllDMWrappers(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 DM Wrapper 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 DM Wrapper 목록
     */
    suspend fun getDMWrappersUpdatedAfter(timestamp: Instant): List<DMWrapper>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param wrapperId DM Wrapper ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        wrapperId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}