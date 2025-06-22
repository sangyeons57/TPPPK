package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

interface DMChannelRepository : DefaultRepository {
    suspend fun getCurrentDmChannelsStream(): Flow<CustomResult<List<DMChannel>, Exception>>
    suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception>
}
