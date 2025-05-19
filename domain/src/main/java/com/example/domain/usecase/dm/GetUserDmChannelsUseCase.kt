package com.example.domain.usecase.dm

import android.util.Log
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 현재 로그인한 사용자의 DM 채널 목록을 스트림으로 가져오는 UseCase
 */
class GetUserDmChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository
) {
    /**
     * 현재 로그인한 사용자의 모든 DM 채널을 Flow로 반환합니다.
     * 
     * @return DM 채널 목록을 포함하는 Flow
     */
    operator fun invoke(): Flow<List<Channel>> = flow {
        try {
            // 현재 사용자 ID 가져오기
            val currentUserId = userRepository.getCurrentUserId()
            Log.d("DMChannelsUseCase", "GetUserDmChannelsUseCase invoked for current user: $currentUserId")
            
            // 현재 사용자의 DM 채널 스트림 수집
            val dmChannelsFlow = channelRepository.getChannelsByTypeStream(ChannelType.DM, currentUserId)
                .onStart { 
                    Log.d("DMChannelsUseCase", "Starting DM channels stream for user: $currentUserId") 
                }
                .onEach { channels ->
                    Log.d("DMChannelsUseCase", "Received ${channels.size} DM channels from repository")
                    channels.forEach { channel ->
                        Log.d("DMChannelsUseCase", "DM channel: ${channel.id}, participants: ${channel.dmSpecificData?.participantIds?.joinToString()}")
                    }
                }
                .map { channels ->
                    // DM 채널만 필터링 (타입 체크는 리포지토리에서 이미 했지만 안전 장치로 한 번 더)
                    val filteredChannels = channels.filter { it.type == ChannelType.DM }
                    if (filteredChannels.size != channels.size) {
                        Log.w("DMChannelsUseCase", "Filtered out ${channels.size - filteredChannels.size} non-DM channels")
                    }
                    Log.d("DMChannelsUseCase", "Returning ${filteredChannels.size} filtered DM channels")
                    filteredChannels
                }
            
            // 채널 스트림 내보내기
            emitAll(dmChannelsFlow)
        } catch (e: Exception) {
            Log.e("DMChannelsUseCase", "Error getting current user or DM channels", e)
            emit(emptyList())
        }
    }
} 