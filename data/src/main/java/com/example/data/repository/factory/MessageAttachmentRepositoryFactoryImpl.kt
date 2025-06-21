package com.example.data.repository.factory

import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.repository.base.MessageAttachmentRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MessageAttachmentRepository
import com.example.domain.repository.factory.context.MessageAttachmentRepositoryFactoryContext
import javax.inject.Inject

class MessageAttachmentRepositoryFactoryImpl @Inject constructor(
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource
) : RepositoryFactory<MessageAttachmentRepositoryFactoryContext, MessageAttachmentRepository> {

    override fun create(input: MessageAttachmentRepositoryFactoryContext): MessageAttachmentRepository {
        return MessageAttachmentRepositoryImpl(
            messageAttachmentRemoteDataSource = messageAttachmentRemoteDataSource
        )
    }
}
