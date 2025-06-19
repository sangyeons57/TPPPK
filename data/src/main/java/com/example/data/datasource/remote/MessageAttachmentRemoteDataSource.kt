package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.MessageAttachmentDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메시지 첨부파일 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 메시지의 첨부파일에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 첨부파일 데이터는 `/{messagePath}/message_attachments/{attachmentId}` 경로에 저장되며, `attachmentId`는 자동 생성됩니다.
 * 모든 작업 전에 `setCollection(messagePath)`를 호출하여 메시지 컨텍스트를 설정해야 합니다.
 * `messagePath`는 부모 메시지 문서의 전체 경로입니다 (예: "dm_channels/channelId/messages/messageId").
 */
interface MessageAttachmentRemoteDataSource : DefaultDatasource<MessageAttachmentDTO> {

}

@Singleton
class MessageAttachmentRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<MessageAttachmentDTO>(firestore, MessageAttachmentDTO::class.java), MessageAttachmentRemoteDataSource {

}
