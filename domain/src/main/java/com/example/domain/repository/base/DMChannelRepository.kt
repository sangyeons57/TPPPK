package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface DMChannelRepository : DefaultRepository {
    override val factoryContext: DMChannelRepositoryFactoryContext

    suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception>
}
