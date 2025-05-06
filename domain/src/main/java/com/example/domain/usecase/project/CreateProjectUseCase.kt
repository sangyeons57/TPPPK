package com.example.domain.usecase.project

import com.example.domain.model.Project
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 새 프로젝트를 생성하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
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
    suspend operator fun invoke(name: String, description: String, isPublic: Boolean): Result<Project> {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim()
        
        // 프로젝트 이름 유효성 검증
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("프로젝트 이름은 필수 입력값입니다."))
        }
        
        // 프로젝트 이름 길이 검증 (2~30자)
        if (trimmedName.length < 2 || trimmedName.length > 30) {
            return Result.failure(IllegalArgumentException("프로젝트 이름은 2~30자 사이여야 합니다."))
        }
        
        // 프로젝트 설명 길이 검증 (최대 500자)
        if (trimmedDescription.length > 500) {
            return Result.failure(IllegalArgumentException("프로젝트 설명은 최대 500자까지 입력 가능합니다."))
        }
        
        // 프로젝트 이름 중복 검사
        val isAvailableResult = projectRepository.isProjectNameAvailable(trimmedName)
        if (isAvailableResult.isFailure) {
            return Result.failure(isAvailableResult.exceptionOrNull() 
                ?: Exception("프로젝트 이름 중복 확인 과정에서 오류가 발생했습니다."))
        }
        
        val isAvailable = isAvailableResult.getOrNull()
        if (isAvailable != true) {
            return Result.failure(IllegalArgumentException("이미 사용 중인 프로젝트 이름입니다."))
        }
        
        // 모든 검증 통과 시 프로젝트 생성
        return projectRepository.createProject(trimmedName, trimmedDescription, isPublic)
    }
} 