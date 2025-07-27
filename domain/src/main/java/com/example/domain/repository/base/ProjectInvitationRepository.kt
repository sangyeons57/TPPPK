package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext

/**
 * Remote ProjectInvitation Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface ProjectInvitationRepository {
    val factoryContext: ProjectInvitationRepositoryFactoryContext

    // === 동기화 메서드 ===

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectId: String? = null
    ): CustomResult<SyncResult<ProjectInvitation>, Exception>

    suspend fun syncToServer(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedInvitationIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions (서버 작업) ===
    
    /**
     * 초대 코드로 초대 정보를 조회합니다.
     */
    suspend fun getInvitationByCode(
        inviteCode: InviteCode
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 초대 코드의 유효성을 검증합니다.
     */
    suspend fun validateInviteCode(
        inviteCode: InviteCode,
        userId: UserId? = null,
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트 초대 링크를 생성합니다.
     */
    suspend fun generateInviteLink(
        projectId: DocumentId,
        expiresInHours: Int = 24,
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 사용하여 프로젝트에 참여합니다.
     */
    suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception>
}