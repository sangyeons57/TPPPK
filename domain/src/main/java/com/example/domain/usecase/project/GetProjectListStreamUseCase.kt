package com.example.domain.usecase.project

import com.example.domain.model.Project
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 프로젝트 목록 스트림을 가져오는 유스케이스입니다.
 *
 * @property projectRepository 프로젝트 관련 데이터를 제공하는 리포지토리
 */
class GetProjectListStreamUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 프로젝트 목록을 지속적으로 관찰할 수 있는 Flow를 반환합니다.
     *
     * @return 프로젝트 목록을 방출하는 [Flow]
     */
    operator fun invoke(): Flow<List<Project>> {
        return projectRepository.getProjectListStream()
    }
} 