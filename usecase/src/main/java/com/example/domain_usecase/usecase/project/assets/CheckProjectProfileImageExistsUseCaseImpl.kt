package com.example.domain_usecase.usecase.project.assets

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 프로젝트 프로필 이미지 존재 여부 확인 UseCase 구현체
 */
class CheckProjectProfileImageExistsUseCaseImpl @Inject constructor(
    private val fileRepository: FileRepository
) : CheckProjectProfileImageExistsUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                // 고정 경로: project_profiles/{projectId}/profile.webp (기존 코드와 일치)
                val imagePath = "project_profiles/${projectId.value}/profile.webp"

                val result = fileRepository.fileExists(imagePath)
                when (result) {
                    is CustomResult.Success -> {
                        CustomResult.Success(result.data)
                    }

                    is CustomResult.Failure -> {
                        // 파일이 존재하지 않는 경우도 정상적인 결과로 처리
                        CustomResult.Success(false)
                    }

                    else -> {
                        CustomResult.Success(false)
                    }
                }
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
}