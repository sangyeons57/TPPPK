package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.model.remote.MessageAttachmentDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.MessageAttachmentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
// import java.io.File // 안드로이드 Uri 대신 File 객체를 사용한다면
import javax.inject.Inject

class MessageAttachmentRepositoryImpl @Inject constructor(
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
    // private val localMediaDataSource: LocalMediaDataSource, // 파일 업로드 전처리 등에 사용 가능
    // TODO: 필요한 Mapper 주입
) : DefaultRepositoryImpl(messageAttachmentRemoteDataSource, factoryContext.collectionPath), MessageAttachmentRepository {
    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is MessageAttachment)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type MessageAttachment"))
        return if (entity.id.isAssigned()) {
            messageAttachmentRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            messageAttachmentRemoteDataSource.create(entity.toDto())
        }
    }
}
