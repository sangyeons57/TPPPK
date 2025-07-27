package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalMembersDataSource
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalMemberRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Member Repository Implementation (SSOT)
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
class LocalMemberRepositoryImpl @Inject constructor(
    private val localMembersDataSource: LocalMembersDataSource
) : LocalMemberRepository {

    companion object {
        private const val TAG = "LocalMemberRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeMemberById(memberId: String): Flow<Member?> {
        Log.d(TAG, "observeMemberById: $memberId")
        return localMembersDataSource.observeMemberById(memberId)
    }

    override fun observeMembersByProject(projectId: String): Flow<List<Member>> {
        Log.d(TAG, "observeMembersByProject: $projectId")
        return localMembersDataSource.observeMembersByProject(projectId)
    }

    override fun observeMemberByProjectAndUser(projectId: String, userId: String): Flow<Member?> {
        Log.d(TAG, "observeMemberByProjectAndUser: projectId=$projectId, userId=$userId")
        return localMembersDataSource.observeMemberByProjectAndUser(projectId, userId)
    }

    override fun observeMembers(memberIds: List<String>): Flow<List<Member>> {
        Log.d(TAG, "observeMembers: ${memberIds.size} members")
        return localMembersDataSource.observeMembers(memberIds)
    }

    override fun observeMembersByRole(roleId: String): Flow<List<Member>> {
        Log.d(TAG, "observeMembersByRole: $roleId")
        return localMembersDataSource.observeMembersByRole(roleId)
    }

    override fun observeMemberUpdatedAt(memberId: String): Flow<Long?> {
        Log.d(TAG, "observeMemberUpdatedAt: $memberId")
        return localMembersDataSource.observeMemberUpdatedAt(memberId)
    }

    override fun observeAllMembers(): Flow<List<Member>> {
        Log.d(TAG, "observeAllMembers")
        return localMembersDataSource.observeAllMembers()
    }

    // === 단순 읽기 작업 ===

    override suspend fun getMemberById(memberId: String): Member? {
        Log.d(TAG, "getMemberById: $memberId")
        return try {
            localMembersDataSource.getMemberById(memberId)
        } catch (e: Exception) {
            Log.e(TAG, "getMemberById failed", e)
            null
        }
    }

    override suspend fun getMembersByProject(projectId: String): List<Member> {
        Log.d(TAG, "getMembersByProject: $projectId")
        return try {
            localMembersDataSource.getMembersByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getMembersByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getMemberByProjectAndUser(projectId: String, userId: String): Member? {
        Log.d(TAG, "getMemberByProjectAndUser: projectId=$projectId, userId=$userId")
        return try {
            localMembersDataSource.getMemberByProjectAndUser(projectId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "getMemberByProjectAndUser failed", e)
            null
        }
    }

    override suspend fun getMembersByIds(memberIds: List<String>): List<Member> {
        Log.d(TAG, "getMembersByIds: ${memberIds.size} members")
        return try {
            localMembersDataSource.getMembersByIds(memberIds)
        } catch (e: Exception) {
            Log.e(TAG, "getMembersByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllMembers(limit: Int?): List<Member> {
        Log.d(TAG, "getAllMembers: limit=$limit")
        return try {
            localMembersDataSource.getAllMembers(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllMembers failed", e)
            emptyList()
        }
    }

    override suspend fun getMembersByRole(roleId: String): List<Member> {
        Log.d(TAG, "getMembersByRole: $roleId")
        return try {
            localMembersDataSource.getMembersByRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "getMembersByRole failed", e)
            emptyList()
        }
    }

    override suspend fun getMembersByRoles(roleIds: List<String>): List<Member> {
        Log.d(TAG, "getMembersByRoles: ${roleIds.size} roles")
        return try {
            localMembersDataSource.getMembersByRoles(roleIds)
        } catch (e: Exception) {
            Log.e(TAG, "getMembersByRoles failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveMember(member: Member): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveMember: ${member.id}")

            // 1. Room DB에 저장
            localMembersDataSource.saveMember(member)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (member.isNew) "CREATE" else "UPDATE"
            localMembersDataSource.addToOutbox(
                memberId = member.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Member saved and added to outbox: ${member.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveMember failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveMembers(members: List<Member>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveMembers: ${members.size} members")

            if (members.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localMembersDataSource.saveMembers(members)

            Log.d(TAG, "Bulk members saved: ${members.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveMembers failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteMember(memberId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteMember: $memberId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localMembersDataSource.deleteMember(memberId)

            // 2. Outbox에 삭제 작업 추가
            localMembersDataSource.addToOutbox(
                memberId = memberId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Member deleted and added to outbox: $memberId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteMember failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateMemberRoles(
        memberId: String,
        roleIds: List<DocumentId>
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateMemberRoles: memberId=$memberId, roles=${roleIds.size}")

            // 1. 현재 멤버 조회
            val currentMember = localMembersDataSource.getMemberById(memberId)
                ?: return CustomResult.Failure(IllegalArgumentException("Member not found: $memberId"))

            // 2. 역할 업데이트
            currentMember.updateRoles(roleIds)

            // 3. 저장 (Outbox 포함)
            saveMember(currentMember)

            Log.d(TAG, "Member roles updated: $memberId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateMemberRoles failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun assignRole(
        memberId: String,
        roleId: DocumentId
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "assignRole: memberId=$memberId, roleId=$roleId")

            // 1. 현재 멤버 조회
            val currentMember = localMembersDataSource.getMemberById(memberId)
                ?: return CustomResult.Failure(IllegalArgumentException("Member not found: $memberId"))

            // 2. 역할 할당
            currentMember.assignRole(roleId)

            // 3. 저장 (Outbox 포함)
            saveMember(currentMember)

            Log.d(TAG, "Role assigned to member: $memberId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "assignRole failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun revokeRole(
        memberId: String,
        roleId: DocumentId
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "revokeRole: memberId=$memberId, roleId=$roleId")

            // 1. 현재 멤버 조회
            val currentMember = localMembersDataSource.getMemberById(memberId)
                ?: return CustomResult.Failure(IllegalArgumentException("Member not found: $memberId"))

            // 2. 역할 해제
            currentMember.revokeRole(roleId)

            // 3. 저장 (Outbox 포함)
            saveMember(currentMember)

            Log.d(TAG, "Role revoked from member: $memberId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "revokeRole failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun memberExists(memberId: String): Boolean {
        return try {
            localMembersDataSource.memberExists(memberId)
        } catch (e: Exception) {
            Log.e(TAG, "memberExists failed", e)
            false
        }
    }

    override suspend fun isUserMemberOfProject(projectId: String, userId: String): Boolean {
        return try {
            localMembersDataSource.isUserMemberOfProject(projectId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "isUserMemberOfProject failed", e)
            false
        }
    }

    override suspend fun memberHasRole(memberId: String, roleId: String): Boolean {
        return try {
            localMembersDataSource.memberHasRole(memberId, roleId)
        } catch (e: Exception) {
            Log.e(TAG, "memberHasRole failed", e)
            false
        }
    }

    override suspend fun getTotalMemberCount(): Int {
        return try {
            localMembersDataSource.getTotalMemberCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalMemberCount failed", e)
            0
        }
    }

    override suspend fun getMemberCountByProject(projectId: String): Int {
        return try {
            localMembersDataSource.getMemberCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getMemberCountByProject failed", e)
            0
        }
    }

    override suspend fun getMemberCountByRole(roleId: String): Int {
        return try {
            localMembersDataSource.getMemberCountByRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "getMemberCountByRole failed", e)
            0
        }
    }

    override suspend fun clearAllMembers(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllMembers")

            localMembersDataSource.clearAllMembers()

            Log.d(TAG, "All members cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllMembers failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getMembersUpdatedAfter(timestamp: Instant): List<Member> {
        return try {
            localMembersDataSource.getMembersUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getMembersUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        memberId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: memberId=$memberId, operation=$operation")

            localMembersDataSource.addToOutbox(memberId, operation, payload)

            Log.d(TAG, "Added to outbox: $memberId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}