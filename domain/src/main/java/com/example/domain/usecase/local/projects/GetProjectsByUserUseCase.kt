package com.example.domain.usecase.local.projects

import com.example.domain.model.Project
import com.example.domain.repository.local.ProjectLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetProjectsByUserUseCase {
    operator fun invoke(userId: String): Flow<List<Project>>
}

class GetProjectsByUserUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : GetProjectsByUserUseCase {

    override operator fun invoke(userId: String): Flow<List<Project>> {
        return TODO("특정 사용자가 속한 모든 프로젝트 목록을 실시간으로 관찰")
    }
} 