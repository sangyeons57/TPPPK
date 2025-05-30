package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import kotlinx.coroutines.flow.Flow

interface DMChannelRepository {
    suspend fun getDmChannelById(dmChannelId: String): CustomResult<DMChannel, Exception>
    suspend fun getDmChannelId(otherUserId: String): CustomResult<String, Exception>
    suspend fun getCurrentDmChannelsStream(): Flow<CustomResult<List<DMChannel>, Exception>>
    suspend fun getDmChannelWithUser(otherUserId: List<String>): CustomResult<DMChannel, Exception>
    suspend fun createDmChannel(otherUserId: String): CustomResult<String, Exception>
    suspend fun findDmChannelWithUser(otherUserId: String): CustomResult<String?, Exception>
}
