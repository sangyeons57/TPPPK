package com.example.data.cache

import android.util.Log
import com.example.domain.model.base.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub version of ChatCacheManager.
 *
 * ⚠ **TEMPORARY**: This is a quick stub to keep the project compiling after removing the
 * real cache implementation. It should be **replaced or deleted** once a proper local cache
 * solution is ready.
 */
@Singleton
class ChatCacheManager @Inject constructor() {

    private val TAG = "ChatCacheManagerStub"

    init {
        Log.w(TAG, "ChatCacheManager stub initialised – this is a temporary no-op implementation. Replace or remove when real cache is ready.")
    }

    private val channelStreams: MutableMap<String, MutableSharedFlow<List<Message>>> = mutableMapOf()

    private fun streamFor(channelId: String): MutableSharedFlow<List<Message>> =
        channelStreams.getOrPut(channelId) { MutableSharedFlow(replay = 1) }

    suspend fun getMessagesWithSync(channelId: String, limit: Int): List<Message> {
        Log.d(TAG, "getMessagesWithSync(channelId=$channelId, limit=$limit)")
        return emptyList()
    }

    fun observeChannelMessages(channelId: String, limit: Int): Flow<List<Message>> {
        Log.d(TAG, "observeChannelMessages(channelId=$channelId, limit=$limit)")
        return streamFor(channelId).asSharedFlow()
    }

    suspend fun loadMoreMessages(channelId: String, beforeTimestamp: Instant, limit: Int): List<Message> {
        Log.d(TAG, "loadMoreMessages(channelId=$channelId, before=$beforeTimestamp, limit=$limit)")
        return emptyList()
    }

    suspend fun addRealtimeMessage(channelId: String, message: Message) {
        Log.d(TAG, "addRealtimeMessage(channelId=$channelId, messageId=${message.id})")
        val stream = streamFor(channelId)
        val current = stream.replayCache.firstOrNull() ?: emptyList()
        stream.emit(current + message)
    }

    suspend fun updateRealtimeMessage(channelId: String, message: Message) {
        Log.d(TAG, "updateRealtimeMessage(channelId=$channelId, messageId=${message.id})")
        val stream = streamFor(channelId)
        val current = stream.replayCache.firstOrNull() ?: emptyList()
        val updated = current.map { if (it.id == message.id) message else it }
        stream.emit(updated)
    }

    suspend fun syncChannelIncremental(channelId: String, forceSync: Boolean) {
        Log.d(TAG, "syncChannelIncremental(channelId=$channelId, forceSync=$forceSync) – no-op")
    }

    suspend fun syncChannelFull(channelId: String, initialLimit: Int) {
        Log.d(TAG, "syncChannelFull(channelId=$channelId, initialLimit=$initialLimit) – no-op")
    }

    suspend fun clearChannelCache(channelId: String) {
        Log.d(TAG, "clearChannelCache(channelId=$channelId)")
        channelStreams.remove(channelId)
    }

    suspend fun clearAllCache() {
        Log.d(TAG, "clearAllCache()")
        channelStreams.clear()
    }
} 