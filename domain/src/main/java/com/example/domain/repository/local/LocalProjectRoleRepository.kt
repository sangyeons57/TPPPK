package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Project Role Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemoteRoleRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Role 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeRoleById
 * - observeAllEntities -> observeAllRoles
 * - observeEntityUpdatedAt -> observeRoleUpdatedAt
 * - getEntityById -> getRoleById
 * - getEntitiesByIds -> getRolesByIds
 * - getAllEntities -> getAllRoles
 * - saveEntity -> saveRole
 * - saveEntities -> saveRoles
 * - deleteEntity -> deleteRole
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalProjectRoleRepository : BaseLocalRepository<Role> {

    // === BaseLocalRepository 메서드 (구현체에서 역할 전용 메서드로 매핑) ===
    // observeEntityById -> observeRoleById
    // observeAllEntities -> observeAllRoles  
    // observeEntityUpdatedAt -> observeRoleUpdatedAt
    // getEntityById -> getRoleById
    // getEntitiesByIds -> getRolesByIds
    // getAllEntities -> getAllRoles
    // saveEntity -> saveRole
    // saveEntities -> saveRoles
    // deleteEntity -> deleteRole
    // getEntitiesUpdatedAfter -> getRolesUpdatedAfter
    // clearAllEntities -> clearAllRoles
    // getTotalEntityCount -> getTotalRoleCount
    // entityExists -> roleExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 역할을 실시간 관찰
     * @param roleId 역할 ID
     * @return 역할 Flow (null 가능)
     */
    fun observeRoleById(roleId: String): Flow<Role?>

    /**
     * 특정 프로젝트의 모든 역할을 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 역할 목록 Flow
     */
    fun observeRolesByProject(projectId: String): Flow<List<Role>>

    /**
     * 주어진 이름과 정확히 일치하는 역할을 실시간 관찰
     * @param name 역할 이름
     * @return 역할 Flow
     */
    fun observeByName(name: Name): Flow<Role?>

    /**
     * 주어진 이름을 포함하는 역할 목록을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 역할 목록 Flow
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<List<Role>>

    /**
     * 주어진 ID 목록에 해당하는 역할 목록을 실시간 관찰
     * @param roleIds 역할 ID 목록
     * @return 역할 목록 Flow
     */
    fun observeRoles(roleIds: List<String>): Flow<List<Role>>

    /**
     * 특정 역할의 updatedAt 필드 변경을 실시간 관찰
     * @param roleId 역할 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeRoleUpdatedAt(roleId: String): Flow<Long?>

    /**
     * 모든 역할을 실시간 관찰
     * @return 전체 역할 목록 Flow
     */
    fun observeAllRoles(): Flow<List<Role>>

    /**
     * 기본 역할들을 실시간 관찰
     * @return 기본 역할 목록 Flow
     */
    fun observeDefaultRoles(): Flow<List<Role>>

    /**
     * 시스템 역할들을 실시간 관찰
     * @return 시스템 역할 목록 Flow
     */
    fun observeSystemRoles(): Flow<List<Role>>

    // === 단순 읽기 작업 ===

    /**
     * 역할 ID로 조회
     * @param roleId 역할 ID
     * @return 역할 (없으면 null)
     */
    suspend fun getRoleById(roleId: String): Role?

    /**
     * 특정 프로젝트의 모든 역할 조회
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    suspend fun getRolesByProject(projectId: String): List<Role>

    /**
     * 역할 이름으로 조회 (정확히 일치)
     * @param name 역할 이름
     * @return 역할 (없으면 null)
     */
    suspend fun getRoleByName(name: Name): Role?

    /**
     * 역할 이름으로 검색 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 역할 목록
     */
    suspend fun searchRolesByName(name: String, limit: Int = 10): List<Role>

    /**
     * 여러 역할 ID로 조회
     * @param roleIds 역할 ID 목록
     * @return 역할 목록
     */
    suspend fun getRolesByIds(roleIds: List<String>): List<Role>

    /**
     * 전체 역할 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 역할 목록
     */
    suspend fun getAllRoles(limit: Int? = null): List<Role>

    /**
     * 기본 역할들 조회
     * @return 기본 역할 목록
     */
    suspend fun getDefaultRoles(): List<Role>

    /**
     * 시스템 역할들 조회
     * @return 시스템 역할 목록
     */
    suspend fun getSystemRoles(): List<Role>

    /**
     * OWNER 역할 조회
     * @return OWNER 역할 (없으면 null)
     */
    suspend fun getOwnerRole(): Role?

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 역할 저장 (생성/수정)
     * @param role 저장할 역할
     * @return 성공 여부
     */
    suspend fun saveRole(role: Role): CustomResult<Unit, Exception>

    /**
     * 역할 대량 저장 (동기화용)
     * @param roles 저장할 역할 목록
     * @return 성공 여부
     */
    suspend fun saveRoles(roles: List<Role>): CustomResult<Unit, Exception>

    /**
     * 역할 삭제 (Soft Delete)
     * @param roleId 역할 ID
     * @return 성공 여부
     */
    suspend fun deleteRole(roleId: String): CustomResult<Unit, Exception>

    /**
     * 역할 이름 업데이트 (로컬)
     * @param roleId 역할 ID
     * @param name 새로운 이름
     * @return 성공 여부
     */
    suspend fun updateRoleName(
        roleId: String,
        name: Name
    ): CustomResult<Unit, Exception>

    /**
     * 역할 기본 상태 업데이트 (로컬)
     * @param roleId 역할 ID
     * @param isDefault 기본 역할 여부
     * @return 성공 여부
     */
    suspend fun updateRoleDefaultStatus(
        roleId: String,
        isDefault: RoleIsDefault
    ): CustomResult<Unit, Exception>

    /**
     * OWNER 역할 생성
     * @return 성공 여부
     */
    suspend fun createOwnerRole(): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 역할 존재 여부 확인
     * @param roleId 역할 ID
     * @return 존재 여부
     */
    suspend fun roleExists(roleId: String): Boolean

    /**
     * 역할 이름 중복 확인
     * @param name 역할 이름
     * @param excludeRoleId 제외할 역할 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun nameExists(name: Name, excludeRoleId: String? = null): Boolean

    /**
     * 시스템 역할인지 확인
     * @param roleId 역할 ID
     * @return 시스템 역할 여부
     */
    suspend fun isSystemRole(roleId: String): Boolean

    /**
     * 기본 역할인지 확인
     * @param roleId 역할 ID
     * @return 기본 역할 여부
     */
    suspend fun isDefaultRole(roleId: String): Boolean

    /**
     * 전체 역할 수 조회
     * @return 역할 수
     */
    suspend fun getTotalRoleCount(): Int

    /**
     * 특정 프로젝트의 역할 수 조회
     * @param projectId 프로젝트 ID
     * @return 역할 수
     */
    suspend fun getRoleCountByProject(projectId: String): Int

    /**
     * 기본 역할 수 조회
     * @return 기본 역할 수
     */
    suspend fun getDefaultRoleCount(): Int

    /**
     * 시스템 역할 수 조회
     * @return 시스템 역할 수
     */
    suspend fun getSystemRoleCount(): Int

    /**
     * 모든 역할 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllRoles(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 역할 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 역할 목록
     */
    suspend fun getRolesUpdatedAfter(timestamp: Instant): List<Role>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param roleId 역할 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        roleId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}