package com.example.domain.usecase.project

import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectsWrapperRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 원격 서버로부터 프로젝트 목록을 가져와 로컬 데이터와 동기화하는 유스케이스입니다.
 *
 * @property projectRepository 프로젝트 관련 데이터를 제공하는 리포지토리
 */
class FetchProjectListUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectsWrapperRepository: ProjectsWrapperRepository
) {
    /**
     * 프로젝트 목록을 동기화합니다.
     *
     * @return Result 객체. 성공 시 Unit, 실패 시 예외를 포함합니다.
     */
    suspend operator fun invoke(): Result<Unit> {
        projectsWrapperRepository.
        return projectRepository.fetchProjectList()
    }
} 