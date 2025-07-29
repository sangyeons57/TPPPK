package com.example.data_core.repository

import com.example.data.cache.ChatCacheManager
import com.example.domain.model.base.Message
import com.example.domain.repository.base.ChatCacheRepository
import com.example.domain.repository.factory.context.ChatCacheRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/**
 * ChatCacheRepository 구현체
 * ChatCacheManager를 래핑하여 도메인 레이어에 필요한 인터페이스를 제공
 */
class ChatCacheRepositoryImpl @Inject constructor(
    private val chatCacheManager: ChatCacheManager,
    private val factoryContext: ChatCacheRepositoryFactoryContext
) : ChatCacheRepository {

    private val channelId: String by lazy {
        extractChannelId(factoryContext.collectionPath.value)
    }

    private fun extractChannelId(collectionPath: String): String {
        // For DM channels: "dm_channels/{dmChannelId}/messages"
        // For project channels: "projects/{projectId}/projectChannels/{channelId}/messages"
        val parts = collectionPath.split("/")
        return when {
            collectionPath.contains("dm_channels") -> parts[1] // dmChannelId
            collectionPath.contains("projectChannels") -> parts[3] // channelId
            else -> throw IllegalArgumentException("Unsupported collection path: $collectionPath")
        }
    }

    override suspend fun getMessagesWithSync(limit: Int): List<Message> {
        return chatCacheManager.getMessagesWithSync(channelId, limit)
    }

    override fun observeChannelMessages(limit: Int): Flow<List<Message>> {
        return chatCacheManager.observeChannelMessages(channelId, limit)
    }

    override suspend fun loadMoreMessages(
        beforeTimestamp: Instant,
        limit: Int
    ): List<Message> {
        return chatCacheManager.loadMoreMessages(channelId, beforeTimestamp, limit)
    }

    override suspend fun addRealtimeMessage(message: Message) {
        chatCacheManager.addRealtimeMessage(channelId, message)
    }

    override suspend fun updateRealtimeMessage(message: Message) {
        chatCacheManager.updateRealtimeMessage(channelId, message)
    }

    override suspend fun syncChannelIncremental(forceSync: Boolean) {
        chatCacheManager.syncChannelIncremental(channelId, forceSync)
    }

    override suspend fun syncChannelFull(initialLimit: Int) {
        chatCacheManager.syncChannelFull(channelId, initialLimit)
    }

    override suspend fun clearChannelCache() {
        chatCacheManager.clearChannelCache(channelId)
    }
}