package com.example.data_core.repository.factory

import com.example.data_core.datasource.remote.MessageRemoteDataSource
import com.example.data_core.repository.base.MessageRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import javax.inject.Inject

class MessageRepositoryFactoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
) : RepositoryFactory<MessageRepositoryFactoryContext, MessageRepository> {

    override fun create(input: MessageRepositoryFactoryContext): MessageRepository {
        return MessageRepositoryImpl(
            messageRemoteDataSource = messageRemoteDataSource,
            factoryContext = input,
        )
    }
}
