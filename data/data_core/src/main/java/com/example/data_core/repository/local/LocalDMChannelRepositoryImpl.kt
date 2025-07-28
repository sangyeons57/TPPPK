package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalDMChannelsDataSource
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.example.domain.model.vo.UserId
import com.example.domain.repository.local.LocalDMChannelRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local DM Channel Repository Implementation (SSOT)
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
class LocalDMChannelRepositoryImpl @Inject constructor(
    private val localDmChannelsDataSource: LocalDMChannelsDataSource
) : LocalDMChannelRepository {

    companion object {
        private const val TAG = "LocalDMChannelRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeDMChannelById(channelId: String): Flow<DMChannel?> {
        Log.d(TAG, "observeDMChannelById: $channelId")
        return localDmChannelsDataSource.observeDMChannelById(channelId)
    }

    override fun observeDMChannelsByUser(userId: String): Flow<List<DMChannel>> {
        Log.d(TAG, "observeDMChannelsByUser: $userId")
        return localDmChannelsDataSource.observeDMChannelsByUser(userId)
    }

    override fun observeDMChannelBetweenUsers(user1Id: String, user2Id: String): Flow<DMChannel?> {
        Log.d(TAG, "observeDMChannelBetweenUsers: $user1Id <-> $user2Id")
        return localDmChannelsDataSource.observeDMChannelBetweenUsers(user1Id, user2Id)
    }

    override fun observeDMChannelsByStatus(status: DMChannelStatus): Flow<List<DMChannel>> {
        Log.d(TAG, "observeDMChannelsByStatus: $status")
        return localDmChannelsDataSource.observeDMChannelsByStatus(status)
    }

    override fun observeDMChannelsByActiveStatus(isActive: Boolean): Flow<List<DMChannel>> {
        Log.d(TAG, "observeDMChannelsByActiveStatus: $isActive")
        return localDmChannelsDataSource.observeDMChannelsByActiveStatus(isActive)
    }

    override fun observeAllDMChannels(): Flow<List<DMChannel>> {
        Log.d(TAG, "observeAllDMChannels")
        return localDmChannelsDataSource.observeAllDMChannels()
    }

    override fun observeDMChannelUpdatedAt(channelId: String): Flow<Long?> {
        Log.d(TAG, "observeDMChannelUpdatedAt: $channelId")
        return localDmChannelsDataSource.observeDMChannelUpdatedAt(channelId)
    }

    override fun observeBlockedDMChannelsByUser(userId: String): Flow<List<DMChannel>> {
        Log.d(TAG, "observeBlockedDMChannelsByUser: $userId")
        return localDmChannelsDataSource.observeBlockedDMChannelsByUser(userId)
    }

    override fun observeCategories(categoryIds: List<String>): Flow<List<DMChannel>> {
        Log.d(TAG, "observeCategories: ${categoryIds.size} categories")
        return localDmChannelsDataSource.observeCategories(categoryIds)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getDMChannelById(channelId: String): DMChannel? {
        Log.d(TAG, "getDMChannelById: $channelId")
        return try {
            localDmChannelsDataSource.getDMChannelById(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelById failed", e)
            null
        }
    }

    override suspend fun getDMChannelsByUser(userId: String): List<DMChannel> {
        Log.d(TAG, "getDMChannelsByUser: $userId")
        return try {
            localDmChannelsDataSource.getDMChannelsByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelsByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getDMChannelBetweenUsers(user1Id: String, user2Id: String): DMChannel? {
        Log.d(TAG, "getDMChannelBetweenUsers: $user1Id <-> $user2Id")
        return try {
            localDmChannelsDataSource.getDMChannelBetweenUsers(user1Id, user2Id)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelBetweenUsers failed", e)
            null
        }
    }

    override suspend fun getDMChannelsByStatus(status: DMChannelStatus): List<DMChannel> {
        Log.d(TAG, "getDMChannelsByStatus: $status")
        return try {
            localDmChannelsDataSource.getDMChannelsByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getDMChannelsByActiveStatus(isActive: Boolean): List<DMChannel> {
        Log.d(TAG, "getDMChannelsByActiveStatus: $isActive")
        return try {
            localDmChannelsDataSource.getDMChannelsByActiveStatus(isActive)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelsByActiveStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getAllDMChannels(limit: Int?): List<DMChannel> {
        Log.d(TAG, "getAllDMChannels: limit=$limit")
        return try {
            localDmChannelsDataSource.getAllDMChannels()
        } catch (e: Exception) {
            Log.e(TAG, "getAllDMChannels failed", e)
            emptyList()
        }
    }

    override suspend fun getDMChannelsByIds(channelIds: List<String>): List<DMChannel> {
        Log.d(TAG, "getDMChannelsByIds: ${channelIds.size} channels")
        return try {
            localDmChannelsDataSource.getDMChannelsByIds(channelIds)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getBlockedDMChannelsByUser(userId: String): List<DMChannel> {
        Log.d(TAG, "getBlockedDMChannelsByUser: $userId")
        return try {
            localDmChannelsDataSource.getBlockedDMChannelsByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getBlockedDMChannelsByUser failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveDMChannel(dmChannel: DMChannel): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveDMChannel: ${dmChannel.id}")

            // 1. Room DB에 저장
            localDmChannelsDataSource.saveDMChannel(dmChannel)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (dmChannel.isNew) "CREATE" else "UPDATE"
            localDmChannelsDataSource.addToOutbox(
                channelId = dmChannel.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "DM Channel saved and added to outbox: ${dmChannel.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveDMChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveDMChannels(dmChannels: List<DMChannel>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveDMChannels: ${dmChannels.size} channels")

            if (dmChannels.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localDmChannelsDataSource.saveDMChannels(dmChannels)

            Log.d(TAG, "Bulk DM channels saved: ${dmChannels.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveDMChannels failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteDMChannel(channelId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteDMChannel: $channelId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localDmChannelsDataSource.deleteDMChannel(channelId)

            // 2. Outbox에 삭제 작업 추가
            localDmChannelsDataSource.addToOutbox(
                channelId = channelId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "DM Channel deleted and added to outbox: $channelId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteDMChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteDMChannelsByUser(userId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteDMChannelsByUser: $userId")

            localDmChannelsDataSource.deleteDMChannelsByUser(userId)

            Log.d(TAG, "DM Channels deleted for user: $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteDMChannelsByUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateDMChannelStatus(
        channelId: String,
        status: DMChannelStatus
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateDMChannelStatus: channelId=$channelId, status=$status")

            // 1. 현재 채널 조회
            val currentChannel = localDmChannelsDataSource.getDMChannelById(channelId)
                ?: return CustomResult.Failure(IllegalArgumentException("DM Channel not found: $channelId"))

            // 2. 상태별 업데이트 로직
            val updatedChannel = when (status) {
                DMChannelStatus.ACTIVE -> currentChannel.activate()
                DMChannelStatus.ARCHIVED -> currentChannel.archive()
                DMChannelStatus.BLOCKED -> currentChannel.block()
                DMChannelStatus.DELETED -> currentChannel.markDeleted()
            }

            // 3. 저장 (Outbox 포함)
            saveDMChannel(updatedChannel)

            Log.d(TAG, "DM Channel status updated: $channelId -> $status")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateDMChannelStatus failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun archiveDMChannel(channelId: String): CustomResult<Unit, Exception> {
        return updateDMChannelStatus(channelId, DMChannelStatus.ARCHIVED)
    }

    override suspend fun activateDMChannel(channelId: String): CustomResult<Unit, Exception> {
        return updateDMChannelStatus(channelId, DMChannelStatus.ACTIVE)
    }

    override suspend fun blockDMChannel(
        channelId: String,
        blockerUserId: String
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "blockDMChannel: channelId=$channelId, blockerUserId=$blockerUserId")

            // 1. 현재 채널 조회
            val currentChannel = localDmChannelsDataSource.getDMChannelById(channelId)
                ?: return CustomResult.Failure(IllegalArgumentException("DM Channel not found: $channelId"))

            // 2. 사용자별 차단 처리
            val blockerUser = UserId(blockerUserId)
            val updatedChannel = currentChannel.blockByUser(blockerUser)

            // 3. 저장 (Outbox 포함)
            saveDMChannel(updatedChannel)

            Log.d(TAG, "DM Channel blocked: $channelId by $blockerUserId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "blockDMChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun unblockDMChannel(
        channelId: String,
        unblockerUserId: String
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "unblockDMChannel: channelId=$channelId, unblockerUserId=$unblockerUserId")

            // 1. 현재 채널 조회
            val currentChannel = localDmChannelsDataSource.getDMChannelById(channelId)
                ?: return CustomResult.Failure(IllegalArgumentException("DM Channel not found: $channelId"))

            // 2. 사용자별 차단 해제 처리
            val unblockerUser = UserId(unblockerUserId)
            val updatedChannel = currentChannel.unblockByUser(unblockerUser)

            // 3. 저장 (Outbox 포함)
            saveDMChannel(updatedChannel)

            Log.d(TAG, "DM Channel unblocked: $channelId by $unblockerUserId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "unblockDMChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun dmChannelExists(channelId: String): Boolean {
        return try {
            localDmChannelsDataSource.dmChannelExists(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "dmChannelExists failed", e)
            false
        }
    }

    override suspend fun dmChannelExistsBetweenUsers(user1Id: String, user2Id: String): Boolean {
        return try {
            localDmChannelsDataSource.dmChannelExistsBetweenUsers(user1Id, user2Id)
        } catch (e: Exception) {
            Log.e(TAG, "dmChannelExistsBetweenUsers failed", e)
            false
        }
    }

    override suspend fun getDMChannelCountByUser(userId: String): Int {
        return try {
            localDmChannelsDataSource.getDMChannelCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelCountByUser failed", e)
            0
        }
    }

    override suspend fun getTotalDMChannelCount(): Int {
        return try {
            localDmChannelsDataSource.getTotalDMChannelCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalDMChannelCount failed", e)
            0
        }
    }

    override suspend fun getActiveDMChannelCount(): Int {
        return try {
            localDmChannelsDataSource.getActiveDMChannelCount()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveDMChannelCount failed", e)
            0
        }
    }

    override suspend fun getDMChannelCountByStatus(status: DMChannelStatus): Int {
        return try {
            localDmChannelsDataSource.getDMChannelCountByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelCountByStatus failed", e)
            0
        }
    }

    override suspend fun clearAllDMChannels(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllDMChannels")

            localDmChannelsDataSource.clearAllDMChannels()

            Log.d(TAG, "All DM channels cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllDMChannels failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getDMChannelsUpdatedAfter(timestamp: Instant): List<DMChannel> {
        return try {
            localDmChannelsDataSource.getDMChannelsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getDMChannelsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        channelId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: channelId=$channelId, operation=$operation")

            localDmChannelsDataSource.addToOutbox(channelId, operation, payload)

            Log.d(TAG, "Added to outbox: $channelId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}