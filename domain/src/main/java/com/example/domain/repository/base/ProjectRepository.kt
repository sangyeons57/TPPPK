package com.example.domain.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext

/**
 * Remote Project Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 *
 * 🔒 제약사항:
 * - Room 접근 금지 (LocalProjectRepository 사용)
 * - Flow/LiveData 반환 금지 (비동기 fetch-only)
 * - 직접적인 CRUD 작업 불가능
 *
 * ✅ 역할:
 * - 서버에서 증분 데이터 가져오기 (updatedAt > cursor)
 * - 로컬 변경사항을 서버에 반영 (Outbox → Firestore)
 * - 동기화 충돌 해결 (서버 vs 로컬)
 * - 커서 기반 동기화 메타데이터 관리
 * - Firebase Functions 호출 (프로젝트 관리, 이미지 업로드 등)
 */
interface ProjectRepository {
    val factoryContext: ProjectRepositoryFactoryContext

    // === 동기화 메서드 ===

    /**
     * 서버에서 증분 데이터 가져오기 (Client-driven Sync)
     * @param lastSyncCursor 마지막 동기화 커서 (null이면 전체 동기화)
     * @param projectIds 특정 프로젝트들만 동기화 (null이면 전체 프로젝트)
     * @return 새로운 프로젝트 목록과 다음 커서
     */
    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectIds: List<String>? = null
    ): CustomResult<SyncResult<Project>, Exception>

    /**
     * 로컬 변경사항을 서버에 반영 (Outbox Processing)
     * @param projectIds 특정 프로젝트들의 Outbox만 처리 (null이면 전체)
     * @return 처리된 Outbox 작업 수
     */
    suspend fun syncToServer(
        projectIds: List<String>? = null
    ): CustomResult<Int, Exception>

    /**
     * 강제 전체 동기화 (예: 첫 로그인, 데이터 불일치 해결)
     * @return 동기화된 프로젝트 수
     */
    suspend fun forceSyncAll(): CustomResult<Int, Exception>

    /**
     * 동기화 충돌 해결 (서버 우선 정책)
     * @param conflictedProjectIds 충돌이 발생한 프로젝트 ID 목록
     * @return 해결된 충돌 수
     */
    suspend fun resolveConflicts(
        conflictedProjectIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions 호출 (서버 작업) ===

    /**
     * 프로젝트 프로필 이미지를 업로드합니다.
     * Firebase Storage에 업로드 후 자동으로 Firebase Functions가 처리합니다.
     *
     * @param projectId 프로젝트 ID
     * @param uri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun uploadProfileImage(projectId: DocumentId, uri: Uri): CustomResult<Unit, Exception>

    /**
     * 프로젝트 프로필 이미지를 삭제합니다.
     * Firebase Functions를 통해 프로젝트 프로필 이미지를 제거합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun removeProfileImage(projectId: DocumentId): CustomResult<Unit, Exception>

    /**
     * 프로젝트를 삭제합니다 (soft delete).
     *
     * @param projectId 삭제할 프로젝트 ID
     * @return 성공 시 삭제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun deleteProject(projectId: DocumentId): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트에서 나갑니다.
     *
     * @param projectId 나갈 프로젝트 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun leaveProject(projectId: DocumentId): CustomResult<Unit, Exception>

    /**
     * 프로젝트 소유권을 다른 멤버에게 전달합니다.
     *
     * @param projectId 프로젝트 ID
     * @param newOwnerId 새로운 소유자 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun transferOwnership(projectId: DocumentId, newOwnerId: String): CustomResult<Unit, Exception>
}
