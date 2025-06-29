package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.MessageAttachmentRepositoryFactoryContext

interface MessageAttachmentRepository : DefaultRepository {
    override val factoryContext: MessageAttachmentRepositoryFactoryContext
}
