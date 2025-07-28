package com.example.domain.usecase.local.file

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.FileLocalRepository
import javax.inject.Inject

interface UploadFileLocalUseCase {
    suspend operator fun invoke(fileUri: Uri, storagePath: String): CustomResult<String, Exception>
}

class UploadFileLocalUseCaseImpl @Inject constructor(
    private val fileLocalRepository: FileLocalRepository
) : UploadFileLocalUseCase {

    override suspend operator fun invoke(
        fileUri: Uri,
        storagePath: String
    ): CustomResult<String, Exception> {
        return TODO("로컬 저장소에 파일을 복사/저장하고 로컬 경로 반환")
    }
} 