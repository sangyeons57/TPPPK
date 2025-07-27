package com.example.data.repository.base

import android.net.Uri
import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalUsersDataSource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.mapper.UsersMapper
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.toDto
import com.example.domain.model.base.User
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

/**
 * Remote User Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 구현체
 *
 * 🔒 제약사항:
 * - Room 직접 접근 금지 (LocalDataSource를 통한 저장만 허용)
 * - Flow/LiveData 반환 금지
 * - 직접적인 UI 데이터 제공 금지
 *
 * ✅ 역할:
 * - Firestore에서 증분 데이터 fetch
 * - Outbox 데이터를 Firestore에 push
 * - 동기화 충돌 해결
 * - SyncMetadata 관리
 * - Firebase Functions 호출 (프로필 업데이트, 이미지 업로드)
 */
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    private val localUsersDataSource: LocalUsersDataSource,
    override val factoryContext: UserRepositoryFactoryContext,
) : UserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private const val SYNC_BATCH_SIZE = 50
    }

    // === 동기화 메서드 ===

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        userIds: List<String>?
    ): CustomResult<SyncResult<User>, Exception> {
        return try {
            Log.d(TAG, "syncFromServer: cursor=$lastSyncCursor, userIds=${userIds?.size}")

            // 1. Firestore에서 증분 데이터 가져오기
            val query = buildIncrementalQuery(lastSyncCursor, userIds)
            val querySnapshot = query.get().await()

            // 2. DTO를 Domain 모델로 변환
            val users = querySnapshot.documents.mapNotNull { document ->
                try {
                    val dto = document.toObject(UserDTO::class.java)?.copy(id = document.id)
                    dto?.let { UsersMapper.toDomain(UsersMapper.toEntity(it)) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to convert document ${document.id}", e)
                    null
                }
            }

            // 3. LocalDataSource에 저장 (Room DB)
            if (users.isNotEmpty()) {
                localUsersDataSource.saveUsers(users)
                Log.d(TAG, "Saved ${users.size} users to local DB")
            }

            // 4. 동기화 커서 업데이트
            val newCursor = users.maxOfOrNull { it.updatedAt.toEpochMilli() }
            if (newCursor != null) {
                localUsersDataSource.updateSyncCursor(
                    cursor = newCursor,
                    timestamp = System.currentTimeMillis(),
                    channelId = null // Users는 채널별 동기화 없음
                )
            }

            // 5. 결과 반환
            val hasMore = querySnapshot.size() >= SYNC_BATCH_SIZE
            CustomResult.Success(
                SyncResult(
                    data = users,
                    nextCursor = newCursor ?: lastSyncCursor,
                    hasMore = hasMore
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "syncFromServer failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun syncToServer(userIds: List<String>?): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "syncToServer: userIds=${userIds?.size}")

            // 1. LocalDataSource에서 Outbox 작업 가져오기
            val outboxOperations = localUsersDataSource.getPendingOutboxOperations()
            val filteredOperations = if (userIds != null) {
                outboxOperations.filter { it.userId in userIds }
            } else {
                outboxOperations
            }

            var processedCount = 0

            // 2. 각 Outbox 작업 처리
            for (operation in filteredOperations) {
                try {
                    when (operation.operation) {
                        "CREATE" -> {
                            val user = localUsersDataSource.getUserById(operation.userId)
                            if (user != null) {
                                val result = userRemoteDataSource.create(user.toDto())
                                if (result is CustomResult.Success) {
                                    localUsersDataSource.markOutboxOperationComplete(operation.id)
                                    processedCount++
                                }
                            }
                        }

                        "UPDATE" -> {
                            val user = localUsersDataSource.getUserById(operation.userId)
                            if (user != null) {
                                val result =
                                    userRemoteDataSource.update(user.id, user.getChangedFields())
                                if (result is CustomResult.Success) {
                                    localUsersDataSource.markOutboxOperationComplete(operation.id)
                                    processedCount++
                                }
                            }
                        }

                        "DELETE" -> {
                            val result = userRemoteDataSource.delete(operation.userId)
                            if (result is CustomResult.Success) {
                                localUsersDataSource.markOutboxOperationComplete(operation.id)
                                processedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to process outbox operation ${operation.id}", e)
                    // 재시도 횟수 증가
                    localUsersDataSource.incrementOutboxRetries(operation.id)
                }
            }

            Log.d(TAG, "Processed $processedCount outbox operations")
            CustomResult.Success(processedCount)

        } catch (e: Exception) {
            Log.e(TAG, "syncToServer failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun forceSyncAll(): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "forceSyncAll")

            var totalSynced = 0
            var lastCursor: Long? = null

            // 기존 동기화 커서 무시하고 전체 동기화
            do {
                val result = syncFromServer(lastCursor, null)
                when (result) {
                    is CustomResult.Success -> {
                        totalSynced += result.data.data.size
                        lastCursor = result.data.nextCursor

                        // 더 이상 데이터가 없으면 종료
                        if (!result.data.hasMore || result.data.data.isEmpty()) {
                            break
                        }
                    }

                    is CustomResult.Failure -> {
                        return result
                    }

                    else -> break
                }
            } while (true)

            Log.d(TAG, "Force sync completed: $totalSynced users")
            CustomResult.Success(totalSynced)

        } catch (e: Exception) {
            Log.e(TAG, "forceSyncAll failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun resolveConflicts(
        conflictedUserIds: List<String>
    ): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "resolveConflicts: ${conflictedUserIds.size} conflicts")

            var resolvedCount = 0

            for (userId in conflictedUserIds) {
                try {
                    // 서버 우선 정책: 서버 데이터로 로컬 덮어쓰기
                    val serverResult = userRemoteDataSource.findById(userId)
                    if (serverResult is CustomResult.Success) {
                        val serverDto = serverResult.data
                        val serverEntity = UsersMapper.toEntity(serverDto)
                        val serverUser = UsersMapper.toDomain(serverEntity)

                        // 로컬에 서버 데이터 저장 (충돌 해결)
                        localUsersDataSource.saveUser(serverUser)
                        resolvedCount++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resolve conflict for user $userId", e)
                }
            }

            Log.d(TAG, "Resolved $resolvedCount conflicts")
            CustomResult.Success(resolvedCount)

        } catch (e: Exception) {
            Log.e(TAG, "resolveConflicts failed", e)
            CustomResult.Failure(e)
        }
    }

    // === Firebase Functions 호출 (서버 작업) ===

    override suspend fun uploadProfileImage(uri: Uri): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.uploadUserProfileImage(uri)
    }

    override suspend fun removeProfileImage(): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.removeUserProfileImage()
    }

    override suspend fun updateProfile(name: String?, memo: String?): CustomResult<Unit, Exception> {
        return when (val result = functionsRemoteDataSource.updateUserProfile(name, memo)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.callFunction(functionName, data)
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> {
        return functionsRemoteDataSource.getHelloWorld()
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData)
    }

    override suspend fun sendFcmTestNotification(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception> {
        val title = "FCM 테스트 알림"
        val body = "채널 입장 테스트 알림 (채널ID: $channelId)"
        val data = mapOf(
            "type" to "mention",
            "channelId" to channelId
        )
        return functionsRemoteDataSource.callFunction(
            "sendCustomNotification",
            mapOf(
                "userId" to userId,
                "title" to title,
                "body" to body,
                "data" to data
            )
        )
    }

    private fun buildIncrementalQuery(
        lastSyncCursor: Long?,
        userIds: List<String>?
    ): Query {
        var query = userRemoteDataSource.getCollectionReference()
            .orderBy("updatedAt", Query.Direction.ASCENDING)
            .limit(SYNC_BATCH_SIZE.toLong())

        // 증분 동기화: 마지막 커서 이후 데이터만
        if (lastSyncCursor != null) {
            val cursorTimestamp = Instant.ofEpochMilli(lastSyncCursor)
            query = query.whereGreaterThan("updatedAt", cursorTimestamp)
        }

        // 특정 사용자들만 동기화
        if (userIds != null && userIds.isNotEmpty()) {
            query = query.whereIn("__name__", userIds)
        }

        return query
    }
}