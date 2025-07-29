package com.example.data_core.repository.factory

import com.example.data_core.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data_core.datasource.remote.special.FileUploadDataSource
import com.example.data_core.repository.base.MessageAttachmentRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MessageAttachmentRepository
import com.example.domain.repository.factory.context.MessageAttachmentRepositoryFactoryContext
import javax.inject.Inject

class MessageAttachmentRepositoryFactoryImpl @Inject constructor(
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource,
    private val fileUploadDataSource: FileUploadDataSource
) : RepositoryFactory<MessageAttachmentRepositoryFactoryContext, MessageAttachmentRepository> {

    override fun create(input: MessageAttachmentRepositoryFactoryContext): MessageAttachmentRepository {
        return MessageAttachmentRepositoryImpl(
            messageAttachmentRemoteDataSource = messageAttachmentRemoteDataSource,
            fileUploadDataSource = fileUploadDataSource,
            factoryContext = input,
        )
    }
}
