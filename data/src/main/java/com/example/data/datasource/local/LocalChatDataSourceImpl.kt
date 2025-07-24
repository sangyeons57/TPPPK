package com.example.data.datasource.local

import android.util.Log
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.ChannelSyncDao
import com.example.data.mapper.ChatMessageMapper
import com.example.data.model.local.ChannelSyncEntity
import com.example.domain.model.base.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 채팅 데이터 저장소 구현체
 * Room Database를 사용하여 updateAt 기반 증분 동기화와 효율적인 캐싱을 제공
 */
@Singleton
class LocalChatDataSourceImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val channelSyncDao: ChannelSyncDao
) : LocalChatDataSource {

    override suspend fun getMessages(channelId: String, limit: Int): List<Message> {
        val entities = chatMessageDao.getLatestMessages(channelId, limit)
        return ChatMessageMapper.toDomainList(entities)
    }

    override suspend fun getMessagesAfter(channelId: String, timestamp: Instant): List<Message> {
        val entities = chatMessageDao.getUpdatedMessages(channelId, timestamp)
        return ChatMessageMapper.toDomainList(entities)
    }

    override suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int
    ): List<Message> {
        val entities = chatMessageDao.getMessagesBefore(channelId, beforeTimestamp, limit)
        return ChatMessageMapper.toDomainList(entities)
    }

    override suspend fun saveMessages(channelId: String, messages: List<Message>) {
        if (messages.isEmpty()) return

        // 메시지들을 Entity로 변환하여 저장 (REPLACE 전략으로 자동 upsert)
        val entities = ChatMessageMapper.toEntityList(messages, channelId)
        chatMessageDao.insertMessages(entities) // OnConflictStrategy.REPLACE로 자동 upsert

        Log.d(
            "LocalChatDataSource",
            "Upserted ${entities.size} messages for channel: $channelId (including updates/deletes)"
        )

        // 동기화 상태 정보 업데이트
        updateChannelStats(channelId)
    }

    override suspend fun saveMessage(channelId: String, message: Message) {
        val entity = ChatMessageMapper.toEntity(message, channelId)
        chatMessageDao.insertMessage(entity)

        // 동기화 상태 정보 업데이트
        updateChannelStats(channelId)
    }

    override fun observeMessages(channelId: String, limit: Int): Flow<List<Message>> {
        return chatMessageDao.observeMessages(channelId, limit).map { entities ->
            ChatMessageMapper.toDomainList(entities)
        }
    }

    override suspend fun getLastSyncTimestamp(channelId: String): Instant? {
        val syncInfo = channelSyncDao.getSyncInfo(channelId)
        return syncInfo?.lastSyncTimestamp
    }

    override suspend fun updateSyncTimestamp(channelId: String, timestamp: Instant) {
        val existingSyncInfo = channelSyncDao.getSyncInfo(channelId)

        if (existingSyncInfo != null) {
            // 기존 동기화 정보가 있으면 타임스탬프만 업데이트
            channelSyncDao.updateLastSyncTimestamp(channelId, timestamp, Instant.now())
        } else {
            // 새로운 동기화 정보 생성
            val newSyncInfo = ChannelSyncEntity(
                channelId = channelId,
                lastSyncTimestamp = timestamp,
                messageCount = chatMessageDao.getMessageCount(channelId),
                hasMoreOlderMessages = true,
                oldestMessageTimestamp = chatMessageDao.getOldestMessageTimestamp(channelId),
                newestMessageTimestamp = chatMessageDao.getNewestMessageTimestamp(channelId),
                lastSyncAt = Instant.now(),
                syncFailureCount = 0
            )
            channelSyncDao.insertSyncInfo(newSyncInfo)
        }
    }

    override suspend fun getSyncInfo(channelId: String): ChatSyncInfo? {
        val entity = channelSyncDao.getSyncInfo(channelId) ?: return null

        return ChatSyncInfo(
            channelId = entity.channelId,
            lastSyncTimestamp = entity.lastSyncTimestamp,
            messageCount = entity.messageCount,
            hasMoreOlderMessages = entity.hasMoreOlderMessages,
            oldestMessageTimestamp = entity.oldestMessageTimestamp,
            newestMessageTimestamp = entity.newestMessageTimestamp,
            syncFailureCount = entity.syncFailureCount
        )
    }

    override suspend fun updateSyncInfo(channelId: String, syncInfo: ChatSyncInfo) {
        val entity = ChannelSyncEntity(
            channelId = syncInfo.channelId,
            lastSyncTimestamp = syncInfo.lastSyncTimestamp,
            messageCount = syncInfo.messageCount,
            hasMoreOlderMessages = syncInfo.hasMoreOlderMessages,
            oldestMessageTimestamp = syncInfo.oldestMessageTimestamp,
            newestMessageTimestamp = syncInfo.newestMessageTimestamp,
            lastSyncAt = Instant.now(),
            syncFailureCount = syncInfo.syncFailureCount
        )

        channelSyncDao.insertSyncInfo(entity)
    }

    override suspend fun deleteOldMessages(channelId: String, keepCount: Int): Int {
        // 현재 메시지 개수 확인
        val currentCount = chatMessageDao.getMessageCount(channelId)

        if (currentCount <= keepCount) {
            return 0 // 삭제할 메시지가 없음
        }

        // 유지할 메시지들 중 가장 오래된 것의 타임스탬프 찾기
        val messagesToKeep = chatMessageDao.getLatestMessages(channelId, keepCount)
        val oldestToKeep = messagesToKeep.lastOrNull()?.createdAt

        return if (oldestToKeep != null) {
            val deletedCount = chatMessageDao.deleteOldMessages(channelId, oldestToKeep)

            // 동기화 상태 정보 업데이트
            updateChannelStats(channelId)

            deletedCount
        } else {
            0
        }
    }

    override suspend fun messageExists(messageId: String): Boolean {
        return chatMessageDao.messageExists(messageId)
    }

    override suspend fun clearChannel(channelId: String) {
        chatMessageDao.deleteAllMessages(channelId)
        channelSyncDao.deleteSyncInfo(channelId)
    }

    /**
     * 채널의 메시지 통계 정보를 업데이트
     * @param channelId 채널 ID
     */
    private suspend fun updateChannelStats(channelId: String) {
        val messageCount = chatMessageDao.getMessageCount(channelId)
        val oldestTimestamp = chatMessageDao.getOldestMessageTimestamp(channelId)
        val newestTimestamp = chatMessageDao.getNewestMessageTimestamp(channelId)

        val existingSyncInfo = channelSyncDao.getSyncInfo(channelId)

        if (existingSyncInfo != null) {
            // 기존 동기화 정보 업데이트
            channelSyncDao.updateMessageStats(
                channelId = channelId,
                messageCount = messageCount,
                oldestTimestamp = oldestTimestamp,
                newestTimestamp = newestTimestamp,
                syncAt = Instant.now()
            )
        } else {
            // 새로운 동기화 정보 생성
            val newSyncInfo = ChannelSyncEntity(
                channelId = channelId,
                lastSyncTimestamp = Instant.EPOCH, // 초기값
                messageCount = messageCount,
                hasMoreOlderMessages = true,
                oldestMessageTimestamp = oldestTimestamp,
                newestMessageTimestamp = newestTimestamp,
                lastSyncAt = Instant.now(),
                syncFailureCount = 0
            )
            channelSyncDao.insertSyncInfo(newSyncInfo)
        }
    }
}