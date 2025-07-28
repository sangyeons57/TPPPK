package com.example.domain.usecase.local.users

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.UserLocalRepository
import javax.inject.Inject

interface UploadProfileImageLocalUseCase {
    suspend operator fun invoke(imageUri: Uri): CustomResult<String, Exception>
}

class UploadProfileImageLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : UploadProfileImageLocalUseCase {

    override suspend operator fun invoke(imageUri: Uri): CustomResult<String, Exception> {
        return TODO("로컬 저장소에 프로필 이미지를 저장하고 경로 반환")
    }
} 