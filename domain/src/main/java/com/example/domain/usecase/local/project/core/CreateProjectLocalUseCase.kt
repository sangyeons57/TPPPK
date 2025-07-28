package com.example.domain.usecase.local.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.repository.local.ProjectLocalRepository
import com.example.domain.repository.local.AuthLocalRepository
import javax.inject.Inject

interface CreateProjectLocalUseCase {
    suspend operator fun invoke(
        projectName: String,
        description: String? = null
    ): CustomResult<Project, Exception>
}

class CreateProjectLocalUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository,
    private val authLocalRepository: AuthLocalRepository
) : CreateProjectLocalUseCase {

    override suspend operator fun invoke(
        projectName: String,
        description: String?
    ): CustomResult<Project, Exception> {
        return TODO("로컬 저장소에 새 프로젝트를 생성하고 기본 구조 설정")
    }
} 