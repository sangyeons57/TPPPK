package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import com.example.domain.repository.base.MessageAttachmentToSend
import com.example.domain.repository.base.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    override val factoryContext: MessageRepositoryFactoryContext
) : DefaultRepositoryImpl(messageRemoteDataSource, factoryContext.collectionPath), MessageRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Message)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Message"))

        return if (entity.isNew) {
            messageRemoteDataSource.create(entity.toDto())
        } else {
            messageRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
