package com.example.domain.usecase.local.projects

import com.example.domain.model.Project
import com.example.domain.repository.local.ProjectLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetAllProjectsUseCase {
    operator fun invoke(): Flow<List<Project>>
}

class GetAllProjectsUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : GetAllProjectsUseCase {

    override operator fun invoke(): Flow<List<Project>> {
        return TODO("로컬에 저장된 모든 프로젝트 목록을 실시간으로 관찰")
    }
} 