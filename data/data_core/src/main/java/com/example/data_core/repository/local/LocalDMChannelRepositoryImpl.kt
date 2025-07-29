package com.example.data_core.repository.local

import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalDMChannelsDataSource
import com.example.data_core.repository.local.base.BaseLocalRepositoryImpl
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.UserId
import com.example.domain.repository.infrastructure.OutboxRepository
import com.example.domain.repository.local.LocalDMChannelRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local DMChannel Repository Implementation (SSOT)
 * BaseLocalRepositoryImpl 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - BaseLocalRepositoryImpl의 공통 CRUD 기능 상속 (80%)
 * - DMChannel 도메인 특화 기능만 구현 (20%)
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 메서드 구현:
 * - observeEntityById -> observeDMChannelById로 위임
 * - observeAllEntities -> observeAllDMChannels로 위임
 * - observeEntityUpdatedAt -> observeDMChannelUpdatedAt로 위임
 * - getEntityById -> getDMChannelById로 위임
 * - getEntitiesByIds -> getDMChannelsByIds로 위임
 * - getAllEntities -> getAllDMChannels로 위임
 * - saveEntity -> saveDMChannel로 위임
 * - saveEntities -> saveDMChannels로 위임
 * - deleteEntity -> deleteDMChannel로 위임
 * - Plus SyncableRepository methods
 */
