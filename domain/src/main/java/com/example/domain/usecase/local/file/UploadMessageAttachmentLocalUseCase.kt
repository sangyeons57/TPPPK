package com.example.domain.usecase.local.file

import android.net.Uri
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalMessageAttachmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UploadMessageAttachmentLocalUseCase {
    operator fun invoke(
        fileUri: Uri,
        messageId: DocumentId
    ): Flow<FileUploadResultData>
}

class UploadMessageAttachmentLocalUseCaseImpl @Inject constructor(
    private val messageAttachmentLocalRepository: LocalMessageAttachmentRepository,
    private val validateFileLocalUseCase: ValidateFileLocalUseCase
) : UploadMessageAttachmentLocalUseCase {

    override operator fun invoke(
        fileUri: Uri,
        messageId: DocumentId
    ): Flow<FileUploadResultData> {
        return TODO("로컬 저장소에 메시지 첨부파일을 저장하고 진행 상태를 Flow로 전달")
    }
} 