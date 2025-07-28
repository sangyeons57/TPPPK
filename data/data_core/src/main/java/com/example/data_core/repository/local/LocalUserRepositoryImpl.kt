package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalUsersDataSource
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.LocalUserRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local User Repository Implementation (SSOT)
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
class LocalUserRepositoryImpl @Inject constructor(
    private val localUsersDataSource: LocalUsersDataSource
) : LocalUserRepository {

    companion object {
        private const val TAG = "LocalUserRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeUserById(userId: String): Flow<User?> {
        Log.d(TAG, "observeUserById: $userId")
        return localUsersDataSource.observeUserById(userId)
    }

    override fun observeByName(name: UserName): Flow<User?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return localUsersDataSource.observeByName(name)
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<User>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return localUsersDataSource.observeAllByName(name, limit)
    }

    override fun observeByEmail(email: String): Flow<User?> {
        Log.d(TAG, "observeByEmail: $email")
        return localUsersDataSource.observeByEmail(email)
    }

    override fun observeUsers(userIds: List<String>): Flow<List<User>> {
        Log.d(TAG, "observeUsers: ${userIds.size} users")
        return localUsersDataSource.observeUsers(userIds)
    }

    override fun observeUserUpdatedAt(userId: String): Flow<Long?> {
        Log.d(TAG, "observeUserUpdatedAt: $userId")
        return localUsersDataSource.observeUserUpdatedAt(userId)
    }

    override fun observeAllUsers(): Flow<List<User>> {
        Log.d(TAG, "observeAllUsers")
        return localUsersDataSource.observeAllUsers()
    }

    // === 단순 읽기 작업 ===

    override suspend fun getUserById(userId: String): User? {
        Log.d(TAG, "getUserById: $userId")
        return try {
            localUsersDataSource.getUserById(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getUserById failed", e)
            null
        }
    }

    override suspend fun getUserByEmail(email: String): User? {
        Log.d(TAG, "getUserByEmail: $email")
        return try {
            localUsersDataSource.getUserByEmail(email)
        } catch (e: Exception) {
            Log.e(TAG, "getUserByEmail failed", e)
            null
        }
    }

    override suspend fun getUserByName(name: UserName): User? {
        Log.d(TAG, "getUserByName: ${name.value}")
        return try {
            localUsersDataSource.getUserByName(name)
        } catch (e: Exception) {
            Log.e(TAG, "getUserByName failed", e)
            null
        }
    }

    override suspend fun searchUsersByName(name: String, limit: Int): List<User> {
        Log.d(TAG, "searchUsersByName: name='$name', limit=$limit")
        return try {
            localUsersDataSource.searchUsersByName(name, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchUsersByName failed", e)
            emptyList()
        }
    }

    override suspend fun getUsersByIds(userIds: List<String>): List<User> {
        Log.d(TAG, "getUsersByIds: ${userIds.size} users")
        return try {
            localUsersDataSource.getUsersByIds(userIds)
        } catch (e: Exception) {
            Log.e(TAG, "getUsersByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllUsers(limit: Int?): List<User> {
        Log.d(TAG, "getAllUsers: limit=$limit")
        return try {
            localUsersDataSource.getAllUsers(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllUsers failed", e)
            emptyList()
        }
    }

    override suspend fun getUsersByStatus(accountStatus: String): List<User> {
        Log.d(TAG, "getUsersByStatus: $accountStatus")
        return try {
            localUsersDataSource.getUsersByStatus(accountStatus)
        } catch (e: Exception) {
            Log.e(TAG, "getUsersByStatus failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveUser(user: User): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveUser: ${user.id}")

            // 1. Room DB에 저장
            localUsersDataSource.saveUser(user)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (user.isNew) "CREATE" else "UPDATE"
            localUsersDataSource.addToOutbox(
                userId = user.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "User saved and added to outbox: ${user.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveUsers(users: List<User>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveUsers: ${users.size} users")

            if (users.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localUsersDataSource.saveUsers(users)

            Log.d(TAG, "Bulk users saved: ${users.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveUsers failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteUser: $userId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localUsersDataSource.deleteUser(userId)

            // 2. Outbox에 삭제 작업 추가
            localUsersDataSource.addToOutbox(
                userId = userId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "User deleted and added to outbox: $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateUserProfile(
        userId: String,
        name: String?,
        memo: String?,
        profileImageUrl: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateUserProfile: userId=$userId, name=$name, memo=$memo")

            // 1. 현재 사용자 조회
            val currentUser = localUsersDataSource.getUserById(userId)
                ?: return CustomResult.Failure(IllegalArgumentException("User not found: $userId"))

            // 2. 업데이트된 사용자 생성 (필요한 필드만 수정)
            var updatedUser = currentUser

            name?.let {
                updatedUser = updatedUser.copy(name = UserName(it))
            }
            memo?.let {
                updatedUser = updatedUser.copy(memo = it)
            }
            profileImageUrl?.let {
                updatedUser = updatedUser.copy(profileImageUrl = it)
            }

            // 3. 저장 (Outbox 포함)
            saveUser(updatedUser)

            Log.d(TAG, "User profile updated: $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateUserProfile failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun userExists(userId: String): Boolean {
        return try {
            localUsersDataSource.userExists(userId)
        } catch (e: Exception) {
            Log.e(TAG, "userExists failed", e)
            false
        }
    }

    override suspend fun emailExists(email: String): Boolean {
        return try {
            localUsersDataSource.emailExists(email)
        } catch (e: Exception) {
            Log.e(TAG, "emailExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: UserName): Boolean {
        return try {
            localUsersDataSource.nameExists(name)
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getTotalUserCount(): Int {
        return try {
            localUsersDataSource.getTotalUserCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalUserCount failed", e)
            0
        }
    }

    override suspend fun getActiveUserCount(): Int {
        return try {
            localUsersDataSource.getActiveUserCount()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveUserCount failed", e)
            0
        }
    }

    override suspend fun clearAllUsers(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllUsers")

            localUsersDataSource.clearAllUsers()

            Log.d(TAG, "All users cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllUsers failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getUsersUpdatedAfter(timestamp: Instant): List<User> {
        return try {
            localUsersDataSource.getUsersUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getUsersUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        userId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: userId=$userId, operation=$operation")

            localUsersDataSource.addToOutbox(userId, operation, payload)

            Log.d(TAG, "Added to outbox: $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}