package com.example.domain.usecase.project

import com.example.domain.model.Project
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * 스케줄 생성 시 선택 가능한 프로젝트 목록을 가져오는 유스케이스 인터페이스
 */
interface GetSchedulableProjectsUseCase {
    suspend operator fun invoke(): Result<List<Project>>
}

/**
 * GetSchedulableProjectsUseCase의 구현체
 * @param projectRepository 프로젝트 데이터 접근을 위한 Repository
 */
class GetSchedulableProjectsUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : GetSchedulableProjectsUseCase {

    /**
     * 유스케이스를 실행하여 스케줄 생성이 가능한 프로젝트 목록을 가져옵니다.
     * @return Result<List<Project>> 프로젝트 목록 로드 결과
     */
    override suspend fun invoke(): Result<List<Project>> {
        return projectRepository.getAvailableProjectsForScheduling()
    }
} 