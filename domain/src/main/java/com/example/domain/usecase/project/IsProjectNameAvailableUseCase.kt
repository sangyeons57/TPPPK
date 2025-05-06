package com.example.domain.usecase.project

import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 프로젝트 이름 사용 가능 여부를 확인하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class IsProjectNameAvailableUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 주어진 이름이 프로젝트 이름으로 사용 가능한지 확인합니다.
     *
     * @param name 확인할 프로젝트 이름
     * @return 성공 시 사용 가능 여부(Boolean)가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(name: String): Result<Boolean> {
        return projectRepository.isProjectNameAvailable(name)
    }
} 