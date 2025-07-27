package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Member Repository Interface (SSOT)
 * Room Database 전용 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemoteMemberRepository 사용)
 *
 * ✅ 역할:
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - 로컬 CRUD 작업 (Insert/Update/Delete)
 * - 로컬 검색 및 필터링
 * - Outbox 관리 (동기화 대상 저장)
 */
interface LocalMemberRepository {

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 멤버를 실시간 관찰
     * @param memberId 멤버 ID
     * @return 멤버 Flow (null 가능)
     */
    fun observeMemberById(memberId: String): Flow<Member?>

    /**
     * 특정 프로젝트의 모든 멤버를 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 멤버 목록 Flow
     */
    fun observeMembersByProject(projectId: String): Flow<List<Member>>

    /**
     * 프로젝트에서 특정 사용자의 멤버십을 실시간 관찰
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 멤버 Flow (null 가능)
     */
    fun observeMemberByProjectAndUser(projectId: String, userId: String): Flow<Member?>

    /**
     * 주어진 ID 목록에 해당하는 멤버 목록을 실시간 관찰
     * @param memberIds 멤버 ID 목록
     * @return 멤버 목록 Flow
     */
    fun observeMembers(memberIds: List<String>): Flow<List<Member>>

    /**
     * 특정 역할을 가진 멤버들을 실시간 관찰
     * @param roleId 역할 ID
     * @return 멤버 목록 Flow
     */
    fun observeMembersByRole(roleId: String): Flow<List<Member>>

    /**
     * 특정 멤버의 updatedAt 필드 변경을 실시간 관찰
     * @param memberId 멤버 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeMemberUpdatedAt(memberId: String): Flow<Long?>

    /**
     * 모든 멤버를 실시간 관찰
     * @return 전체 멤버 목록 Flow
     */
    fun observeAllMembers(): Flow<List<Member>>

    // === 단순 읽기 작업 ===

    /**
     * 멤버 ID로 조회
     * @param memberId 멤버 ID
     * @return 멤버 (없으면 null)
     */
    suspend fun getMemberById(memberId: String): Member?

    /**
     * 특정 프로젝트의 모든 멤버 조회
     * @param projectId 프로젝트 ID
     * @return 멤버 목록
     */
    suspend fun getMembersByProject(projectId: String): List<Member>

    /**
     * 프로젝트에서 특정 사용자의 멤버십 조회
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 멤버 (없으면 null)
     */
    suspend fun getMemberByProjectAndUser(projectId: String, userId: String): Member?

    /**
     * 여러 멤버 ID로 조회
     * @param memberIds 멤버 ID 목록
     * @return 멤버 목록
     */
    suspend fun getMembersByIds(memberIds: List<String>): List<Member>

    /**
     * 전체 멤버 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 멤버 목록
     */
    suspend fun getAllMembers(limit: Int? = null): List<Member>

    /**
     * 특정 역할을 가진 멤버들 조회
     * @param roleId 역할 ID
     * @return 멤버 목록
     */
    suspend fun getMembersByRole(roleId: String): List<Member>

    /**
     * 여러 역할 중 하나라도 가진 멤버들 조회
     * @param roleIds 역할 ID 목록
     * @return 멤버 목록
     */
    suspend fun getMembersByRoles(roleIds: List<String>): List<Member>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 멤버 저장 (생성/수정)
     * @param member 저장할 멤버
     * @return 성공 여부
     */
    suspend fun saveMember(member: Member): CustomResult<Unit, Exception>

    /**
     * 멤버 대량 저장 (동기화용)
     * @param members 저장할 멤버 목록
     * @return 성공 여부
     */
    suspend fun saveMembers(members: List<Member>): CustomResult<Unit, Exception>

    /**
     * 멤버 삭제 (Soft Delete)
     * @param memberId 멤버 ID
     * @return 성공 여부
     */
    suspend fun deleteMember(memberId: String): CustomResult<Unit, Exception>

    /**
     * 멤버의 역할 업데이트 (로컬)
     * @param memberId 멤버 ID
     * @param roleIds 새로운 역할 ID 목록
     * @return 성공 여부
     */
    suspend fun updateMemberRoles(
        memberId: String,
        roleIds: List<DocumentId>
    ): CustomResult<Unit, Exception>

    /**
     * 멤버에게 역할 추가
     * @param memberId 멤버 ID
     * @param roleId 추가할 역할 ID
     * @return 성공 여부
     */
    suspend fun assignRole(
        memberId: String,
        roleId: DocumentId
    ): CustomResult<Unit, Exception>

    /**
     * 멤버에게서 역할 제거
     * @param memberId 멤버 ID
     * @param roleId 제거할 역할 ID
     * @return 성공 여부
     */
    suspend fun revokeRole(
        memberId: String,
        roleId: DocumentId
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 멤버 존재 여부 확인
     * @param memberId 멤버 ID
     * @return 존재 여부
     */
    suspend fun memberExists(memberId: String): Boolean

    /**
     * 사용자가 특정 프로젝트의 멤버인지 확인
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 멤버 여부
     */
    suspend fun isUserMemberOfProject(projectId: String, userId: String): Boolean

    /**
     * 멤버가 특정 역할을 가지고 있는지 확인
     * @param memberId 멤버 ID
     * @param roleId 역할 ID
     * @return 역할 보유 여부
     */
    suspend fun memberHasRole(memberId: String, roleId: String): Boolean

    /**
     * 전체 멤버 수 조회
     * @return 멤버 수
     */
    suspend fun getTotalMemberCount(): Int

    /**
     * 특정 프로젝트의 멤버 수 조회
     * @param projectId 프로젝트 ID
     * @return 멤버 수
     */
    suspend fun getMemberCountByProject(projectId: String): Int

    /**
     * 특정 역할을 가진 멤버 수 조회
     * @param roleId 역할 ID
     * @return 멤버 수
     */
    suspend fun getMemberCountByRole(roleId: String): Int

    /**
     * 모든 멤버 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllMembers(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 멤버 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 멤버 목록
     */
    suspend fun getMembersUpdatedAfter(timestamp: Instant): List<Member>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param memberId 멤버 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        memberId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}