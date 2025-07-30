package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.MessageRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.MessageRepository
import com.example.mapper.message.MessageMapper
import javax.inject.Inject


class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageMapper: MessageMapper,
) : DefaultRepositoryImpl(messageRemoteDataSource), MessageRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Message)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Message"))
        ensureCollection()
        return if (entity.isNew) {
            messageRemoteDataSource.create(messageMapper.domainToDto(entity))
        } else {
            messageRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
