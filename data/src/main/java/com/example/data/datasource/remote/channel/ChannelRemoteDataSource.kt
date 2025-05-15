package com.example.data.datasource.remote.channel

import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 채널 관련 원격 데이터 소스 인터페이스
 * Firestore의 'channels' 컬렉션 및 관련 하위 컬렉션과의 상호작용을 정의합니다.
 * 메시지 관련 기능은 [com.example.data.datasource.remote.message.MessageRemoteDataSource]로 분리되었습니다.
 */
interface ChannelRemoteDataSource {
    suspend fun getChannel(channelId: String): Result<Channel>
    
    fun getChannelStream(channelId: String): Flow<Channel>
    
    suspend fun createChannel(channel: Channel): Result<Channel>
    
    suspend fun updateChannel(channel: Channel): Result<Unit>
    
    suspend fun deleteChannel(channelId: String): Result<Unit>
    
    suspend fun getUserChannels(userId: String, type: ChannelType? = null): Result<List<Channel>>
    
    fun getUserChannelsStream(userId: String, type: ChannelType? = null): Flow<List<Channel>>
    
    suspend fun getChannelsByType(type: ChannelType, userId: String? = null): Result<List<Channel>>
    
    fun getChannelsByTypeStream(type: ChannelType, userId: String? = null): Flow<List<Channel>>
    
    suspend fun addDmParticipant(channelId: String, userId: String): Result<Unit>
    
    suspend fun removeDmParticipant(channelId: String, userId: String): Result<Unit>
    
    suspend fun getDmParticipants(channelId: String): Result<List<String>>
    
    fun getDmParticipantsStream(channelId: String): Flow<List<String>>
}