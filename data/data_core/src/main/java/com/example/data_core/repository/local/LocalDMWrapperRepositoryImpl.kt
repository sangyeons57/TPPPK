package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalDMWrapperDataSource
import com.example.data_core.repository.local.base.BaseLocalRepositoryImpl
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
 * Local DMWrapper Repository Implementation (SSOT)
 * BaseLocalRepositoryImpl 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - BaseLocalRepositoryImpl의 공통 CRUD 기능 상속 (80%)
 * - DMWrapper 도메인 특화 기능만 구현 (20%)
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 메서드 구현:
 * - observeEntityById -> observeDMWrapperById로 위임
 * - observeAllEntities -> observeAllDMWrappers로 위임
 * - observeEntityUpdatedAt -> observeDMWrapperUpdatedAt로 위임
 * - getEntityById -> getDMWrapperById로 위임
 * - getEntitiesByIds -> getDMWrappersByIds로 위임
 * - getAllEntities -> getAllDMWrappers로 위임
 * - saveEntity -> saveDMWrapper로 위임
 * - saveEntities -> saveDMWrappers로 위임
 * - deleteEntity -> deleteDMWrapper로 위임
 * - Plus SyncableRepository methods
 */
@Singleton
class LocalDMWrapperRepositoryImpl @Inject constructor(
    private val localDmWrapperDataSource: LocalDMWrapperDataSource
) : BaseLocalRepositoryImpl<DMWrapper>(), LocalDMWrapperRepository {

    companion object {
        private const val TAG = "LocalDMWrapperRepository"
    }

    // === BaseLocalRepository 메서드 구현 (도메인 특화 메서드로 위임) ===

    override fun observeEntityById(entityId: String): Flow<DMWrapper?> = 
        observeDMWrapperById(entityId)

    override fun observeAllEntities(): Flow<List<DMWrapper>> = 
        observeAllDMWrappers()

    override fun observeEntityUpdatedAt(entityId: String): Flow<Long?> = 
        observeDMWrapperUpdatedAt(entityId)

    override suspend fun getEntityById(entityId: String): CustomResult<DMWrapper?, Exception> = 
        handleOperation("getDMWrapperById($entityId)", TAG) {
            getDMWrapperById(entityId)
        }

    override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<DMWrapper>, Exception> = 
        handleOperation("getDMWrappersByIds(${entityIds.size})", TAG) {
            getDMWrappersByIds(entityIds)
        }

    override suspend fun getAllEntities(limit: Int?): CustomResult<List<DMWrapper>, Exception> = 
        handleOperation("getAllDMWrappers($limit)", TAG) {
            getAllDMWrappers(limit)
        }

    override suspend fun saveEntity(entity: DMWrapper): CustomResult<Unit, Exception> = 
        saveDMWrapper(entity)

    override suspend fun saveEntities(entities: List<DMWrapper>): CustomResult<Unit, Exception> = 
        saveDMWrappers(entities)

    override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception> = 
        deleteDMWrapper(entityId)

    override suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<DMWrapper>, Exception> = 
        handleOperation("getDMWrappersUpdatedAfter($timestamp)", TAG) {
            getDMWrappersUpdatedAfter(timestamp)
        }

    override suspend fun clearAllEntities(): CustomResult<Unit, Exception> = 
        clearAllDMWrappers()

    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception> = 
        handleOperation("getTotalDMWrapperCount", TAG) {
            getTotalDMWrapperCount()
        }

    override suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception> = 
        handleOperation("dmWrapperExists($entityId)", TAG) {
            dmWrapperExists(entityId)
        }

    override suspend fun addToOutbox(
        entityId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return handleOperation("addToOutbox($entityId, $operation)", TAG) {
            localDmWrapperDataSource.addToOutbox(entityId, operation, payload)
        }
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeDMWrapperById(wrapperId: String): Flow<DMWrapper?> {
        logDebug("observeDMWrapperById: $wrapperId", TAG)
        return localDmWrapperDataSource.observeDMWrapperById(wrapperId)
    }

    override fun observeDMWrappersByUser(currentUserId: String): Flow<List<DMWrapper>> {
        logDebug("observeDMWrappersByUser: $currentUserId", TAG)
        return localDmWrapperDataSource.observeDMWrappersByUser(currentUserId)
    }

    override fun observeDMWrapperByOtherUser(otherUserId: String): Flow<DMWrapper?> {
        logDebug("observeDMWrapperByOtherUser: $otherUserId", TAG)
        return localDmWrapperDataSource.observeDMWrapperByOtherUser(otherUserId)
    }

    override fun observeDMWrappersByUserName(userName: String, limit: Int): Flow<List<DMWrapper>> {
        logDebug("observeDMWrappersByUserName: userName='$userName', limit=$limit", TAG)
        return localDmWrapperDataSource.observeDMWrappersByUserName(userName, limit)
    }

    override fun observeAllDMWrappers(): Flow<List<DMWrapper>> {
        logDebug("observeAllDMWrappers", TAG)
        return localDmWrapperDataSource.observeAllDMWrappers()
    }

    override fun observeDMWrapperUpdatedAt(wrapperId: String): Flow<Long?> {
        logDebug("observeDMWrapperUpdatedAt: $wrapperId", TAG)
        return localDmWrapperDataSource.observeDMWrapperUpdatedAt(wrapperId)
    }

    override fun observeDMWrappersWithRecentMessages(): Flow<List<DMWrapper>> {
        logDebug("observeDMWrappersWithRecentMessages", TAG)
        return localDmWrapperDataSource.observeDMWrappersWithRecentMessages()
    }

    override fun observeDMWrappers(wrapperIds: List<String>): Flow<List<DMWrapper>> {
        logDebug("observeDMWrappers: ${wrapperIds.size} wrappers", TAG)
        return localDmWrapperDataSource.observeDMWrappers(wrapperIds)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getDMWrapperById(wrapperId: String): DMWrapper? {
        logDebug("getDMWrapperById: $wrapperId", TAG)
        return try {
            localDmWrapperDataSource.getDMWrapperById(wrapperId)
        } catch (e: Exception) {
            logError("getDMWrapperById failed", e, TAG)
            null
        }
    }

    override suspend fun getDMWrappersByUser(currentUserId: String): List<DMWrapper> {
        logDebug("getDMWrappersByUser: $currentUserId", TAG)
        return try {
            localDmWrapperDataSource.getDMWrappersByUser(currentUserId)
        } catch (e: Exception) {
            logError("getDMWrappersByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrapperByOtherUser(otherUserId: String): DMWrapper? {
        logDebug("getDMWrapperByOtherUser: $otherUserId")
        return try {
            localDmWrapperDataSource.getDMWrapperByOtherUser(otherUserId)
        } catch (e: Exception) {
            logError("getDMWrapperByOtherUser failed", e)
            null
        }
    }

    override suspend fun searchDMWrappersByUserName(userName: String, limit: Int): List<DMWrapper> {
        logDebug("searchDMWrappersByUserName: userName='$userName', limit=$limit")
        return try {
            localDmWrapperDataSource.searchDMWrappersByUserName(userName, limit)
        } catch (e: Exception) {
            logError("searchDMWrappersByUserName failed", e)
            emptyList()
        }
    }

    override suspend fun getAllDMWrappers(limit: Int?): List<DMWrapper> {
        logDebug("getAllDMWrappers: limit=$limit")
        return try {
            localDmWrapperDataSource.getAllDMWrappers()
        } catch (e: Exception) {
            logError("getAllDMWrappers failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrappersByIds(wrapperIds: List<String>): List<DMWrapper> {
        logDebug("getDMWrappersByIds: ${wrapperIds.size} wrappers")
        return try {
            localDmWrapperDataSource.getDMWrappersByIds(wrapperIds)
        } catch (e: Exception) {
            logError("getDMWrappersByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrappersWithRecentMessages(): List<DMWrapper> {
        logDebug("getDMWrappersWithRecentMessages")
        return try {
            localDmWrapperDataSource.getDMWrappersWithRecentMessages()
        } catch (e: Exception) {
            logError("getDMWrappersWithRecentMessages failed", e)
            emptyList()
        }
    }

    override suspend fun getDMWrappersByOtherUsers(otherUserIds: List<String>): List<DMWrapper> {
        logDebug("getDMWrappersByOtherUsers: ${otherUserIds.size} users")
        return try {
            localDmWrapperDataSource.getDMWrappersByOtherUsers(otherUserIds)
        } catch (e: Exception) {
            logError("getDMWrappersByOtherUsers failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveDMWrapper(dmWrapper: DMWrapper): CustomResult<Unit, Exception> {
        return handleOperation("saveDMWrapper(${dmWrapper.id})", TAG) {
            // 1. Room DB에 저장
            localDmWrapperDataSource.saveDMWrapper(dmWrapper)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (dmWrapper.isNew) "CREATE" else "UPDATE"
            localDmWrapperDataSource.addToOutbox(
                wrapperId = dmWrapper.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )
        }
    }

    override suspend fun saveDMWrappers(dmWrappers: List<DMWrapper>): CustomResult<Unit, Exception> {
        return handleOperation("saveDMWrappers(${dmWrappers.size} wrappers)", TAG) {
            if (dmWrappers.isEmpty()) {
                return@handleOperation
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localDmWrapperDataSource.saveDMWrappers(dmWrappers)
        }
    }

    override suspend fun deleteDMWrapper(wrapperId: String): CustomResult<Unit, Exception> {
        return handleOperation("deleteDMWrapper($wrapperId)", TAG) {
            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localDmWrapperDataSource.deleteDMWrapper(wrapperId)

            // 2. Outbox에 삭제 작업 추가
            localDmWrapperDataSource.addToOutbox(
                wrapperId = wrapperId,
                operation = "DELETE",
                payload = null
            )
        }
    }

    override suspend fun deleteDMWrappersByUser(currentUserId: String): CustomResult<Unit, Exception> {
        return try {
            logDebug("deleteDMWrappersByUser: $currentUserId")

            localDmWrapperDataSource.deleteDMWrappersByUser(currentUserId)

            logDebug("DM Wrappers deleted for user: $currentUserId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError("deleteDMWrappersByUser failed", e)
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
            logDebug("updateDMWrapper: wrapperId=$wrapperId")

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

            logDebug("DM Wrapper updated: $wrapperId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError("updateDMWrapper failed", e)
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

            logDebug("DM Wrapper other user updated: $wrapperId -> $newOtherUserId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError("updateDMWrapperOtherUser failed", e)
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
            logError("dmWrapperExists failed", e)
            false
        }
    }

    override suspend fun dmWrapperExistsWithOtherUser(otherUserId: String): Boolean {
        return try {
            localDmWrapperDataSource.dmWrapperExistsWithOtherUser(otherUserId)
        } catch (e: Exception) {
            logError("dmWrapperExistsWithOtherUser failed", e)
            false
        }
    }

    override suspend fun getDMWrapperCountByUser(currentUserId: String): Int {
        return try {
            localDmWrapperDataSource.getDMWrapperCount(currentUserId)
        } catch (e: Exception) {
            logError("getDMWrapperCountByUser failed", e)
            0
        }
    }

    override suspend fun getTotalDMWrapperCount(): Int {
        return try {
            localDmWrapperDataSource.getTotalDMWrapperCount()
        } catch (e: Exception) {
            logError("getTotalDMWrapperCount failed", e)
            0
        }
    }

    override suspend fun getDMWrapperCountWithRecentMessages(): Int {
        return try {
            localDmWrapperDataSource.getDMWrapperCountWithRecentMessages()
        } catch (e: Exception) {
            logError("getDMWrapperCountWithRecentMessages failed", e)
            0
        }
    }

    override suspend fun clearAllDMWrappers(): CustomResult<Unit, Exception> {
        return handleOperation("clearAllDMWrappers", TAG) {
            localDmWrapperDataSource.clearAllDMWrappers()
        }
    }

    // === 동기화 지원 ===

    override suspend fun getDMWrappersUpdatedAfter(timestamp: Instant): List<DMWrapper> {
        return try {
            localDmWrapperDataSource.getDMWrappersUpdatedAfter(timestamp)
        } catch (e: Exception) {
            logError("getDMWrappersUpdatedAfter failed", e)
            emptyList()
        }
    }

    // Note: addToOutbox is already implemented above as a BaseLocalRepository method
}