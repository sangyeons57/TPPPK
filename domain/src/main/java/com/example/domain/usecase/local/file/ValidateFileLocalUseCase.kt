package com.example.domain.usecase.local.file

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.remote.FileRepository
import javax.inject.Inject

interface ValidateFileLocalUseCase {
    suspend operator fun invoke(fileUri: Uri): CustomResult<FileValidationResult, FileValidationError>
}

class ValidateFileLocalUseCaseImpl @Inject constructor(
    private val fileLocalRepository: FileRepository
) : ValidateFileLocalUseCase {

    override suspend operator fun invoke(fileUri: Uri): CustomResult<FileValidationResult, FileValidationError> {
        return TODO("로컬 저장소에서 파일 유효성 검사 (크기, 타입, 접근 가능성 등)")
    }
} 