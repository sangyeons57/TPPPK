package com.example.domain.usecase.local.file

import com.example.core_common.result.CustomResult
import com.example.domain.repository.remote.FileRepository
import javax.inject.Inject

interface DeleteFileLocalUseCase {
    suspend operator fun invoke(storagePath: String): CustomResult<Unit, Exception>
}

class DeleteFileLocalUseCaseImpl @Inject constructor(
    private val fileLocalRepository: FileRepository
) : DeleteFileLocalUseCase {

    override suspend operator fun invoke(storagePath: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 지정된 경로의 파일을 삭제")
    }
} 