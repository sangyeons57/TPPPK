package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalDMWrapperDataSource
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.LocalDMWrapperRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local DM Wrapper Repository Implementation (SSOT)
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
class LocalDMWrapperRepositoryImpl @Inject constructor(
    private val localDmWrapperDataSource: LocalDMWrapperDataSource
) : LocalDMWrapperRepository {

    companion object {
        private const val TAG = "LocalDMWrapperRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeDMWrapperById(wrapperId: String): Flow<DMWrapper?> {
        Log.d(TAG, "observeDMWrapperById: $wrapperId")
        return localDmWrapperDataSource.observeDMWrapperById(wrapperId)
    }

    override fun observeDMWrappersByUser(currentUserId: String): Flow<List<DMWrapper>> {
        Log.d(TAG, "observeDMWrappersByUser: $currentUserId")
        return localDmWrapperDataSource.observeDMWrappersByUser(currentUserId)
    }

    override fun observeDMWrapperByOtherUser(otherUserId: String): Flow<DMWrapper?> {
        Log.d(TAG, "observeDMWrapperByOtherUser: $otherUserId")
        return localDmWrapperDataSource.observeDMWrapperByOtherUser(otherUserId)
    }

    override fun observeDMWrappersByUserName(userName: String, limit: Int): Flow<List<DMWrapper>> {
        Log.d(TAG, "observeDMWrappersByUserName: userName='$userName', limit=$limit")
        return localDmWrapperDataSource.observeDMWrappersByUserName(userName, limit)
    }

    override fun observeAllDMWrappers(): Flow<List<DMWrapper>> {
        Log.d(TAG, "observeAllDMWrappers")
        return localDmWrapperDataSource.observeAllDMWrappers()
    }

    override fun observeDMWrapperUpdatedAt(wrapperId: String): Flow<Long?> {
        Log.d(TAG, "observeDMWrapperUpdatedAt: $wrapperId")
        return localDmWrapperDataSource.observeDMWrapperUpdatedAt(wrapperId)
    }

    override fun observeDMWrappersWithRecentMessages(): Flow<List<DMWrapper>> {
        Log.d(TAG, "observeDMWrappersWithRecentMessages")
        return localDmWrapperDataSource.observeDMWrappersWithRecentMessages()
    }

    override fun observeDMWrappers(wrapperIds: List<String>): Flow<List<DMWrapper>> {
        Log.d(TAG, "observeDMWrappers: ${wrapperIds.size} wrappers")
        return localDmWrapperDataSource.observeDMWrappers(wrapperIds)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getDMWrapperById(wrapperId: String): DMWrapper? {
        Log.d(TAG, "getDMWrapperById: $wrapperId")
        return try {
            localDmWrapperDataSource.getDMWrapperById(wrapperId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrapperById failed", e)
            null
        }
    }

    override suspend fun getDMWrappersByUser(currentUserId: String): List<DMWrapper> {
        Log.d(TAG, "getDMWrappersByUser: $currentUserId")
        return try {
            localDmWrapperDataSource.getDMWrappersByUser(currentUserId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrappersByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrapperByOtherUser(otherUserId: String): DMWrapper? {
        Log.d(TAG, "getDMWrapperByOtherUser: $otherUserId")
        return try {
            localDmWrapperDataSource.getDMWrapperByOtherUser(otherUserId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrapperByOtherUser failed", e)
            null
        }
    }

    override suspend fun searchDMWrappersByUserName(userName: String, limit: Int): List<DMWrapper> {
        Log.d(TAG, "searchDMWrappersByUserName: userName='$userName', limit=$limit")
        return try {
            localDmWrapperDataSource.searchDMWrappersByUserName(userName, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchDMWrappersByUserName failed", e)
            emptyList()
        }
    }

    override suspend fun getAllDMWrappers(limit: Int?): List<DMWrapper> {
        Log.d(TAG, "getAllDMWrappers: limit=$limit")
        return try {
            localDmWrapperDataSource.getAllDMWrappers()
        } catch (e: Exception) {
            Log.e(TAG, "getAllDMWrappers failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrappersByIds(wrapperIds: List<String>): List<DMWrapper> {
        Log.d(TAG, "getDMWrappersByIds: ${wrapperIds.size} wrappers")
        return try {
            localDmWrapperDataSource.getDMWrappersByIds(wrapperIds)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrappersByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrappersWithRecentMessages(): List<DMWrapper> {
        Log.d(TAG, "getDMWrappersWithRecentMessages")
        return try {
            localDmWrapperDataSource.getDMWrappersWithRecentMessages()
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrappersWithRecentMessages failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrappersByOtherUsers(otherUserIds: List<String>): List<DMWrapper> {
        Log.d(TAG, "getDMWrappersByOtherUsers: ${otherUserIds.size} users")
        return try {
            localDmWrapperDataSource.getDMWrappersByOtherUsers(otherUserIds)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrappersByOtherUsers failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveDMWrapper(dmWrapper: DMWrapper): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveDMWrapper: ${dmWrapper.id}")

            // 1. Room DB에 저장
            localDmWrapperDataSource.saveDMWrapper(dmWrapper)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (dmWrapper.isNew) "CREATE" else "UPDATE"
            localDmWrapperDataSource.addToOutbox(
                wrapperId = dmWrapper.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "DM Wrapper saved and added to outbox: ${dmWrapper.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveDMWrapper failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveDMWrappers(dmWrappers: List<DMWrapper>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveDMWrappers: ${dmWrappers.size} wrappers")

            if (dmWrappers.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localDmWrapperDataSource.saveDMWrappers(dmWrappers)

            Log.d(TAG, "Bulk DM wrappers saved: ${dmWrappers.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveDMWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteDMWrapper(wrapperId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteDMWrapper: $wrapperId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localDmWrapperDataSource.deleteDMWrapper(wrapperId)

            // 2. Outbox에 삭제 작업 추가
            localDmWrapperDataSource.addToOutbox(
                wrapperId = wrapperId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "DM Wrapper deleted and added to outbox: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteDMWrapper failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteDMWrappersByUser(currentUserId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteDMWrappersByUser: $currentUserId")

            localDmWrapperDataSource.deleteDMWrappersByUser(currentUserId)

            Log.d(TAG, "DM Wrappers deleted for user: $currentUserId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteDMWrappersByUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateDMWrapper(
        wrapperId: String,
        otherUserName: UserName?,
        otherUserImageUrl: ImageUrl?,
        lastMessagePreview: DMChannelLastMessagePreview?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateDMWrapper: wrapperId=$wrapperId")

            // 1. 현재 wrapper 조회
            val currentWrapper = localDmWrapperDataSource.getDMWrapperById(wrapperId)
                ?: return CustomResult.Failure(IllegalArgumentException("DM Wrapper not found: $wrapperId"))

            // 2. 업데이트된 wrapper 생성 (필요한 필드만 수정)
            var updatedWrapper = currentWrapper

            // 실제 DMWrapper 클래스에 업데이트 메서드가 없으므로 새로 생성
            if (otherUserName != null || otherUserImageUrl != null || lastMessagePreview != null) {
                updatedWrapper = DMWrapper.fromDataSource(
                    id = currentWrapper.id,
                    dmChannelId = currentWrapper.dmChannelId,
                    currentUserId = currentWrapper.currentUserId,
                    otherUserId = currentWrapper.otherUserId,
                    otherUserName = otherUserName ?: currentWrapper.otherUserName,
                    otherUserImageUrl = otherUserImageUrl ?: currentWrapper.otherUserImageUrl,
                    lastMessagePreview = lastMessagePreview ?: currentWrapper.lastMessagePreview,
                    createdAt = currentWrapper.createdAt,
                    updatedAt = com.example.core_common.util.DateTimeUtil.nowInstant()
                )
            }

            // 3. 저장 (Outbox 포함)
            saveDMWrapper(updatedWrapper)

            Log.d(TAG, "DM Wrapper updated: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateDMWrapper failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateDMWrapperOtherUser(
        wrapperId: String,
        newOtherUserId: UserId
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "updateDMWrapperOtherUser: wrapperId=$wrapperId, newOtherUserId=$newOtherUserId"
            )

            // 1. 현재 wrapper 조회
            val currentWrapper = localDmWrapperDataSource.getDMWrapperById(wrapperId)
                ?: return CustomResult.Failure(IllegalArgumentException("DM Wrapper not found: $wrapperId"))

            // 2. 다른 사용자 변경
            val updatedWrapper = currentWrapper.changeOtherUser(newOtherUserId)

            // 3. 저장 (Outbox 포함)
            saveDMWrapper(updatedWrapper)

            Log.d(TAG, "DM Wrapper other user updated: $wrapperId -> $newOtherUserId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateDMWrapperOtherUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateLastMessagePreview(
        wrapperId: String,
        lastMessagePreview: DMChannelLastMessagePreview?
    ): CustomResult<Unit, Exception> {
        return updateDMWrapper(wrapperId, null, null, lastMessagePreview)
    }

    // === 유틸리티 ===

    override suspend fun dmWrapperExists(wrapperId: String): Boolean {
        return try {
            localDmWrapperDataSource.dmWrapperExists(wrapperId)
        } catch (e: Exception) {
            Log.e(TAG, "dmWrapperExists failed", e)
            false
        }
    }

    override suspend fun dmWrapperExistsWithOtherUser(otherUserId: String): Boolean {
        return try {
            localDmWrapperDataSource.dmWrapperExistsWithOtherUser(otherUserId)
        } catch (e: Exception) {
            Log.e(TAG, "dmWrapperExistsWithOtherUser failed", e)
            false
        }
    }

    override suspend fun getDMWrapperCountByUser(currentUserId: String): Int {
        return try {
            localDmWrapperDataSource.getDMWrapperCount(currentUserId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrapperCountByUser failed", e)
            0
        }
    }

    override suspend fun getTotalDMWrapperCount(): Int {
        return try {
            localDmWrapperDataSource.getTotalDMWrapperCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalDMWrapperCount failed", e)
            0
        }
    }

    override suspend fun getDMWrapperCountWithRecentMessages(): Int {
        return try {
            localDmWrapperDataSource.getDMWrapperCountWithRecentMessages()
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrapperCountWithRecentMessages failed", e)
            0
        }
    }

    override suspend fun clearAllDMWrappers(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllDMWrappers")

            localDmWrapperDataSource.clearAllDMWrappers()

            Log.d(TAG, "All DM wrappers cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllDMWrappers failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getDMWrappersUpdatedAfter(timestamp: Instant): List<DMWrapper> {
        return try {
            localDmWrapperDataSource.getDMWrappersUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getDMWrappersUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        wrapperId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: wrapperId=$wrapperId, operation=$operation")

            localDmWrapperDataSource.addToOutbox(wrapperId, operation, payload)

            Log.d(TAG, "Added to outbox: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}