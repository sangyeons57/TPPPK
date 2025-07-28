package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Permission Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemotePermissionRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Permission 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observePermissionById
 * - observeAllEntities -> observeAllPermissions
 * - observeEntityUpdatedAt -> observePermissionUpdatedAt
 * - getEntityById -> getPermissionById
 * - getEntitiesByIds -> getPermissionsByIds
 * - getAllEntities -> getAllPermissions
 * - saveEntity -> savePermission
 * - saveEntities -> savePermissions
 * - deleteEntity -> deletePermission
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalPermissionRepository : BaseLocalRepository<Permission> {

    // === BaseLocalRepository 메서드 (구현체에서 권한 전용 메서드로 매핑) ===
    // observeEntityById -> observePermissionById
    // observeAllEntities -> observeAllPermissions  
    // observeEntityUpdatedAt -> observePermissionUpdatedAt
    // getEntityById -> getPermissionById
    // getEntitiesByIds -> getPermissionsByIds
    // getAllEntities -> getAllPermissions
    // saveEntity -> savePermission
    // saveEntities -> savePermissions
    // deleteEntity -> deletePermission
    // getEntitiesUpdatedAfter -> getPermissionsUpdatedAfter
    // clearAllEntities -> clearAllPermissions
    // getTotalEntityCount -> getTotalPermissionCount
    // entityExists -> permissionExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 권한을 실시간 관찰
     * @param permissionId 권한 ID
     * @return 권한 Flow (null 가능)
     */
    fun observePermissionById(permissionId: String): Flow<Permission?>

    /**
     * 특정 역할의 모든 권한을 실시간 관찰
     * @param roleId 역할 ID
     * @return 권한 목록 Flow
     */
    fun observePermissionsByRole(roleId: String): Flow<List<Permission>>

    /**
     * 주어진 ID 목록에 해당하는 권한 목록을 실시간 관찰
     * @param permissionIds 권한 ID 목록
     * @return 권한 목록 Flow
     */
    fun observePermissions(permissionIds: List<String>): Flow<List<Permission>>

    /**
     * 특정 권한의 updatedAt 필드 변경을 실시간 관찰
     * @param permissionId 권한 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observePermissionUpdatedAt(permissionId: String): Flow<Long?>

    /**
     * 모든 권한을 실시간 관찰
     * @return 전체 권한 목록 Flow
     */
    fun observeAllPermissions(): Flow<List<Permission>>

    /**
     * 특정 권한 유형을 실시간 관찰
     * @param rolePermission 권한 유형
     * @return 권한 목록 Flow
     */
    fun observePermissionsByType(rolePermission: RolePermission): Flow<List<Permission>>

    // === 단순 읽기 작업 ===

    /**
     * 권한 ID로 조회
     * @param permissionId 권한 ID
     * @return 권한 (없으면 null)
     */
    suspend fun getPermissionById(permissionId: String): Permission?

    /**
     * 특정 역할의 모든 권한 조회
     * @param roleId 역할 ID
     * @return 권한 목록
     */
    suspend fun getPermissionsByRole(roleId: String): List<Permission>

    /**
     * 여러 권한 ID로 조회
     * @param permissionIds 권한 ID 목록
     * @return 권한 목록
     */
    suspend fun getPermissionsByIds(permissionIds: List<String>): List<Permission>

    /**
     * 전체 권한 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 권한 목록
     */
    suspend fun getAllPermissions(limit: Int? = null): List<Permission>

    /**
     * 특정 권한 유형 조회
     * @param rolePermission 권한 유형
     * @return 권한 목록
     */
    suspend fun getPermissionsByType(rolePermission: RolePermission): List<Permission>

    /**
     * 여러 역할의 권한들 조회
     * @param roleIds 역할 ID 목록
     * @return 권한 목록 (중복 제거됨)
     */
    suspend fun getPermissionsByRoles(roleIds: List<String>): List<Permission>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 권한 저장 (생성/수정)
     * @param permission 저장할 권한
     * @return 성공 여부
     */
    suspend fun savePermission(permission: Permission): CustomResult<Unit, Exception>

    /**
     * 권한 대량 저장 (동기화용)
     * @param permissions 저장할 권한 목록
     * @return 성공 여부
     */
    suspend fun savePermissions(permissions: List<Permission>): CustomResult<Unit, Exception>

    /**
     * 권한 삭제 (Soft Delete)
     * @param permissionId 권한 ID
     * @return 성공 여부
     */
    suspend fun deletePermission(permissionId: String): CustomResult<Unit, Exception>

    /**
     * 역할의 권한들 일괄 삭제
     * @param roleId 역할 ID
     * @return 성공 여부
     */
    suspend fun deletePermissionsByRole(roleId: String): CustomResult<Unit, Exception>

    /**
     * 특정 유형의 권한 생성
     * @param rolePermission 권한 유형
     * @return 성공 여부
     */
    suspend fun createPermissionByType(rolePermission: RolePermission): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 권한 존재 여부 확인
     * @param permissionId 권한 ID
     * @return 존재 여부
     */
    suspend fun permissionExists(permissionId: String): Boolean

    /**
     * 역할이 특정 권한을 가지고 있는지 확인
     * @param roleId 역할 ID
     * @param rolePermission 권한 유형
     * @return 권한 보유 여부
     */
    suspend fun roleHasPermission(roleId: String, rolePermission: RolePermission): Boolean

    /**
     * 특정 권한 유형이 존재하는지 확인
     * @param rolePermission 권한 유형
     * @return 존재 여부
     */
    suspend fun permissionTypeExists(rolePermission: RolePermission): Boolean

    /**
     * 전체 권한 수 조회
     * @return 권한 수
     */
    suspend fun getTotalPermissionCount(): Int

    /**
     * 특정 역할의 권한 수 조회
     * @param roleId 역할 ID
     * @return 권한 수
     */
    suspend fun getPermissionCountByRole(roleId: String): Int

    /**
     * 특정 권한 유형의 개수 조회
     * @param rolePermission 권한 유형
     * @return 권한 수
     */
    suspend fun getPermissionCountByType(rolePermission: RolePermission): Int

    /**
     * 모든 권한 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllPermissions(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 권한 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 권한 목록
     */
    suspend fun getPermissionsUpdatedAfter(timestamp: Instant): List<Permission>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param permissionId 권한 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        permissionId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}