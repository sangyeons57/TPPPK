package com.example.domain.usecase.file

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.FileRepository
import javax.inject.Inject

/**
 * 파일 다운로드 URL 조회 UseCase
 *
 * 지정된 저장소 경로의 파일에 대한 다운로드 URL을 반환합니다.
 */
interface GetFileUrlUseCase {
    suspend operator fun invoke(storagePath: String): CustomResult<String, Exception>
}

class GetFileUrlUseCaseImpl @Inject constructor(
    private val fileRepository: FileRepository
) : GetFileUrlUseCase {

    override suspend operator fun invoke(storagePath: String): CustomResult<String, Exception> {
        return fileRepository.getDownloadUrl(storagePath)
    }
}