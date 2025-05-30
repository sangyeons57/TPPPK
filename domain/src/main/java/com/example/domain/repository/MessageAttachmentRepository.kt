package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model._new.enum.MessageAttachmentType
import com.example.domain.model.base.MessageAttachment
import kotlinx.coroutines.flow.Flow

interface MessageAttachmentRepository {
    /**
     * 특정 메시지에 첨부된 모든 첨부 파일 목록을 가져옵니다.
     * (실시간 업데이트가 필요하면 Flow<CustomResult<List<MessageAttachment>>> 로 변경)
     */
    suspend fun getAttachmentsForMessage(messageId: String): CustomResult<List<MessageAttachment>, Unit>

    /**
     * 새로운 첨부 파일을 업로드하고, 업로드된 파일 정보를 반환합니다.
     * @param channelId 첨부 파일이 속할 채널 ID (없을 수도 있음, 정책에 따라 결정)
     * @param fileUri 디바이스 내 파일의 Uri (String 또는 android.net.Uri)
     * @param type 첨부 파일 타입
     */
    suspend fun uploadAttachment(channelId: String?, fileUri: String, type: MessageAttachmentType): CustomResult<MessageAttachment, Unit>

    /**
     * 특정 첨부 파일을 삭제합니다.
     * (주의: 메시지와 연결된 첨부 파일을 삭제할 때의 정책 필요)
     */
    suspend fun deleteAttachment(attachmentId: String): CustomResult<Unit, Unit>

    // TODO: 메시지 ID 없이 첨부파일 ID로 직접 조회하는 기능 (getAttachmentById) 등 필요시 추가
}
