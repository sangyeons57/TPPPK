
package com.example.data.datasource.remote

import com.example.data.model._remote.MessageAttachmentDTO
import kotlinx.coroutines.flow.Flow

interface MessageAttachmentRemoteDataSource {

    /**
     * 특정 메시지에 포함된 모든 첨부파일 목록을 실시간으로 관찰합니다.
     * @param messagePath 첨부파일 목록을 가져올 메시지의 전체 경로
     * (예: "dm_channels/channelId/messages/messageId")
     */
    fun getAttachments(messagePath: String): Flow<List<MessageAttachmentDTO>>

    /**
     * 메시지에 첨부파일 정보를 추가합니다. 파일 업로드 후 URL을 받아와서 사용합니다.
     * @param messagePath 첨부파일을 추가할 메시지의 전체 경로
     * @param attachment 추가할 첨부파일의 정보 DTO
     * @return 생성된 첨부파일 문서의 ID를 포함한 Result 객체
     */
    suspend fun addAttachment(messagePath: String, attachment: MessageAttachmentDTO): Result<String>

    /**
     * 메시지에서 첨부파일 정보를 제거합니다.
     * @param messagePath 대상 메시지의 전체 경로
     * @param attachmentId 제거할 첨부파일의 ID
     */
    suspend fun removeAttachment(messagePath: String, attachmentId: String): Result<Unit>
}

