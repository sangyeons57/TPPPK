package com.example.domain.usecase.local.projects

import com.example.domain.model.Project
import com.example.domain.repository.local.ProjectLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface InsertProjectUseCase {
    suspend operator fun invoke(project: Project): CustomResult<Unit, Exception>
}

class InsertProjectUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : InsertProjectUseCase {

    override suspend operator fun invoke(project: Project): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에 프로젝트 정보를 삽입하거나 갱신")
    }
} 