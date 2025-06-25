package com.example.domain.usecase.file

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.FileRepository
import javax.inject.Inject

/**
 * 파일 다운로드 UseCase
 *
 * 지정된 저장소 경로의 파일을 로컬 URI로 다운로드합니다.
 */
interface DownloadFileUseCase {
    suspend operator fun invoke(
        storagePath: String,
        localFileUri: Uri
    ): CustomResult<Unit, Exception>
}

class DownloadFileUseCaseImpl @Inject constructor(
    private val fileRepository: FileRepository
) : DownloadFileUseCase {

    override suspend operator fun invoke(
        storagePath: String,
        localFileUri: Uri
    ): CustomResult<Unit, Exception> {
        return fileRepository.downloadFileToUri(storagePath, localFileUri)
    }
}