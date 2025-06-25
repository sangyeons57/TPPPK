package com.example.domain.usecase.media

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.MediaRepository
import javax.inject.Inject

/**
 * 미디어 삭제 UseCase
 *
 * 지정된 파일 URL의 미디어를 삭제합니다.
 */
interface DeleteMediaUseCase {
    suspend operator fun invoke(fileUrl: String): CustomResult<Unit, Exception>
}

class DeleteMediaUseCaseImpl @Inject constructor(
    private val mediaRepository: MediaRepository
) : DeleteMediaUseCase {

    override suspend operator fun invoke(fileUrl: String): CustomResult<Unit, Exception> {
        return mediaRepository.deleteFile(fileUrl)
    }
}