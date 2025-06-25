package com.example.domain.usecase.file

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.FileRepository
import javax.inject.Inject

/**
 * 파일 삭제 UseCase
 *
 * 지정된 저장소 경로의 파일을 삭제합니다.
 */
interface DeleteFileUseCase {
    suspend operator fun invoke(storagePath: String): CustomResult<Unit, Exception>
}

class DeleteFileUseCaseImpl @Inject constructor(
    private val fileRepository: FileRepository
) : DeleteFileUseCase {

    override suspend operator fun invoke(storagePath: String): CustomResult<Unit, Exception> {
        return fileRepository.deleteFile(storagePath)
    }
}