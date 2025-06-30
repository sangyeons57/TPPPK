package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

// 메시지 전송 시 사용할 첨부파일 모델 (도메인 모델 MessageAttachment와 구분)
data class MessageAttachmentToSend(
    val fileName: String,
    val mimeType: String,
    val sourceUri: String // 예시: content URI 또는 file URI
    // val bytes: ByteArray? // 또는 직접 바이트를 전달할 경우
)


interface MessageRepository : DefaultRepository {
    override val factoryContext: MessageRepositoryFactoryContext
}
