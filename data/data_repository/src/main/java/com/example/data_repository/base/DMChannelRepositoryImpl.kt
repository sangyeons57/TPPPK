package com.example.data_repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.DMChannelRemoteDataSource
import com.example.data_datasource.remote.special.AuthRemoteDataSource
import com.example.data_datasource.remote.special.FunctionsRemoteDataSource
import com.example.data_model.remote.DMChannelDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.DMChannel
import com.example.domain_repository.base.DMChannelRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class DMChannelRepositoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    private val dmChannelMapper: DtoMapper<DMChannel, DMChannelDTO>,
) : DefaultRepositoryImpl<DMChannel, DMChannelDTO>(dmChannelRemoteDataSource, dmChannelMapper),
    DMChannelRepository {


    override suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception> {
        val currentUserId = authRemoteDataSource.getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))

        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot create DM channel with oneself."))
        }
        ensureCollection()

        val participants  = listOf(currentUserId, otherUserId)
        Log.d("DMChannelRepositoryImpl", "participants: $participants")
        val channelIdResult = dmChannelRemoteDataSource.findByParticipants(participants)
        return when (channelIdResult) {
            is CustomResult.Success -> {
                val dmChannelDTO = channelIdResult.data
                CustomResult.Success(mapper.dtoToDomain(dmChannelDTO))
            }
            is CustomResult.Failure -> CustomResult.Failure(channelIdResult.error)
            is CustomResult.Loading -> CustomResult.Loading // Propagate loading
            is CustomResult.Initial -> CustomResult.Initial // Propagate initial
            is CustomResult.Progress -> CustomResult.Progress(channelIdResult.progress) // Propagate progress
        }
    }

    override suspend fun createDMChannel(targetUserId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.createDMChannel(targetUserId)
    }
    
    override suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.blockDMChannel(channelId)
    }
    
    override suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.unblockDMChannel(channelId)
    }

    override suspend fun unblockDMChannelByUserId(targetUserId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.unblockDMChannelByUserId(targetUserId)
    }
}
