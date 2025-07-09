package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import javax.inject.Inject

class DMChannelRepositoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: DMChannelRepositoryFactoryContext
) : DefaultRepositoryImpl(dmChannelRemoteDataSource, factoryContext), DMChannelRepository {


    override suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception> {
        val currentUserId = authRemoteDataSource.getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))

        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot create DM channel with oneself."))
        }
        ensureCollection()

        val channelIdResult = dmChannelRemoteDataSource.findByParticipants(listOf(currentUserId, otherUserId))
        return when (channelIdResult) {
            is CustomResult.Success -> {
                val dmChannelDTO = channelIdResult.data
                CustomResult.Success(dmChannelDTO.toDomain())
            }
            is CustomResult.Failure -> CustomResult.Failure(channelIdResult.error)
            is CustomResult.Loading -> CustomResult.Loading // Propagate loading
            is CustomResult.Initial -> CustomResult.Initial // Propagate initial
            is CustomResult.Progress -> CustomResult.Progress(channelIdResult.progress) // Propagate progress
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMChannel)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMChannel"))
        ensureCollection()
        if (entity.isNew) {
            return dmChannelRemoteDataSource.create(entity.toDto())
        } else {
            return dmChannelRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
    
    override suspend fun createDMChannel(targetUserName: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.createDMChannel(targetUserName)
    }
    
    override suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.blockDMChannel(channelId)
    }
    
    override suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.unblockDMChannel(channelId)
    }
}
