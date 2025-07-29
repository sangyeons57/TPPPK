package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Projects Wrapper Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemoteProjectsWrapperRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - ProjectsWrapper 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeWrapperById
 * - observeAllEntities -> observeAllWrappers
 * - observeEntityUpdatedAt -> observeWrapperUpdatedAt
 * - getEntityById -> getWrapperById
 * - getEntitiesByIds -> getWrappersByIds
 * - getAllEntities -> getAllWrappers
 * - saveEntity -> saveWrapper
 * - saveEntities -> saveWrappers
 * - deleteEntity -> deleteWrapper
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalProjectsWrapperRepository : BaseLocalRepository<ProjectsWrapper> {

    // === BaseLocalRepository 메서드 (구현체에서 래퍼 전용 메서드로 매핑) ===
    // observeEntityById -> observeWrapperById
    // observeAllEntities -> observeAllWrappers  
    // observeEntityUpdatedAt -> observeWrapperUpdatedAt
    // getEntityById -> getWrapperById
    // getEntitiesByIds -> getWrappersByIds
    // getAllEntities -> getAllWrappers
    // saveEntity -> saveWrapper
    // saveEntities -> saveWrappers
    // deleteEntity -> deleteWrapper
    // getEntitiesUpdatedAfter -> getWrappersUpdatedAfter
    // clearAllEntities -> clearAllWrappers
    // getTotalEntityCount -> getTotalWrapperCount
    // entityExists -> wrapperExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 프로젝트 래퍼를 실시간 관찰
     * @param wrapperId 래퍼 ID
     * @return 래퍼 Flow (null 가능)
     */
    fun observeWrapperById(wrapperId: String): Flow<ProjectsWrapper?>

    /**
     * 특정 사용자의 모든 프로젝트 래퍼를 순서대로 실시간 관찰
     * @param userId 사용자 ID
     * @return 래퍼 목록 Flow (order 순서)
     */
    fun observeWrappersByUser(userId: String): Flow<List<ProjectsWrapper>>

    /**
     * 사용자의 특정 프로젝트 래퍼를 실시간 관찰
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID
     * @return 래퍼 Flow (null 가능)
     */
    fun observeWrapperByUserAndProject(userId: String, projectId: String): Flow<ProjectsWrapper?>

    /**
     * 특정 프로젝트 이름과 일치하는 래퍼들을 실시간 관찰
     * @param projectName 프로젝트 이름
     * @return 래퍼 목록 Flow
     */
    fun observeWrappersByProjectName(projectName: ProjectName): Flow<List<ProjectsWrapper>>

    /**
     * 프로젝트 이름으로 검색하여 래퍼들을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 래퍼 목록 Flow
     */
    fun observeWrappersByNameContaining(name: String, limit: Int = 10): Flow<List<ProjectsWrapper>>

    /**
     * 주어진 ID 목록에 해당하는 래퍼 목록을 실시간 관찰
     * @param wrapperIds 래퍼 ID 목록
     * @return 래퍼 목록 Flow
     */
    fun observeWrappers(wrapperIds: List<String>): Flow<List<ProjectsWrapper>>

    /**
     * 특정 래퍼의 updatedAt 필드 변경을 실시간 관찰
     * @param wrapperId 래퍼 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeWrapperUpdatedAt(wrapperId: String): Flow<Long?>

    /**
     * 모든 래퍼를 실시간 관찰
     * @return 전체 래퍼 목록 Flow
     */
    fun observeAllWrappers(): Flow<List<ProjectsWrapper>>

    /**
     * 특정 순서 범위의 래퍼들을 실시간 관찰
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 래퍼 목록 Flow
     */
    fun observeWrappersByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<ProjectsWrapper>>

    // === 단순 읽기 작업 ===

    /**
     * 래퍼 ID로 조회
     * @param wrapperId 래퍼 ID
     * @return 래퍼 (없으면 null)
     */
    suspend fun getWrapperById(wrapperId: String): ProjectsWrapper?

    /**
     * 특정 사용자의 모든 프로젝트 래퍼를 순서대로 조회
     * @param userId 사용자 ID
     * @return 래퍼 목록 (order 순서)
     */
    suspend fun getWrappersByUser(userId: String): List<ProjectsWrapper>

    /**
     * 사용자의 특정 프로젝트 래퍼 조회
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID
     * @return 래퍼 (없으면 null)
     */
    suspend fun getWrapperByUserAndProject(userId: String, projectId: String): ProjectsWrapper?

    /**
     * 특정 프로젝트 이름과 일치하는 래퍼들 조회
     * @param projectName 프로젝트 이름
     * @return 래퍼 목록
     */
    suspend fun getWrappersByProjectName(projectName: ProjectName): List<ProjectsWrapper>

    /**
     * 프로젝트 이름으로 검색하여 래퍼들 조회
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 래퍼 목록
     */
    suspend fun searchWrappersByName(name: String, limit: Int = 10): List<ProjectsWrapper>

    /**
     * 여러 래퍼 ID로 조회
     * @param wrapperIds 래퍼 ID 목록
     * @return 래퍼 목록
     */
    suspend fun getWrappersByIds(wrapperIds: List<String>): List<ProjectsWrapper>

    /**
     * 전체 래퍼 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 래퍼 목록
     */
    suspend fun getAllWrappers(limit: Int? = null): List<ProjectsWrapper>

    /**
     * 특정 순서 범위의 래퍼들 조회
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 래퍼 목록
     */
    suspend fun getWrappersByOrderRange(minOrder: Int, maxOrder: Int): List<ProjectsWrapper>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 래퍼 저장 (생성/수정)
     * @param wrapper 저장할 래퍼
     * @return 성공 여부
     */
    suspend fun saveWrapper(wrapper: ProjectsWrapper): CustomResult<Unit, Exception>

    /**
     * 래퍼 대량 저장 (동기화용)
     * @param wrappers 저장할 래퍼 목록
     * @return 성공 여부
     */
    suspend fun saveWrappers(wrappers: List<ProjectsWrapper>): CustomResult<Unit, Exception>

    /**
     * 래퍼 삭제 (Soft Delete)
     * @param wrapperId 래퍼 ID
     * @return 성공 여부
     */
    suspend fun deleteWrapper(wrapperId: String): CustomResult<Unit, Exception>

    /**
     * 래퍼 정보 업데이트 (로컬)
     * @param wrapperId 래퍼 ID
     * @param projectName 새로운 프로젝트 이름 (nullable)
     * @param projectImageUrl 새로운 프로젝트 이미지 URL (nullable)
     * @param order 새로운 순서 (nullable)
     * @return 성공 여부
     */
    suspend fun updateWrapper(
        wrapperId: String,
        projectName: ProjectName? = null,
        projectImageUrl: ImageUrl? = null,
        order: ProjectWrapperOrder? = null
    ): CustomResult<Unit, Exception>

    /**
     * 래퍼 순서 재정렬
     * @param wrapperOrderMap 래퍼 ID와 새로운 순서 매핑
     * @return 성공 여부
     */
    suspend fun reorderWrappers(wrapperOrderMap: Map<String, Int>): CustomResult<Unit, Exception>

    /**
     * 사용자의 프로젝트 래퍼 순서 재정렬
     * @param userId 사용자 ID
     * @param wrapperOrderMap 래퍼 ID와 새로운 순서 매핑
     * @return 성공 여부
     */
    suspend fun reorderUserWrappers(
        userId: String,
        wrapperOrderMap: Map<String, Int>
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 래퍼 존재 여부 확인
     * @param wrapperId 래퍼 ID
     * @return 존재 여부
     */
    suspend fun wrapperExists(wrapperId: String): Boolean

    /**
     * 사용자가 특정 프로젝트의 래퍼를 가지고 있는지 확인
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID
     * @return 래퍼 보유 여부
     */
    suspend fun userHasProjectWrapper(userId: String, projectId: String): Boolean

    /**
     * 프로젝트 이름 중복 확인 (사용자별)
     * @param userId 사용자 ID
     * @param projectName 프로젝트 이름
     * @param excludeWrapperId 제외할 래퍼 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun projectNameExistsForUser(
        userId: String,
        projectName: ProjectName,
        excludeWrapperId: String? = null
    ): Boolean

    /**
     * 전체 래퍼 수 조회
     * @return 래퍼 수
     */
    suspend fun getTotalWrapperCount(): Int

    /**
     * 특정 사용자의 래퍼 수 조회
     * @param userId 사용자 ID
     * @return 래퍼 수
     */
    suspend fun getWrapperCountByUser(userId: String): Int

    /**
     * 사용자의 다음 래퍼 순서 조회
     * @param userId 사용자 ID
     * @return 다음 순서 번호
     */
    suspend fun getNextWrapperOrder(userId: String): Int

    /**
     * 모든 래퍼 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllWrappers(): CustomResult<Unit, Exception>

    /**
     * 특정 사용자의 모든 래퍼 삭제
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    suspend fun clearUserWrappers(userId: String): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 래퍼 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 래퍼 목록
     */
    suspend fun getWrappersUpdatedAfter(timestamp: Instant): List<ProjectsWrapper>

}