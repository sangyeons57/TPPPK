package com.example.domain.repository

import com.example.domain.model.Channel
import com.example.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID

/**
 * ChannelRepository의 테스트 구현체입니다.
 * 인메모리 데이터 저장소를 사용하여 실제 데이터베이스를 사용하지 않고 테스트할 수 있도록 합니다.
 */
class FakeChannelRepository(
    private val dispatcher: CoroutineDispatcher
) : ChannelRepository {
    
    // 인메모리 데이터 저장소
    private val channels = mutableMapOf<String, Channel>()
    private val messages = mutableMapOf<String, MutableList<ChatMessage>>()
    private val channelsFlow = MutableStateFlow<Map<String, Channel>>(emptyMap())
    private val messagesFlow = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    
    // 테스트 유틸리티 메서드 (ChannelRepository에는 없는 메서드)
    fun saveChannel(channel: Channel) {
        channels[channel.id] = channel
        channelsFlow.value = channels.toMap()
        
        if (!messagesFlow.containsKey(channel.id)) {
            messagesFlow[channel.id] = MutableStateFlow(emptyList())
        }
    }
    
    override fun getChannel(channelId: String): Result<Channel> {
        return channels[channelId]?.let {
            Result.success(it)
        } ?: Result.failure(NoSuchElementException("Channel not found: $channelId"))
    }
    
    override fun getUserChannels(userId: String): Flow<Result<List<Channel>>> = flow {
        emit(Result.success(
            channels.values.filter { channel ->
                channel.participantIds.contains(userId)
            }.toList()
        ))
    }.flowOn(dispatcher)
    
    override fun getProjectChannels(projectId: String): Flow<Result<List<Channel>>> = flow {
        emit(Result.success(
            channels.values.filter { channel ->
                channel.metadata?.get("projectId") == projectId
            }.toList()
        ))
    }.flowOn(dispatcher)
    
    override fun getChannelMessages(channelId: String): Flow<Result<List<ChatMessage>>> {
        val flow = messagesFlow[channelId] ?: MutableStateFlow(emptyList())
        return flow.map { Result.success(it) }.flowOn(dispatcher)
    }
    
    override fun sendMessage(message: ChatMessage): Result<Unit> {
        if (!channels.containsKey(message.channelId)) {
            return Result.failure(NoSuchElementException("Channel not found: ${message.channelId}"))
        }
        
        val channelMessages = messages.getOrPut(message.channelId) { mutableListOf() }
        channelMessages.add(message)
        
        // 메시지 Flow 업데이트
        messagesFlow.getOrPut(message.channelId) { MutableStateFlow(emptyList()) }
            .value = channelMessages.toList()
        
        // 채널의 마지막 메시지 정보 업데이트
        val channel = channels[message.channelId]
        channel?.let { 
            channels[message.channelId] = it.copy(
                lastMessagePreview = message.text,
                lastMessageTimestamp = message.timestamp,
                updatedAt = LocalDateTime.now()
            )
            channelsFlow.value = channels.toMap()
        }
        
        return Result.success(Unit)
    }
    
    override fun editMessage(channelId: String, messageId: String, newText: String): Result<Unit> {
        if (!channels.containsKey(channelId)) {
            return Result.failure(NoSuchElementException("Channel not found: $channelId"))
        }
        
        val channelMessages = messages[channelId] ?: return Result.failure(
            NoSuchElementException("No messages in channel: $channelId")
        )
        
        val messageIndex = channelMessages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) {
            return Result.failure(NoSuchElementException("Message not found: $messageId"))
        }
        
        val oldMessage = channelMessages[messageIndex]
        val updatedMessage = oldMessage.copy(
            text = newText,
            isEdited = true
        )
        
        channelMessages[messageIndex] = updatedMessage
        messagesFlow[channelId]?.value = channelMessages.toList()
        
        return Result.success(Unit)
    }
    
    override fun deleteMessage(channelId: String, messageId: String): Result<Unit> {
        if (!channels.containsKey(channelId)) {
            return Result.failure(NoSuchElementException("Channel not found: $channelId"))
        }
        
        val channelMessages = messages[channelId] ?: return Result.failure(
            NoSuchElementException("No messages in channel: $channelId")
        )
        
        val messageIndex = channelMessages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) {
            return Result.failure(NoSuchElementException("Message not found: $messageId"))
        }
        
        val oldMessage = channelMessages[messageIndex]
        val deletedMessage = oldMessage.copy(
            isDeleted = true
        )
        
        channelMessages[messageIndex] = deletedMessage
        messagesFlow[channelId]?.value = channelMessages.toList()
        
        return Result.success(Unit)
    }
    
    override fun addReaction(channelId: String, messageId: String, userId: String, reaction: String): Result<Unit> {
        if (!channels.containsKey(channelId)) {
            return Result.failure(NoSuchElementException("Channel not found: $channelId"))
        }
        
        val channelMessages = messages[channelId] ?: return Result.failure(
            NoSuchElementException("No messages in channel: $channelId")
        )
        
        val messageIndex = channelMessages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) {
            return Result.failure(NoSuchElementException("Message not found: $messageId"))
        }
        
        val oldMessage = channelMessages[messageIndex]
        val reactions = oldMessage.reactions.toMutableMap()
        
        // 기존 리액션 목록에 유저 ID 추가
        val users = reactions[reaction]?.toMutableList() ?: mutableListOf()
        if (!users.contains(userId)) {
            users.add(userId)
        }
        reactions[reaction] = users
        
        val updatedMessage = oldMessage.copy(
            reactions = reactions
        )
        
        channelMessages[messageIndex] = updatedMessage
        messagesFlow[channelId]?.value = channelMessages.toList()
        
        return Result.success(Unit)
    }
    
    override fun removeReaction(channelId: String, messageId: String, userId: String, reaction: String): Result<Unit> {
        if (!channels.containsKey(channelId)) {
            return Result.failure(NoSuchElementException("Channel not found: $channelId"))
        }
        
        val channelMessages = messages[channelId] ?: return Result.failure(
            NoSuchElementException("No messages in channel: $channelId")
        )
        
        val messageIndex = channelMessages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) {
            return Result.failure(NoSuchElementException("Message not found: $messageId"))
        }
        
        val oldMessage = channelMessages[messageIndex]
        val reactions = oldMessage.reactions.toMutableMap()
        
        // 리액션에서 유저 ID 제거
        val users = reactions[reaction]?.toMutableList()
        users?.remove(userId)
        
        // 유저가 없으면 리액션 제거, 있으면 업데이트
        if (users.isNullOrEmpty()) {
            reactions.remove(reaction)
        } else {
            reactions[reaction] = users
        }
        
        val updatedMessage = oldMessage.copy(
            reactions = reactions
        )
        
        channelMessages[messageIndex] = updatedMessage
        messagesFlow[channelId]?.value = channelMessages.toList()
        
        return Result.success(Unit)
    }
    
    override fun createChannel(
        name: String,
        description: String?,
        ownerId: String,
        participantIds: List<String>,
        metadata: Map<String, Any>?
    ): Result<Channel> {
        val channelId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        
        val channel = Channel(
            id = channelId,
            name = name,
            description = description,
            ownerId = ownerId,
            participantIds = participantIds,
            lastMessagePreview = null,
            lastMessageTimestamp = null,
            metadata = metadata,
            createdAt = now,
            updatedAt = now
        )
        
        saveChannel(channel)
        return Result.success(channel)
    }
    
    override fun markChannelAsRead(channelId: String, userId: String): Result<Unit> {
        // In a real implementation, this would update a readMarkers collection
        // For testing, we just verify the channel exists
        if (!channels.containsKey(channelId)) {
            return Result.failure(NoSuchElementException("Channel not found: $channelId"))
        }
        
        return Result.success(Unit)
    }
    
    override fun getUnreadCount(channelId: String, userId: String): Flow<Result<Int>> = flow {
        // For testing, we always return 0 unread messages
        emit(Result.success(0))
    }.flowOn(dispatcher)
} 