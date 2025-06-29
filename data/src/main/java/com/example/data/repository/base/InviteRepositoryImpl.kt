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
import com.example.domain.repository.factory.context.InviteRepositoryFactoryContext
import com.example.domain.repository.base.InviteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val inviteRemoteDataSource: InviteRemoteDataSource,
    override val factoryContext: InviteRepositoryFactoryContext
) : DefaultRepositoryImpl(inviteRemoteDataSource, factoryContext), InviteRepository {
    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Invite)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Invite"))
        ensureCollection()
        return if (entity.isNew) {
            inviteRemoteDataSource.create(entity.toDto())
        } else {
            inviteRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
