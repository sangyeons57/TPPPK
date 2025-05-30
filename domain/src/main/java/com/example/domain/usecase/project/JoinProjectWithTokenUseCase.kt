package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 토큰을 사용하여 프로젝트에 참여하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class JoinProjectWithTokenUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 주어진 토큰을 사용하여 프로젝트에 참여합니다.
     *
     * @param token 프로젝트 초대 토큰
     * @return 성공 시 프로젝트 ID가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(token: String): CustomResult<String, Exception> {
        //return projectRepository.joinProjectWithToken(token)
        return CustomResult.Failure(Exception("미구현 사용할지 고민중"))
    }
} 