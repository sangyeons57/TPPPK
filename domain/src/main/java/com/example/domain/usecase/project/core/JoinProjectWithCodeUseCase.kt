package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 참여 코드를 사용하여 프로젝트에 참여하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class JoinProjectWithCodeUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 주어진 참여 코드를 사용하여 프로젝트에 참여합니다.
     *
     * @param code 프로젝트 참여 코드
     * @return 성공 시 프로젝트 ID가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(code: String): CustomResult<String, Exception> {
        if (code.isBlank()) {
            return CustomResult.Failure(Exception("초대 코드가 비어있습니다."))
        }

        return when (val result = projectRepository.joinProjectWithInvite(code)) {
            is CustomResult.Success -> {
                val projectId = result.data["projectId"] as? String
                if (projectId != null) {
                    CustomResult.Success(projectId)
                } else {
                    CustomResult.Failure(Exception("프로젝트 참여 응답에서 프로젝트 ID를 찾을 수 없습니다."))
                }
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(result.error)
            }
            is CustomResult.Loading -> {
                CustomResult.Loading
            }
            is CustomResult.Initial -> {
                CustomResult.Initial
            }
            is CustomResult.Progress -> {
                CustomResult.Loading
            }
        }
    }
} 