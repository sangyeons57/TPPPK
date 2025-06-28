package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.model.remote.DMChannelDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.DMChannelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class DMChannelRepositoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(dmChannelRemoteDataSource, factoryContext.collectionPath), DMChannelRepository {


    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getCurrentDmChannelsStream(): Flow<CustomResult<List<DMChannel>, Exception>> {
        val currentUserId = authRemoteDataSource.getCurrentUserId()
            ?: return flowOf(CustomResult.Failure(Exception("User not logged in.")))

        return dmChannelRemoteDataSource.observeAll().flatMapLatest { dmWrappersResult ->
            when (dmWrappersResult) {
                is CustomResult.Success -> {
                    val dmWrapperDTOs = dmWrappersResult.data
                    if (dmWrapperDTOs.isEmpty()) {
                        flowOf(CustomResult.Success(emptyList()))
                    } else {
                        flowOf(CustomResult.Success(
                            buildList {
                                for (wrapper in dmWrapperDTOs) {
                                    when (val dtoResult = dmChannelRemoteDataSource.findById(DocumentId(wrapper.id))) {
                                        is CustomResult.Success -> add(dtoResult.data.toDomain() as DMChannel)
                                        else -> { /* skip non-success */ }
                                    }
                                }
                            } // keep only successful DMChannel objects
                        ))
                    }
                }
                is CustomResult.Failure -> flowOf(CustomResult.Failure(dmWrappersResult.error))
                is CustomResult.Loading -> flowOf(CustomResult.Loading)
                is CustomResult.Initial -> flowOf(CustomResult.Initial)
                is CustomResult.Progress -> flowOf(CustomResult.Progress(dmWrappersResult.progress))
            }
        }
    }

    override suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception> {
        val currentUserId = authRemoteDataSource.getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))

        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot create DM channel with oneself."))
        }

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
        if (entity.id.isAssigned()) {
            return dmChannelRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            return dmChannelRemoteDataSource.create(entity.toDto())
        }
    }


    override suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMChannel)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMChannel"))
        return dmChannelRemoteDataSource.create(entity.toDto())
    }
}
