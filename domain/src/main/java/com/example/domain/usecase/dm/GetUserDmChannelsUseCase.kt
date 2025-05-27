// Required change for DmRepository:
// interface DmRepository {
//     // ... other methods
//     fun getDmChannelsStreamForUser(userId: String): Flow<List<Channel>> // Or Flow<List<DmChannelModel>> if a specific model is created
// }
// TODO: Consider creating a specific DmChannelModel instead of using the generic Channel model.
package com.example.domain.usecase.dm

import android.util.Log
import com.example.domain.model.Channel // TODO: Replace with DmChannelModel if created
import com.example.domain.repository.DmRepository // TODO: Create this repository interface
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 현재 로그인한 사용자의 DM 채널 목록을 스트림으로 가져오는 UseCase
 */
class GetUserDmChannelsUseCase @Inject constructor(
    private val dmRepository: DmRepository, // Changed from ChannelRepository
    private val userRepository: UserRepository
) {
    /**
     * 현재 로그인한 사용자의 모든 DM 채널을 Flow로 반환합니다.
     * 
     * @return DM 채널 목록을 포함하는 Flow. // TODO: Consider returning Flow<List<DmChannelModel>>
     */
    operator fun invoke(): Flow<List<Channel>> = flow { // TODO: Consider Flow<List<DmChannelModel>>
        try {
            // 현재 사용자 ID 가져오기
            val currentUserId = userRepository.getCurrentUserId()
            Log.d("DMChannelsUseCase", "GetUserDmChannelsUseCase invoked for current user: $currentUserId")
            
            // 현재 사용자의 DM 채널 스트림 수집
            // Changed from channelRepository.getChannelsByTypeStream(ChannelType.DM, currentUserId)
            val dmChannelsFlow = dmRepository.getDmChannelsStreamForUser(currentUserId)
                .onStart { 
                    Log.d("DMChannelsUseCase", "Starting DM channels stream for user: $currentUserId from DmRepository") 
                }
                .onEach { channels ->
                    // TODO: If DmChannelModel is used, logging might need adjustment if model structure changes.
                    Log.d("DMChannelsUseCase", "Received ${channels.size} DM channels from DmRepository")
                    channels.forEach { channel ->
                        Log.d("DMChannelsUseCase", "DM channel: ${channel.id}, participants: ${channel.dmSpecificData?.participantIds?.joinToString()}")
                    }
                }
                .map { channels ->
                    // Assuming DmRepository now only returns DM channels, explicit filtering might not be needed.
                    // If DmRepository can return other types, filtering would be necessary:
                    // val filteredChannels = channels.filter { it.type == ChannelType.DM }
                    // Log.d("DMChannelsUseCase", "Returning ${filteredChannels.size} DM channels after ensuring type")
                    // filteredChannels
                    Log.d("DMChannelsUseCase", "Returning ${channels.size} DM channels from DmRepository")
                    channels
                }
            
            // 채널 스트림 내보내기
            emitAll(dmChannelsFlow)
        } catch (e: Exception) {
            Log.e("DMChannelsUseCase", "Error getting current user or DM channels from DmRepository", e)
            emit(emptyList()) // TODO: Consider a more specific error handling or Result type
        }
    }
}