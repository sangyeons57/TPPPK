package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalProjectInvitationsDataSource
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.repository.local.LocalProjectInvitationRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Project Invitation Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalProjectInvitationRepositoryImpl @Inject constructor(
    private val localProjectInvitationsDataSource: LocalProjectInvitationsDataSource
) : LocalProjectInvitationRepository {

    companion object {
        private const val TAG = "LocalProjectInvitationRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeInvitationById(invitationId: String): Flow<ProjectInvitation?> {
        Log.d(TAG, "observeInvitationById: $invitationId")
        return localProjectInvitationsDataSource.observeInvitationById(invitationId)
    }

    override fun observeInvitationByCode(inviteCode: InviteCode): Flow<ProjectInvitation?> {
        Log.d(TAG, "observeInvitationByCode: ${inviteCode.value}")
        return localProjectInvitationsDataSource.observeInvitationByCode(inviteCode)
    }

    override fun observeInvitationsByProject(projectId: String): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeInvitationsByProject: $projectId")
        return localProjectInvitationsDataSource.observeInvitationsByProject(projectId)
    }

    override fun observeInvitationsByInviter(inviterId: UserId): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeInvitationsByInviter: ${inviterId.value}")
        return localProjectInvitationsDataSource.observeInvitationsByInviter(inviterId)
    }

    override fun observeInvitationsByStatus(status: InviteStatus): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeInvitationsByStatus: $status")
        return localProjectInvitationsDataSource.observeInvitationsByStatus(status)
    }

    override fun observeInvitations(invitationIds: List<String>): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeInvitations: ${invitationIds.size} invitations")
        return localProjectInvitationsDataSource.observeInvitations(invitationIds)
    }

    override fun observeInvitationUpdatedAt(invitationId: String): Flow<Long?> {
        Log.d(TAG, "observeInvitationUpdatedAt: $invitationId")
        return localProjectInvitationsDataSource.observeInvitationUpdatedAt(invitationId)
    }

    override fun observeAllInvitations(): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeAllInvitations")
        return localProjectInvitationsDataSource.observeAllInvitations()
    }

    override fun observeActiveInvitations(): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeActiveInvitations")
        return localProjectInvitationsDataSource.observeActiveInvitations()
    }

    override fun observeExpiredInvitations(): Flow<List<ProjectInvitation>> {
        Log.d(TAG, "observeExpiredInvitations")
        return localProjectInvitationsDataSource.observeExpiredInvitations()
    }

    // === 단순 읽기 작업 ===

    override suspend fun getInvitationById(invitationId: String): ProjectInvitation? {
        Log.d(TAG, "getInvitationById: $invitationId")
        return try {
            localProjectInvitationsDataSource.getInvitationById(invitationId)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationById failed", e)
            null
        }
    }

    override suspend fun getInvitationByCode(inviteCode: InviteCode): ProjectInvitation? {
        Log.d(TAG, "getInvitationByCode: ${inviteCode.value}")
        return try {
            localProjectInvitationsDataSource.getInvitationByCode(inviteCode)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationByCode failed", e)
            null
        }
    }

    override suspend fun getInvitationsByProject(projectId: String): List<ProjectInvitation> {
        Log.d(TAG, "getInvitationsByProject: $projectId")
        return try {
            localProjectInvitationsDataSource.getInvitationsByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationsByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getInvitationsByInviter(inviterId: UserId): List<ProjectInvitation> {
        Log.d(TAG, "getInvitationsByInviter: ${inviterId.value}")
        return try {
            localProjectInvitationsDataSource.getInvitationsByInviter(inviterId)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationsByInviter failed", e)
            emptyList()
        }
    }

    override suspend fun getInvitationsByStatus(status: InviteStatus): List<ProjectInvitation> {
        Log.d(TAG, "getInvitationsByStatus: $status")
        return try {
            localProjectInvitationsDataSource.getInvitationsByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getInvitationsByIds(invitationIds: List<String>): List<ProjectInvitation> {
        Log.d(TAG, "getInvitationsByIds: ${invitationIds.size} invitations")
        return try {
            localProjectInvitationsDataSource.getInvitationsByIds(invitationIds)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllInvitations(limit: Int?): List<ProjectInvitation> {
        Log.d(TAG, "getAllInvitations: limit=$limit")
        return try {
            localProjectInvitationsDataSource.getAllInvitations(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllInvitations failed", e)
            emptyList()
        }
    }

    override suspend fun getActiveInvitations(): List<ProjectInvitation> {
        Log.d(TAG, "getActiveInvitations")
        return try {
            localProjectInvitationsDataSource.getActiveInvitations()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveInvitations failed", e)
            emptyList()
        }
    }

    override suspend fun getExpiredInvitations(): List<ProjectInvitation> {
        Log.d(TAG, "getExpiredInvitations")
        return try {
            localProjectInvitationsDataSource.getExpiredInvitations()
        } catch (e: Exception) {
            Log.e(TAG, "getExpiredInvitations failed", e)
            emptyList()
        }
    }

    override suspend fun getInvitationsExpiringBefore(beforeTime: Instant): List<ProjectInvitation> {
        Log.d(TAG, "getInvitationsExpiringBefore: $beforeTime")
        return try {
            localProjectInvitationsDataSource.getInvitationsExpiringBefore(beforeTime)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationsExpiringBefore failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveInvitation(invitation: ProjectInvitation): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveInvitation: ${invitation.id}")

            // 1. Room DB에 저장
            localProjectInvitationsDataSource.saveInvitation(invitation)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (invitation.isNew) "CREATE" else "UPDATE"
            localProjectInvitationsDataSource.addToOutbox(
                invitationId = invitation.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Invitation saved and added to outbox: ${invitation.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveInvitation failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveInvitations(invitations: List<ProjectInvitation>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveInvitations: ${invitations.size} invitations")

            if (invitations.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localProjectInvitationsDataSource.saveInvitations(invitations)

            Log.d(TAG, "Bulk invitations saved: ${invitations.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveInvitations failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteInvitation(invitationId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteInvitation: $invitationId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localProjectInvitationsDataSource.deleteInvitation(invitationId)

            // 2. Outbox에 삭제 작업 추가
            localProjectInvitationsDataSource.addToOutbox(
                invitationId = invitationId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Invitation deleted and added to outbox: $invitationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteInvitation failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateInvitationStatus(
        invitationId: String,
        status: InviteStatus
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateInvitationStatus: invitationId=$invitationId, status=$status")

            // 1. 현재 초대 조회
            val currentInvitation =
                localProjectInvitationsDataSource.getInvitationById(invitationId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Invitation not found: $invitationId"))

            // 2. 상태 변경
            currentInvitation.changeStatus(status)

            // 3. 저장 (Outbox 포함)
            saveInvitation(currentInvitation)

            Log.d(TAG, "Invitation status updated: $invitationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateInvitationStatus failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun expireInvitation(invitationId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "expireInvitation: $invitationId")

            // 1. 현재 초대 조회
            val currentInvitation =
                localProjectInvitationsDataSource.getInvitationById(invitationId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Invitation not found: $invitationId"))

            // 2. 만료 처리
            currentInvitation.expire()

            // 3. 저장 (Outbox 포함)
            saveInvitation(currentInvitation)

            Log.d(TAG, "Invitation expired: $invitationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "expireInvitation failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun revokeInvitation(invitationId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "revokeInvitation: $invitationId")

            // 1. 현재 초대 조회
            val currentInvitation =
                localProjectInvitationsDataSource.getInvitationById(invitationId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Invitation not found: $invitationId"))

            // 2. 취소 처리
            currentInvitation.revoke()

            // 3. 저장 (Outbox 포함)
            saveInvitation(currentInvitation)

            Log.d(TAG, "Invitation revoked: $invitationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "revokeInvitation failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun acceptInvitation(invitationId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "acceptInvitation: $invitationId")

            // 1. 현재 초대 조회
            val currentInvitation =
                localProjectInvitationsDataSource.getInvitationById(invitationId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Invitation not found: $invitationId"))

            // 2. 수락 처리
            currentInvitation.changeStatus(InviteStatus.ACCEPTED)

            // 3. 저장 (Outbox 포함)
            saveInvitation(currentInvitation)

            Log.d(TAG, "Invitation accepted: $invitationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "acceptInvitation failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun cleanupExpiredInvitations(): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "cleanupExpiredInvitations")

            // 1. 만료된 초대들 조회
            val expiredInvitations = localProjectInvitationsDataSource.getExpiredInvitations()

            // 2. 각 초대를 만료 상태로 업데이트
            var cleanedCount = 0
            expiredInvitations.forEach { invitation ->
                if (invitation.status != InviteStatus.EXPIRED) {
                    invitation.expire()
                    localProjectInvitationsDataSource.saveInvitation(invitation)
                    cleanedCount++
                }
            }

            Log.d(TAG, "Expired invitations cleaned up: $cleanedCount")
            CustomResult.Success(cleanedCount)

        } catch (e: Exception) {
            Log.e(TAG, "cleanupExpiredInvitations failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun invitationExists(invitationId: String): Boolean {
        return try {
            localProjectInvitationsDataSource.invitationExists(invitationId)
        } catch (e: Exception) {
            Log.e(TAG, "invitationExists failed", e)
            false
        }
    }

    override suspend fun inviteCodeExists(inviteCode: InviteCode): Boolean {
        return try {
            localProjectInvitationsDataSource.inviteCodeExists(inviteCode)
        } catch (e: Exception) {
            Log.e(TAG, "inviteCodeExists failed", e)
            false
        }
    }

    override suspend fun isInvitationActive(invitationId: String): Boolean {
        return try {
            val invitation = localProjectInvitationsDataSource.getInvitationById(invitationId)
            invitation?.isActive() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "isInvitationActive failed", e)
            false
        }
    }

    override suspend fun canInvitationBeUsed(invitationId: String): Boolean {
        return try {
            val invitation = localProjectInvitationsDataSource.getInvitationById(invitationId)
            invitation?.canBeUsed() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "canInvitationBeUsed failed", e)
            false
        }
    }

    override suspend fun isInvitationExpired(invitationId: String): Boolean {
        return try {
            val invitation = localProjectInvitationsDataSource.getInvitationById(invitationId)
            invitation?.status == InviteStatus.EXPIRED ||
                    (invitation?.expiresAt?.isBefore(Instant.now()) == true)
        } catch (e: Exception) {
            Log.e(TAG, "isInvitationExpired failed", e)
            false
        }
    }

    override suspend fun getTotalInvitationCount(): Int {
        return try {
            localProjectInvitationsDataSource.getTotalInvitationCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalInvitationCount failed", e)
            0
        }
    }

    override suspend fun getInvitationCountByProject(projectId: String): Int {
        return try {
            localProjectInvitationsDataSource.getInvitationCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationCountByProject failed", e)
            0
        }
    }

    override suspend fun getInvitationCountByStatus(status: InviteStatus): Int {
        return try {
            localProjectInvitationsDataSource.getInvitationCountByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationCountByStatus failed", e)
            0
        }
    }

    override suspend fun getActiveInvitationCount(): Int {
        return try {
            localProjectInvitationsDataSource.getActiveInvitationCount()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveInvitationCount failed", e)
            0
        }
    }

    override suspend fun clearAllInvitations(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllInvitations")

            localProjectInvitationsDataSource.clearAllInvitations()

            Log.d(TAG, "All invitations cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllInvitations failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getInvitationsUpdatedAfter(timestamp: Instant): List<ProjectInvitation> {
        return try {
            localProjectInvitationsDataSource.getInvitationsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitationsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        invitationId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: invitationId=$invitationId, operation=$operation")

            localProjectInvitationsDataSource.addToOutbox(invitationId, operation, payload)

            Log.d(TAG, "Added to outbox: $invitationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}