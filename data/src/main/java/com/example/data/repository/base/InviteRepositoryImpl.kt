package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.InviteRemoteDataSource
import com.example.data.model.remote.InviteDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.base.Invite
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.InviteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val inviteRemoteDataSource: InviteRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(inviteRemoteDataSource, factoryContext.collectionPath), InviteRepository {
    override fun getActiveProjectInvitesStream(projectId: String): Flow<CustomResult<List<Invite>, Exception>> {
        TODO("Not yet implemented")
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Invite)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Invite"))

        return if (entity.id.isAssigned()) {
            inviteRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            inviteRemoteDataSource.create(entity.toDto())
        }
    }

    override suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Invite)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Invite"))
        return inviteRemoteDataSource.create(entity.toDto())
    }

}
