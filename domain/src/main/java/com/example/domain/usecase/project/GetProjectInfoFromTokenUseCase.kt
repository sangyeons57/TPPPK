package com.example.domain.usecase.project

import com.example.domain.model.ProjectInfo
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 토큰으로부터 프로젝트 정보를 가져오는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class GetProjectInfoFromTokenUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 주어진 토큰으로부터 프로젝트 정보를 가져옵니다.
     *
     * @param token 프로젝트 초대 토큰
     * @return 성공 시 프로젝트 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(token: String): Result<ProjectInfo> {
        return projectRepository.getProjectInfoFromToken(token)
    }
} 