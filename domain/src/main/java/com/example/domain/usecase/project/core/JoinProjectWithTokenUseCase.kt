package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 토큰을 사용하여 프로젝트에 참여하는 UseCase
 * 
 * 참고: 현재 프로젝트에서는 코드 기반 초대(JoinProjectWithCodeUseCase)만 사용하고 있습니다.
 * 토큰 기반 초대 시스템은 향후 구현 예정입니다.
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
        // TODO: 토큰 기반 초대 시스템 구현 시 활성화
        // 현재는 코드 기반 초대(JoinProjectWithCodeUseCase)만 지원
        return CustomResult.Failure(Exception("토큰 기반 초대는 아직 구현되지 않았습니다. 초대 코드를 사용해주세요."))
    }
} 