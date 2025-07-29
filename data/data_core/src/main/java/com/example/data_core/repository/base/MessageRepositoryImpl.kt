package com.example.data_core.repository.base

import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.remote.MessageRemoteDataSource
import com.example.data_core.repository.DefaultRepositoryImpl
import com.example.data_model.remote.toDto
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import javax.inject.Inject


class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    override val factoryContext: MessageRepositoryFactoryContext
) : DefaultRepositoryImpl(messageRemoteDataSource, factoryContext), MessageRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Message)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Message"))
        ensureCollection()
        return if (entity.isNew) {
            messageRemoteDataSource.create(entity.toDto())
        } else {
            messageRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
