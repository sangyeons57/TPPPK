package com.example.data.repository.factory

import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.repository.base.MessageRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import javax.inject.Inject

class MessageRepositoryFactoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource
) : RepositoryFactory<MessageRepositoryFactoryContext, MessageRepository> {

    override fun create(input: MessageRepositoryFactoryContext): MessageRepository {
        return MessageRepositoryImpl(
            messageRemoteDataSource = messageRemoteDataSource,
            messageAttachmentRemoteDataSource = messageAttachmentRemoteDataSource
        )
    }
}
