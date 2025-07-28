package com.example.domain.usecase.local.media

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.MediaLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UploadMediaLocalUseCase {
    operator fun invoke(
        fileUri: Uri,
        storagePath: String
    ): Flow<CustomResult<String, Exception>>
}

class UploadMediaLocalUseCaseImpl @Inject constructor(
    private val mediaLocalRepository: MediaLocalRepository
) : UploadMediaLocalUseCase {

    override operator fun invoke(
        fileUri: Uri,
        storagePath: String
    ): Flow<CustomResult<String, Exception>> {
        return TODO("로컬 저장소에 미디어 파일을 저장하고 업로드 진행 상태를 Flow로 전달")
    }
} 