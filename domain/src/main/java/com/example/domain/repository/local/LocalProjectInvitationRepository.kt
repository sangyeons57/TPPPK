package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Project Invitation Repository Interface (SSOT)
 * Room Database 전용 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemoteProjectInvitationRepository 사용)
 *
 * ✅ 역할:
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - 로컬 CRUD 작업 (Insert/Update/Delete)
 * - 로컬 검색 및 필터링
 * - Outbox 관리 (동기화 대상 저장)
 */
interface LocalProjectInvitationRepository {

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 초대를 실시간 관찰
     * @param invitationId 초대 ID
     * @return 초대 Flow (null 가능)
     */
    fun observeInvitationById(invitationId: String): Flow<ProjectInvitation?>

    /**
     * 초대 코드로 특정 초대를 실시간 관찰
     * @param inviteCode 초대 코드
     * @return 초대 Flow (null 가능)
     */
    fun observeInvitationByCode(inviteCode: InviteCode): Flow<ProjectInvitation?>

    /**
     * 특정 프로젝트의 모든 초대를 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 초대 목록 Flow
     */
    fun observeInvitationsByProject(projectId: String): Flow<List<ProjectInvitation>>

    /**
     * 특정 사용자가 생성한 초대들을 실시간 관찰
     * @param inviterId 초대자 ID
     * @return 초대 목록 Flow
     */
    fun observeInvitationsByInviter(inviterId: UserId): Flow<List<ProjectInvitation>>

    /**
     * 특정 상태의 초대들을 실시간 관찰
     * @param status 초대 상태
     * @return 초대 목록 Flow
     */
    fun observeInvitationsByStatus(status: InviteStatus): Flow<List<ProjectInvitation>>

    /**
     * 주어진 ID 목록에 해당하는 초대 목록을 실시간 관찰
     * @param invitationIds 초대 ID 목록
     * @return 초대 목록 Flow
     */
    fun observeInvitations(invitationIds: List<String>): Flow<List<ProjectInvitation>>

    /**
     * 특정 초대의 updatedAt 필드 변경을 실시간 관찰
     * @param invitationId 초대 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeInvitationUpdatedAt(invitationId: String): Flow<Long?>

    /**
     * 모든 초대를 실시간 관찰
     * @return 전체 초대 목록 Flow
     */
    fun observeAllInvitations(): Flow<List<ProjectInvitation>>

    /**
     * 활성 초대들을 실시간 관찰
     * @return 활성 초대 목록 Flow
     */
    fun observeActiveInvitations(): Flow<List<ProjectInvitation>>

    /**
     * 만료된 초대들을 실시간 관찰
     * @return 만료된 초대 목록 Flow
     */
    fun observeExpiredInvitations(): Flow<List<ProjectInvitation>>

    // === 단순 읽기 작업 ===

    /**
     * 초대 ID로 조회
     * @param invitationId 초대 ID
     * @return 초대 (없으면 null)
     */
    suspend fun getInvitationById(invitationId: String): ProjectInvitation?

    /**
     * 초대 코드로 조회
     * @param inviteCode 초대 코드
     * @return 초대 (없으면 null)
     */
    suspend fun getInvitationByCode(inviteCode: InviteCode): ProjectInvitation?

    /**
     * 특정 프로젝트의 모든 초대 조회
     * @param projectId 프로젝트 ID
     * @return 초대 목록
     */
    suspend fun getInvitationsByProject(projectId: String): List<ProjectInvitation>

    /**
     * 특정 사용자가 생성한 초대들 조회
     * @param inviterId 초대자 ID
     * @return 초대 목록
     */
    suspend fun getInvitationsByInviter(inviterId: UserId): List<ProjectInvitation>

    /**
     * 특정 상태의 초대들 조회
     * @param status 초대 상태
     * @return 초대 목록
     */
    suspend fun getInvitationsByStatus(status: InviteStatus): List<ProjectInvitation>

    /**
     * 여러 초대 ID로 조회
     * @param invitationIds 초대 ID 목록
     * @return 초대 목록
     */
    suspend fun getInvitationsByIds(invitationIds: List<String>): List<ProjectInvitation>

    /**
     * 전체 초대 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 초대 목록
     */
    suspend fun getAllInvitations(limit: Int? = null): List<ProjectInvitation>

