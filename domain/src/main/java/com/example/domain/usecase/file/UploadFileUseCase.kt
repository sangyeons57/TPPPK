package com.example.domain.usecase.file

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.FileRepository
import javax.inject.Inject

/**
 * 파일 업로드 UseCase
 *
 * 주어진 파일 URI를 지정된 저장소 경로에 업로드합니다.
 */
interface UploadFileUseCase {
    suspend operator fun invoke(fileUri: Uri, storagePath: String): CustomResult<String, Exception>
}

class UploadFileUseCaseImpl @Inject constructor(
    private val fileRepository: FileRepository
) : UploadFileUseCase {

    override suspend operator fun invoke(
        fileUri: Uri,
        storagePath: String
    ): CustomResult<String, Exception> {
        return fileRepository.uploadFile(storagePath, fileUri)
    }
}