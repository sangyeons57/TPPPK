package com.example.domain.usecase.local.project.assets

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectLocalRepository
import com.example.domain.repository.local.FileLocalRepository
import javax.inject.Inject

interface UploadProjectProfileImageLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        imageUri: Uri
    ): CustomResult<String, Exception>
}

class UploadProjectProfileImageLocalUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository,
    private val fileLocalRepository: FileLocalRepository
) : UploadProjectProfileImageLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        imageUri: Uri
    ): CustomResult<String, Exception> {
        return TODO("로컬 저장소에 프로젝트 프로필 이미지를 저장하고 프로젝트에 경로 업데이트")
    }
} 