package com.example.domain.usecase.local.project.assets

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectLocalRepository
import com.example.domain.repository.local.FileLocalRepository
import javax.inject.Inject

interface RemoveProjectProfileImageLocalUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Unit, Exception>
}

class RemoveProjectProfileImageLocalUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository,
    private val fileLocalRepository: FileLocalRepository
) : RemoveProjectProfileImageLocalUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 프로젝트 프로필 이미지를 제거하고 프로젝트 정보 업데이트")
    }
} 