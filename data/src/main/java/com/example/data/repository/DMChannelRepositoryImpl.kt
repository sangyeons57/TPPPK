package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.DMChannelRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DMChannelRepositoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val auth: FirebaseAuth
) : DMChannelRepository {

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun getDmChannelById(dmChannelId: String): CustomResult<DMChannel, Exception> {
        return try {
            val dtoResult = dmChannelRemoteDataSource.observeDMChannel(dmChannelId).firstOrNull()
            if (dtoResult != null) {
                CustomResult.Success(dtoResult.toDomain())
            } else {
                CustomResult.Failure(Exception("DMChannel with ID $dmChannelId not found or failed to load."))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun getDmChannelId(otherUserId: String): CustomResult<String, Exception> {
        val currentUserId = getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))
        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot get DM channel ID with oneself."))
        }
        return dmChannelRemoteDataSource.findOrCreateDMChannel(otherUserId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getCurrentDmChannelsStream(): Flow<CustomResult<List<DMChannel>, Exception>> {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            return flowOf(CustomResult.Failure(Exception("User not logged in.")))
        }

        return userRemoteDataSource.getDmWrappersStream(currentUserId).flatMapLatest { dmWrappersResult ->
            when (dmWrappersResult) {
                is CustomResult.Success -> {
                    val dmWrapperDTOs = dmWrappersResult.data
                    if (dmWrapperDTOs.isEmpty()) {
                        flowOf(CustomResult.Success(emptyList()))
                    } else {
                        val channelFlows = dmWrapperDTOs.map { wrapper ->
                            dmChannelRemoteDataSource.observeDMChannel(wrapper.dmChannelId)
                                .map { dto -> dto?.toDomain() } // Map to Domain or null
                        }
                        combine(channelFlows) { channelsArray ->
                            CustomResult.Success(channelsArray.filterNotNull())
                        }
                    }
                }
                is CustomResult.Failure -> flowOf(CustomResult.Failure(dmWrappersResult.error))
                is CustomResult.Loading -> flowOf(CustomResult.Loading)
                is CustomResult.Initial -> flowOf(CustomResult.Initial)
                is CustomResult.Progress -> flowOf(CustomResult.Progress(dmWrappersResult.progress))
            }
        }
    }

    override suspend fun getDmChannelWithUser(otherUserIds: List<String>): CustomResult<DMChannel, Exception> {
        if (otherUserIds.size != 1) {
            return CustomResult.Failure(Exception("This method currently supports DM with a single user only."))
        }
        val otherUserId = otherUserIds.first()
        val currentUserId = getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))

        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot create DM channel with oneself."))
        }

        val channelIdResult = dmChannelRemoteDataSource.findOrCreateDMChannel(otherUserId)
        return when (channelIdResult) {
            is CustomResult.Success -> {
                val channelId = channelIdResult.data
                getDmChannelById(channelId) // Reuse existing method
            }
            is CustomResult.Failure -> CustomResult.Failure(channelIdResult.error)
            is CustomResult.Loading -> CustomResult.Loading // Propagate loading
            is CustomResult.Initial -> CustomResult.Initial // Propagate initial
            is CustomResult.Progress -> CustomResult.Progress(channelIdResult.progress) // Propagate progress
        }
    }

    override suspend fun createDmChannel(otherUserId: String): CustomResult<String, Exception> {
        val currentUserId = getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))
        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot create DM channel with oneself."))
        }
        return dmChannelRemoteDataSource.findOrCreateDMChannel(otherUserId)
    }

    override suspend fun findDmChannelWithUser(otherUserId: String): CustomResult<String?, Exception> {
        val currentUid = getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in to find DM channel."))

        if (currentUid == otherUserId) {
             // Depending on requirements, this could be an error or just return null (no such distinct channel)
            return CustomResult.Success(null) 
        }
        
        return try {
            val dmWrappersResult = userRemoteDataSource.getDmWrappersStream(currentUid).firstOrNull()
            when (dmWrappersResult) {
                is CustomResult.Success -> {
                    val foundWrapper = dmWrappersResult.data.find { it.otherUserId == otherUserId }
                    CustomResult.Success(foundWrapper?.dmChannelId)
                }
                is CustomResult.Failure -> CustomResult.Failure(dmWrappersResult.error)
                is CustomResult.Loading -> CustomResult.Loading // Or handle as appropriate for a find operation
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(dmWrappersResult.progress)
                null -> CustomResult.Success(null) // Stream ended before emitting, implies not found or initial state
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
        // 예: return dmChannelRemoteDataSource.findDmChannelWithUser(otherUserId)
        throw NotImplementedError("구현 필요: findDmChannelWithUser")
    }
}
