package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
// import com.example.data.datasource.local.media.LocalMediaDataSource // 필요시 파일 업로드/관리
import com.example.data.model.mapper.toDomain // TODO: 실제 매퍼 경로 및 함수 확인
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.repository.MessageAttachmentRepository
// import java.io.File // 안드로이드 Uri 대신 File 객체를 사용한다면
import javax.inject.Inject

class MessageAttachmentRepositoryImpl @Inject constructor(
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource
    // private val localMediaDataSource: LocalMediaDataSource, // 파일 업로드 전처리 등에 사용 가능
    // TODO: 필요한 Mapper 주입
) : MessageAttachmentRepository {

    override suspend fun getAttachmentsForMessage(messageId: String): CustomResult<List<MessageAttachment>> {
        return messageAttachmentRemoteDataSource.getAttachmentsForMessage(messageId).mapCatching { dtoList ->
            dtoList.map { it.toDomain() } // TODO: MessageAttachmentDto를 MessageAttachment로 매핑
        }
    }

    override suspend fun uploadAttachment(channelId: String?, fileUri: String, type: MessageAttachmentType): CustomResult<MessageAttachment> {
        // TODO: fileUri (String 또는 android.net.Uri)를 실제 파일로 변환하거나,
        // DataSource가 Uri를 직접 처리할 수 있도록 수정 필요.
        // 예: localMediaDataSource.getFileFromUri(fileUri)?.let { file ->
        //         return messageAttachmentRemoteDataSource.uploadAttachment(channelId, file, type).mapCatching { dto ->
        //             dto.toDomain()
        //         }
        //     } ?: CustomResult.Error(Exception("File not found or invalid URI"))
        return messageAttachmentRemoteDataSource.uploadAttachment(channelId, fileUri, type).mapCatching { dto ->
             dto.toDomain() // TODO: MessageAttachmentDto를 MessageAttachment로 매핑
        }
    }

    override suspend fun deleteAttachment(attachmentId: String): CustomResult<Unit> {
        return messageAttachmentRemoteDataSource.deleteAttachment(attachmentId)
    }
}
