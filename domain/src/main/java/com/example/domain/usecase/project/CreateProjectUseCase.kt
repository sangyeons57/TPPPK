package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 새 프로젝트를 생성하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 새 프로젝트를 생성합니다.
     * 프로젝트 이름 중복 여부와 입력값 유효성을 검사합니다.
     *
     * @param name 프로젝트 이름
     * @param description 프로젝트 설명
     * @param isPublic 공개 프로젝트 여부
     * @return 성공 시 생성된 프로젝트 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(name: String): CustomResult<String, Exception> {
        val trimmedName = name.trim()

        // 프로젝트 이름 유효성 검증
        if (trimmedName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 이름은 필수 입력값입니다."))
        }
        
        // 프로젝트 이름 길이 검증 (2~30자)
        if (trimmedName.length < 2 || trimmedName.length > 30) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 이름은 2~30자 사이여야 합니다."))
        }
        

        val session = authRepository.getCurrentUserSession()

        // 모든 검증 통과 시 프로젝트 생성
        return when (session) {
            is CustomResult.Success -> {
                return projectRepository.createProject(trimmedName, session.data.userId)
            }
            else -> {
                return CustomResult.Failure(Exception("로그인이 필요합니다."))
            }
        }
    }
} 