    /**
     * 활성 초대들 조회
     * @return 활성 초대 목록
     */
    suspend fun getActiveInvitations(): List<ProjectInvitation>

    /**
     * 만료된 초대들 조회
     * @return 만료된 초대 목록
     */
    suspend fun getExpiredInvitations(): List<ProjectInvitation>

    /**
     * 특정 시간 이전에 만료되는 초대들 조회
     * @param beforeTime 기준 시간
     * @return 만료 예정 초대 목록
     */
    suspend fun getInvitationsExpiringBefore(beforeTime: Instant): List<ProjectInvitation>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 초대 저장 (생성/수정)
     * @param invitation 저장할 초대
     * @return 성공 여부
     */
    suspend fun saveInvitation(invitation: ProjectInvitation): CustomResult<Unit, Exception>

    /**
     * 초대 대량 저장 (동기화용)
     * @param invitations 저장할 초대 목록
     * @return 성공 여부
     */
    suspend fun saveInvitations(invitations: List<ProjectInvitation>): CustomResult<Unit, Exception>

    /**
     * 초대 삭제 (Soft Delete)
     * @param invitationId 초대 ID
     * @return 성공 여부
     */
    suspend fun deleteInvitation(invitationId: String): CustomResult<Unit, Exception>

    /**
     * 초대 상태 업데이트 (로컬)
     * @param invitationId 초대 ID
     * @param status 새로운 상태
     * @return 성공 여부
     */
    suspend fun updateInvitationStatus(
        invitationId: String,
        status: InviteStatus
    ): CustomResult<Unit, Exception>

    /**
     * 초대 만료 처리
     * @param invitationId 초대 ID
     * @return 성공 여부
     */
    suspend fun expireInvitation(invitationId: String): CustomResult<Unit, Exception>

    /**
     * 초대 취소 처리
     * @param invitationId 초대 ID
     * @return 성공 여부
     */
    suspend fun revokeInvitation(invitationId: String): CustomResult<Unit, Exception>

    /**
     * 초대 수락 처리
     * @param invitationId 초대 ID
     * @return 성공 여부
     */
    suspend fun acceptInvitation(invitationId: String): CustomResult<Unit, Exception>

    /**
     * 만료된 초대들 일괄 정리
     * @return 정리된 초대 수
     */
    suspend fun cleanupExpiredInvitations(): CustomResult<Int, Exception>

    // === 유틸리티 ===

    /**
     * 초대 존재 여부 확인
     * @param invitationId 초대 ID
     * @return 존재 여부
     */
    suspend fun invitationExists(invitationId: String): Boolean

    /**
     * 초대 코드 존재 여부 확인
     * @param inviteCode 초대 코드
     * @return 존재 여부
     */
    suspend fun inviteCodeExists(inviteCode: InviteCode): Boolean

    /**
     * 초대가 활성 상태인지 확인
     * @param invitationId 초대 ID
     * @return 활성 여부
     */
    suspend fun isInvitationActive(invitationId: String): Boolean

    /**
     * 초대가 사용 가능한지 확인
     * @param invitationId 초대 ID
     * @return 사용 가능 여부
     */
    suspend fun canInvitationBeUsed(invitationId: String): Boolean

    /**
     * 초대가 만료되었는지 확인
     * @param invitationId 초대 ID
     * @return 만료 여부
     */
    suspend fun isInvitationExpired(invitationId: String): Boolean

    /**
     * 전체 초대 수 조회
     * @return 초대 수
     */
    suspend fun getTotalInvitationCount(): Int

    /**
     * 특정 프로젝트의 초대 수 조회
     * @param projectId 프로젝트 ID
     * @return 초대 수
     */
    suspend fun getInvitationCountByProject(projectId: String): Int

    /**
     * 특정 상태의 초대 수 조회
     * @param status 초대 상태
     * @return 초대 수
     */
    suspend fun getInvitationCountByStatus(status: InviteStatus): Int

    /**
     * 활성 초대 수 조회
     * @return 활성 초대 수
     */
    suspend fun getActiveInvitationCount(): Int

    /**
     * 모든 초대 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllInvitations(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 초대 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 초대 목록
     */
    suspend fun getInvitationsUpdatedAfter(timestamp: Instant): List<ProjectInvitation>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param invitationId 초대 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        invitationId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}