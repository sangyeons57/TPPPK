package com.example.domain.usecase.media

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.MediaRepository
import javax.inject.Inject

/**
 * 미디어 업로드 UseCase
 *
 * 주어진 미디어 파일 URI를 지정된 저장소 경로에 업로드합니다.
 */
interface UploadMediaUseCase {
    suspend operator fun invoke(uri: Uri, storagePath: String): CustomResult<String, Exception>
}

class UploadMediaUseCaseImpl @Inject constructor(
    private val mediaRepository: MediaRepository
) : UploadMediaUseCase {

    override suspend operator fun invoke(
        uri: Uri,
        storagePath: String
    ): CustomResult<String, Exception> {
        return mediaRepository.uploadFile(uri, storagePath)
    }
}