package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalProjectChannelsDataSource
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelStatus
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.repository.local.LocalProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Project Channel Repository Implementation (SSOT)
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
class LocalProjectChannelRepositoryImpl @Inject constructor(
    private val localProjectChannelsDataSource: LocalProjectChannelsDataSource
) : LocalProjectChannelRepository {

    companion object {
        private const val TAG = "LocalProjectChannelRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeChannelById(channelId: String): Flow<ProjectChannel?> {
        Log.d(TAG, "observeChannelById: $channelId")
        return localProjectChannelsDataSource.observeChannelById(channelId)
    }

    override fun observeByName(name: Name): Flow<ProjectChannel?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return try {
            localProjectChannelsDataSource.observeAllChannels()
                .map { channels ->
                    channels.find { it.channelName.value == name.value }
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeByName failed", e)
            flow { emit(null) }
        }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return try {
            localProjectChannelsDataSource.observeAllChannels()
                .map { channels ->
                    channels.filter { it.channelName.value.contains(name, ignoreCase = true) }
                        .take(limit)
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeAllByName failed", e)
            flow { emit(emptyList()) }
        }
    }

    override fun observeChannelsByCategory(categoryId: String): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeChannelsByCategory: $categoryId")
        return localProjectChannelsDataSource.observeChannelsByCategory(categoryId)
    }

    override fun observeChannelsByType(channelType: ProjectChannelType): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeChannelsByType: $channelType")
        return try {
            localProjectChannelsDataSource.observeAllChannels()
                .map { channels ->
                    channels.filter { it.channelType == channelType }
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeChannelsByType failed", e)
            flow { emit(emptyList()) }
        }
    }

    override fun observeChannelsByStatus(status: ProjectChannelStatus): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeChannelsByStatus: $status")
        return try {
            localProjectChannelsDataSource.observeAllChannels()
                .map { channels ->
                    channels.filter { it.status == status }
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeChannelsByStatus failed", e)
            flow { emit(emptyList()) }
        }
    }

    override fun observeChannels(channelIds: List<String>): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeChannels: ${channelIds.size} channels")
        return try {
            localProjectChannelsDataSource.observeAllChannels()
                .map { channels ->
                    channels.filter { it.id.value in channelIds }
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeChannels failed", e)
            flow { emit(emptyList()) }
        }
    }

    override fun observeChannelUpdatedAt(channelId: String): Flow<Long?> {
        Log.d(TAG, "observeChannelUpdatedAt: $channelId")
        return try {
            localProjectChannelsDataSource.observeChannelById(channelId)
                .map { channel ->
                    channel?.updatedAt?.toEpochMilli()
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeChannelUpdatedAt failed", e)
            flow { emit(null) }
        }
    }

    override fun observeAllChannels(): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeAllChannels")
        return localProjectChannelsDataSource.observeAllChannels()
    }

    override fun observeChannelsByOrderRange(
        categoryId: String,
        minOrder: Int,
        maxOrder: Int
    ): Flow<List<ProjectChannel>> {
        Log.d(TAG, "observeChannelsByOrderRange: category=$categoryId, $minOrder-$maxOrder")
        return try {
            localProjectChannelsDataSource.observeChannelsByCategory(categoryId)
                .map { channels ->
                    channels.filter { it.order.value in minOrder..maxOrder }
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeChannelsByOrderRange failed", e)
            flow { emit(emptyList()) }
        }
    }

    // === 단순 읽기 작업 ===

    override suspend fun getChannelById(channelId: String): ProjectChannel? {
        Log.d(TAG, "getChannelById: $channelId")
        return try {
            localProjectChannelsDataSource.getChannelById(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelById failed", e)
            null
        }
    }

    override suspend fun getChannelByName(name: Name): ProjectChannel? {
        Log.d(TAG, "getChannelByName: ${name.value}")
        return try {
            localProjectChannelsDataSource.searchChannelsByName(name.value)
                .find { it.channelName.value == name.value }
        } catch (e: Exception) {
            Log.e(TAG, "getChannelByName failed", e)
            null
        }
    }

    override suspend fun searchChannelsByName(name: String, limit: Int): List<ProjectChannel> {
        Log.d(TAG, "searchChannelsByName: name='$name', limit=$limit")
        return try {
            localProjectChannelsDataSource.searchChannelsByName(name)
                .take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchChannelsByName failed", e)
            emptyList()
        }
    }

    override suspend fun getChannelsByIds(channelIds: List<String>): List<ProjectChannel> {
        Log.d(TAG, "getChannelsByIds: ${channelIds.size} channels")
        return try {
            channelIds.mapNotNull { channelId ->
                localProjectChannelsDataSource.getChannelById(channelId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllChannels(limit: Int?): List<ProjectChannel> {
        Log.d(TAG, "getAllChannels: limit=$limit")
        return try {
            val channels = localProjectChannelsDataSource.getAllChannels()
            if (limit != null) channels.take(limit) else channels
        } catch (e: Exception) {
            Log.e(TAG, "getAllChannels failed", e)
            emptyList()
        }
    }

    override suspend fun getChannelsByCategory(categoryId: String): List<ProjectChannel> {
        Log.d(TAG, "getChannelsByCategory: $categoryId")
        return try {
            localProjectChannelsDataSource.getChannelsByCategory(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsByCategory failed", e)
            emptyList()
        }
    }

    override suspend fun getChannelsByType(channelType: ProjectChannelType): List<ProjectChannel> {
        Log.d(TAG, "getChannelsByType: $channelType")
        return try {
            localProjectChannelsDataSource.getChannelsByType(channelType.name)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsByType failed", e)
            emptyList()
        }
    }

    override suspend fun getChannelsByStatus(status: ProjectChannelStatus): List<ProjectChannel> {
        Log.d(TAG, "getChannelsByStatus: $status")
        return try {
            localProjectChannelsDataSource.getChannelsByStatus(status.name)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getChannelsByOrderRange(
        categoryId: String,
        minOrder: Int,
        maxOrder: Int
    ): List<ProjectChannel> {
        Log.d(TAG, "getChannelsByOrderRange: category=$categoryId, $minOrder-$maxOrder")
        return try {
            localProjectChannelsDataSource.getChannelsByCategory(categoryId)
                .filter { it.order.value in minOrder..maxOrder }
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsByOrderRange failed", e)
            emptyList()
        }
    }

    override suspend fun getChannelsAfterOrder(
        categoryId: String,
        order: Int
    ): List<ProjectChannel> {
        Log.d(TAG, "getChannelsAfterOrder: category=$categoryId, order=$order")
        return try {
            localProjectChannelsDataSource.getChannelsAfterOrder(categoryId, order)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsAfterOrder failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveChannel(channel: ProjectChannel): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveChannel: ${channel.id}")

            // 1. Room DB에 저장
            localProjectChannelsDataSource.saveChannel(channel)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (channel.isNew) "CREATE" else "UPDATE"
            localProjectChannelsDataSource.addToOutbox(
                channelId = channel.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Channel saved and added to outbox: ${channel.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveChannels(channels: List<ProjectChannel>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveChannels: ${channels.size} channels")

            if (channels.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localProjectChannelsDataSource.saveChannels(channels)

            Log.d(TAG, "Bulk channels saved: ${channels.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveChannels failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteChannel(channelId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteChannel: $channelId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localProjectChannelsDataSource.deleteChannel(channelId)

            // 2. Outbox에 삭제 작업 추가
            localProjectChannelsDataSource.addToOutbox(
                channelId = channelId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Channel deleted and added to outbox: $channelId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateChannel(
        channelId: String,
        name: Name?,
        order: ProjectChannelOrder?,
        status: ProjectChannelStatus?,
        categoryId: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "updateChannel: channelId=$channelId, name=$name, order=$order, status=$status, categoryId=$categoryId"
            )

            // 1. 현재 채널 조회
            val currentChannel = localProjectChannelsDataSource.getChannelById(channelId)
                ?: return CustomResult.Failure(IllegalArgumentException("Channel not found: $channelId"))

            // 2. 업데이트된 채널 생성 (필요한 필드만 수정)
            var updatedChannel = currentChannel

            // Name 업데이트
            if (name != null && currentChannel.channelName != name) {
                updatedChannel.updateName(name)
            }

            // Order 업데이트
            if (order != null && currentChannel.order != order) {
                updatedChannel.changeOrder(order)
            }

            // Category 이동
            if (categoryId != null && currentChannel.categoryId.value != categoryId) {
                updatedChannel.moveToCategory(com.example.domain.model.vo.DocumentId(categoryId))
            }

            // Status 업데이트
            if (status != null && currentChannel.status != status) {
                updatedChannel = when (status) {
                    ProjectChannelStatus.ACTIVE -> updatedChannel.activate()
                    ProjectChannelStatus.ARCHIVED -> updatedChannel.archive()
                    ProjectChannelStatus.DISABLED -> updatedChannel.disable()
                    ProjectChannelStatus.DELETED -> updatedChannel.markDeleted()
                }
            }

            // 3. 저장 (Outbox 포함)
            saveChannel(updatedChannel)

            Log.d(TAG, "Channel updated: $channelId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun reorderChannels(channelOrderMap: Map<String, Int>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "reorderChannels: ${channelOrderMap.size} channels")

            // 각 채널의 순서를 업데이트
            for ((channelId, newOrder) in channelOrderMap) {
                updateChannel(channelId, null, ProjectChannelOrder(newOrder), null, null)
            }

            Log.d(TAG, "Channels reordered successfully")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "reorderChannels failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun moveChannelToCategory(
        channelId: String,
        newCategoryId: String,
        newOrder: ProjectChannelOrder?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "moveChannelToCategory: channelId=$channelId, newCategoryId=$newCategoryId, newOrder=$newOrder"
            )

            updateChannel(channelId, null, newOrder, null, newCategoryId)

            Log.d(TAG, "Channel moved to category successfully")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "moveChannelToCategory failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun channelExists(channelId: String): Boolean {
        return try {
            localProjectChannelsDataSource.channelExists(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "channelExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: Name, excludeChannelId: String?): Boolean {
        return try {
            val channels = localProjectChannelsDataSource.searchChannelsByName(name.value)
            channels.any { channel ->
                channel.channelName.value == name.value && channel.id.value != excludeChannelId
            }
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getTotalChannelCount(): Int {
        return try {
            localProjectChannelsDataSource.getChannelCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalChannelCount failed", e)
            0
        }
    }

    override suspend fun getChannelCountByCategory(categoryId: String): Int {
        return try {
            localProjectChannelsDataSource.getChannelCountByCategory(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelCountByCategory failed", e)
            0
        }
    }

    override suspend fun getChannelCountByType(channelType: ProjectChannelType): Int {
        return try {
            localProjectChannelsDataSource.getChannelsByType(channelType.name).size
        } catch (e: Exception) {
            Log.e(TAG, "getChannelCountByType failed", e)
            0
        }
    }

    override suspend fun getChannelCountByStatus(status: ProjectChannelStatus): Int {
        return try {
            localProjectChannelsDataSource.getChannelsByStatus(status.name).size
        } catch (e: Exception) {
            Log.e(TAG, "getChannelCountByStatus failed", e)
            0
        }
    }

    override suspend fun getNextChannelOrder(categoryId: String): Int {
        return try {
            localProjectChannelsDataSource.getNextOrderInCategory(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "getNextChannelOrder failed", e)
            1
        }
    }

    override suspend fun clearAllChannels(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllChannels")

            localProjectChannelsDataSource.clearAllChannels()

            Log.d(TAG, "All channels cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllChannels failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getChannelsUpdatedAfter(timestamp: Instant): List<ProjectChannel> {
        return try {
            localProjectChannelsDataSource.getChannelsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getChannelsUpdatedAfter failed", e)
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

            localProjectChannelsDataSource.addToOutbox(channelId, operation, payload)

            Log.d(TAG, "Added to outbox: $channelId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}