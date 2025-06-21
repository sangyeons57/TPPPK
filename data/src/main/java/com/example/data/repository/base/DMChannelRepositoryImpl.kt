package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.model.remote.DMChannelDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.DMChannelRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DMChannelRepositoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val auth: FirebaseAuth,
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(dmChannelRemoteDataSource, factoryContext.collectionPath), DMChannelRepository {

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getCurrentDmChannelsStream(): Flow<CustomResult<List<DMChannel>, Exception>> {
        val currentUserId = getCurrentUserId()
            ?: return flowOf(CustomResult.Failure(Exception("User not logged in.")))

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

    override suspend fun findByOtherUserId(otherUserIds: List<String>): CustomResult<DMChannel, Exception> {
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

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMChannel)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMChannel"))
        if (entity.id.isAssigned()) {
            return dmChannelRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            return dmChannelRemoteDataSource.create(entity.toDto())
        }
    }

}