@Singleton
class LocalDMChannelRepositoryImpl @Inject constructor(
    private val localDmChannelsDataSource: LocalDMChannelsDataSource,
    private val outboxRepository: OutboxRepository
) : BaseLocalRepositoryImpl<DMChannel>(), LocalDMChannelRepository {

    override lateinit var collectionPath: CollectionPath
    companion object {
        private const val TAG = "LocalDMChannelRepository"
        private const val COLLECTION_NAME = "dm_channels"
    }

    // === BaseLocalRepository 메서드 구현 (도메인 특화 메서드로 위임) ===

    override fun observeEntityById(entityId: String): Flow<DMChannel?> = 
        observeDMChannelById(entityId)

    override fun observeAllEntities(): Flow<List<DMChannel>> = 
        observeAllDMChannels()

    override fun observeEntityUpdatedAt(entityId: String): Flow<Long?> = 
        observeDMChannelUpdatedAt(entityId)

    override suspend fun getEntityById(entityId: String): CustomResult<DMChannel?, Exception> = 
        handleOperation("getDMChannelById($entityId)", TAG) {
            getDMChannelById(entityId)
        }

    override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<DMChannel>, Exception> = 
        handleOperation("getDMChannelsByIds(${entityIds.size})", TAG) {
            getDMChannelsByIds(entityIds)
        }

    override suspend fun getAllEntities(limit: Int?): CustomResult<List<DMChannel>, Exception> = 
        handleOperation("getAllDMChannels($limit)", TAG) {
            getAllDMChannels(limit)
        }

    override suspend fun saveEntity(entity: DMChannel): CustomResult<Unit, Exception> = 
        saveDMChannel(entity)

    override suspend fun saveEntities(entities: List<DMChannel>): CustomResult<Unit, Exception> = 
        saveDMChannels(entities)

    override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception> = 
        deleteDMChannel(entityId)

    override suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<DMChannel>, Exception> = 
        handleOperation("getDMChannelsUpdatedAfter($timestamp)", TAG) {
            getDMChannelsUpdatedAfter(timestamp)
        }

    override suspend fun clearAllEntities(): CustomResult<Unit, Exception> = 
        clearAllDMChannels()

    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception> = 
        handleOperation("getTotalDMChannelCount", TAG) {
            getTotalDMChannelCount()
        }

    override suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception> = 
        handleOperation("dmChannelExists($entityId)", TAG) {
            dmChannelExists(entityId)
        }

    // === BaseLocalRepositoryImpl 추상 메서드 구현 ===

    override suspend fun getTotalEntityCountInternal(): Int {
        return getTotalDMChannelCount()
    }

    override suspend fun entityExistsInternal(entityId: String): Boolean {
        return dmChannelExists(entityId)
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeDMChannelById(channelId: String): Flow<DMChannel?> {
        logDebug("observeDMChannelById: $channelId", TAG)
        return localDmChannelsDataSource.observeDMChannelById(channelId)
    }

    override fun observeDMChannelsByUser(userId: String): Flow<List<DMChannel>> {
        logDebug("observeDMChannelsByUser: $userId", TAG)
        return localDmChannelsDataSource.observeDMChannelsByUser(userId)
    }

    override fun observeDMChannelBetweenUsers(user1Id: String, user2Id: String): Flow<DMChannel?> {
        logDebug("observeDMChannelBetweenUsers: $user1Id <-> $user2Id", TAG)
        return localDmChannelsDataSource.observeDMChannelBetweenUsers(user1Id, user2Id)
    }

    override fun observeDMChannelsByStatus(status: DMChannelStatus): Flow<List<DMChannel>> {
        logDebug("observeDMChannelsByStatus: $status", TAG)
        return localDmChannelsDataSource.observeDMChannelsByStatus(status)
    }

    override fun observeDMChannelsByActiveStatus(isActive: Boolean): Flow<List<DMChannel>> {
        logDebug("observeDMChannelsByActiveStatus: $isActive", TAG)
        return localDmChannelsDataSource.observeDMChannelsByActiveStatus(isActive)
    }

    override fun observeAllDMChannels(): Flow<List<DMChannel>> {
        logDebug("observeAllDMChannels", TAG)
        return localDmChannelsDataSource.observeAllDMChannels()
    }

    override fun observeDMChannelUpdatedAt(channelId: String): Flow<Long?> {
        logDebug("observeDMChannelUpdatedAt: $channelId", TAG)
        return localDmChannelsDataSource.observeDMChannelUpdatedAt(channelId)
    }

    override fun observeBlockedDMChannelsByUser(userId: String): Flow<List<DMChannel>> {
        logDebug("observeBlockedDMChannelsByUser: $userId", TAG)
        return localDmChannelsDataSource.observeBlockedDMChannelsByUser(userId)
    }

    override fun observeCategories(categoryIds: List<String>): Flow<List<DMChannel>> {
        logDebug("observeCategories: ${categoryIds.size} categories", TAG)
        return localDmChannelsDataSource.observeCategories(categoryIds)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getDMChannelById(channelId: String): DMChannel? {
        logDebug("getDMChannelById: $channelId", TAG)
        return try {
            localDmChannelsDataSource.getDMChannelById(channelId)
        } catch (e: Exception) {
            logError("getDMChannelById failed", e, TAG)
            null
        }
    }

    override suspend fun getDMChannelsByUser(userId: String): List<DMChannel> {
        logDebug("getDMChannelsByUser: $userId", TAG)
        return try {
            localDmChannelsDataSource.getDMChannelsByUser(userId)
        } catch (e: Exception) {
            logError("getDMChannelsByUser failed", e, TAG)
            emptyList()
        }
    }

    override suspend fun getDMChannelBetweenUsers(user1Id: String, user2Id: String): DMChannel? {
        logDebug("getDMChannelBetweenUsers: $user1Id <-> $user2Id", TAG)
        return try {
            localDmChannelsDataSource.getDMChannelBetweenUsers(user1Id, user2Id)
        } catch (e: Exception) {
            logError("getDMChannelBetweenUsers failed", e, TAG)
            null
        }
    }

    override suspend fun getDMChannelsByStatus(status: DMChannelStatus): List<DMChannel> {
        logDebug("getDMChannelsByStatus: $status", TAG)
        return try {
            localDmChannelsDataSource.getDMChannelsByStatus(status)
        } catch (e: Exception) {
            logError("getDMChannelsByStatus failed", e, TAG)
            emptyList()
        }
    }

    override suspend fun getDMChannelsByActiveStatus(isActive: Boolean): List<DMChannel> {
        logDebug("getDMChannelsByActiveStatus: $isActive", TAG)
        return try {
            localDmChannelsDataSource.getDMChannelsByActiveStatus(isActive)
        } catch (e: Exception) {
            logError("getDMChannelsByActiveStatus failed", e, TAG)
            emptyList()
        }
    }

    override suspend fun getAllDMChannels(limit: Int?): List<DMChannel> {
        logDebug("getAllDMChannels: limit=$limit", TAG)
        return try {
            localDmChannelsDataSource.getAllDMChannels()
        } catch (e: Exception) {
            logError("getAllDMChannels failed", e, TAG)
            emptyList()
        }
    }

    override suspend fun getDMChannelsByIds(channelIds: List<String>): List<DMChannel> {
        logDebug("getDMChannelsByIds: ${channelIds.size} channels", TAG)
        return try {
            localDmChannelsDataSource.getDMChannelsByIds(channelIds)
        } catch (e: Exception) {
            logError("getDMChannelsByIds failed", e, TAG)
            emptyList()
        }
    }

    override suspend fun getBlockedDMChannelsByUser(userId: String): List<DMChannel> {
        logDebug("getBlockedDMChannelsByUser: $userId", TAG)
        return try {
            localDmChannelsDataSource.getBlockedDMChannelsByUser(userId)
        } catch (e: Exception) {
            logError("getBlockedDMChannelsByUser failed", e, TAG)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveDMChannel(dmChannel: DMChannel): CustomResult<Unit, Exception> {
        return try {
            logDebug("saveDMChannel: ${dmChannel.id}", TAG)

            // 1. Room DB에 저장
            localDmChannelsDataSource.saveDMChannel(dmChannel)

            // 2. OutboxRepository를 통한 동기화 작업 추가
            val operation = if (dmChannel.isNew) "CREATE" else "UPDATE"
            val outboxResult = outboxRepository.enqueue(
                collectionName = COLLECTION_NAME,
                documentId = dmChannel.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            when (outboxResult) {
                is CustomResult.Success -> {
                    logDebug("DMChannel saved and added to outbox: ${dmChannel.id}", TAG)
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    logError("Failed to add DMChannel to outbox", outboxResult.error, TAG)
                    // DB 저장은 성공했지만 Outbox 추가 실패 - 경고만 출력하고 성공 처리
                    CustomResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            logError("saveDMChannel failed", e, TAG)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveDMChannels(dmChannels: List<DMChannel>): CustomResult<Unit, Exception> {
        return handleOperation("saveDMChannels(${dmChannels.size} channels)", TAG) {
            if (dmChannels.isEmpty()) {
                return@handleOperation
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localDmChannelsDataSource.saveDMChannels(dmChannels)
        }
    }

    override suspend fun deleteDMChannel(channelId: String): CustomResult<Unit, Exception> {
        return try {
            logDebug("deleteDMChannel: $channelId", TAG)

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localDmChannelsDataSource.deleteDMChannel(channelId)

            // 2. OutboxRepository를 통한 삭제 작업 추가
            val outboxResult = outboxRepository.enqueue(
                collectionName = COLLECTION_NAME,
                documentId = channelId,
                operation = "DELETE",
                payload = null
            )

            when (outboxResult) {
                is CustomResult.Success -> {
                    logDebug("DMChannel deleted and added to outbox: $channelId", TAG)
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    logError("Failed to add delete operation to outbox", outboxResult.error, TAG)
                    // DB 삭제는 성공했지만 Outbox 추가 실패 - 경고만 출력하고 성공 처리
                    CustomResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            logError("deleteDMChannel failed", e, TAG)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteDMChannelsByUser(userId: String): CustomResult<Unit, Exception> {
        return handleOperation("deleteDMChannelsByUser($userId)", TAG) {
            localDmChannelsDataSource.deleteDMChannelsByUser(userId)
        }
    }

    override suspend fun updateDMChannelStatus(
        channelId: String,
        status: DMChannelStatus
    ): CustomResult<Unit, Exception> {
        return handleOperation("updateDMChannelStatus(channelId=$channelId, status=$status)", TAG) {
            // 1. 현재 채널 조회
            val currentChannel = localDmChannelsDataSource.getDMChannelById(channelId)
                ?: throw IllegalArgumentException("DM Channel not found: $channelId")

            // 2. 상태별 업데이트 로직
            val updatedChannel = when (status) {
                DMChannelStatus.ACTIVE -> currentChannel.activate()
                DMChannelStatus.ARCHIVED -> currentChannel.archive()
                DMChannelStatus.BLOCKED -> currentChannel.block()
                DMChannelStatus.DELETED -> currentChannel.markDeleted()
            }

            // 3. 저장 (Outbox 포함)
            val result = saveDMChannel(updatedChannel)
            if (result is CustomResult.Failure) {
                throw result.exception
            }
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
        return handleOperation("blockDMChannel(channelId=$channelId, blockerUserId=$blockerUserId)", TAG) {
            // 1. 현재 채널 조회
            val currentChannel = localDmChannelsDataSource.getDMChannelById(channelId)
                ?: throw IllegalArgumentException("DM Channel not found: $channelId")

            // 2. 사용자별 차단 처리
            val blockerUser = UserId(blockerUserId)
            val updatedChannel = currentChannel.blockByUser(blockerUser)

            // 3. 저장 (Outbox 포함)
            val result = saveDMChannel(updatedChannel)
            if (result is CustomResult.Failure) {
                throw result.exception
            }
        }
    }

    override suspend fun unblockDMChannel(
        channelId: String,
        unblockerUserId: String
    ): CustomResult<Unit, Exception> {
        return handleOperation("unblockDMChannel(channelId=$channelId, unblockerUserId=$unblockerUserId)", TAG) {
            // 1. 현재 채널 조회
            val currentChannel = localDmChannelsDataSource.getDMChannelById(channelId)
                ?: throw IllegalArgumentException("DM Channel not found: $channelId")

            // 2. 사용자별 차단 해제 처리
            val unblockerUser = UserId(unblockerUserId)
            val updatedChannel = currentChannel.unblockByUser(unblockerUser)

            // 3. 저장 (Outbox 포함)
            val result = saveDMChannel(updatedChannel)
            if (result is CustomResult.Failure) {
                throw result.exception
            }
        }
    }

    // === 유틸리티 ===

    override suspend fun dmChannelExists(channelId: String): Boolean {
        return try {
            localDmChannelsDataSource.dmChannelExists(channelId)
        } catch (e: Exception) {
            logError("dmChannelExists failed", e, TAG)
            false
        }
    }

    override suspend fun dmChannelExistsBetweenUsers(user1Id: String, user2Id: String): Boolean {
        return try {
            localDmChannelsDataSource.dmChannelExistsBetweenUsers(user1Id, user2Id)
        } catch (e: Exception) {
            logError("dmChannelExistsBetweenUsers failed", e, TAG)
            false
        }
    }

    override suspend fun getDMChannelCountByUser(userId: String): Int {
        return try {
            localDmChannelsDataSource.getDMChannelCount(userId)
        } catch (e: Exception) {
            logError("getDMChannelCountByUser failed", e, TAG)
            0
        }
    }

    override suspend fun getTotalDMChannelCount(): Int {
        return try {
            localDmChannelsDataSource.getTotalDMChannelCount()
        } catch (e: Exception) {
            logError("getTotalDMChannelCount failed", e, TAG)
            0
        }
    }

    override suspend fun getActiveDMChannelCount(): Int {
        return try {
            localDmChannelsDataSource.getActiveDMChannelCount()
        } catch (e: Exception) {
            logError("getActiveDMChannelCount failed", e, TAG)
            0
        }
    }

    override suspend fun getDMChannelCountByStatus(status: DMChannelStatus): Int {
        return try {
            localDmChannelsDataSource.getDMChannelCountByStatus(status)
        } catch (e: Exception) {
            logError("getDMChannelCountByStatus failed", e, TAG)
            0
        }
    }

    override suspend fun clearAllDMChannels(): CustomResult<Unit, Exception> {
        return handleOperation("clearAllDMChannels", TAG) {
            localDmChannelsDataSource.clearAllDMChannels()
        }
    }

    // === 동기화 지원 ===

    override suspend fun getDMChannelsUpdatedAfter(timestamp: Instant): List<DMChannel> {
        return try {
            localDmChannelsDataSource.getDMChannelsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            logError("getDMChannelsUpdatedAfter failed", e, TAG)
            emptyList()
        }
    }

    // Note: addToOutbox is already implemented above as a BaseLocalRepository method
}