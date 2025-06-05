package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectsWrapperRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.Result

/**
 * 새 프로젝트를 생성하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectsWrapperRepository: ProjectsWrapperRepository,
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
                val projectIdResult = projectRepository.createProject(trimmedName, session.data.userId)
                if (projectIdResult is CustomResult.Failure) {
                    return CustomResult.Failure(projectIdResult.error)
                } else if (projectIdResult !is CustomResult.Success) {
                    return CustomResult.Failure(Exception("프로젝트 생성에 실패했습니다."))
                }
                val projectId = projectIdResult.data

                // 생성된 프로젝트에 현재 사용자를 참여시킵니다.
                val addProjectToUserResult = projectsWrapperRepository.addProjectToUser(session.data.userId, projectId)

                if (addProjectToUserResult is CustomResult.Failure) {
                    // 프로젝트 생성은 성공했으나 사용자를 참여시키는 데 실패한 경우입니다.
                    // 필요에 따라 여기서 생성된 프로젝트를 삭제하는 등의 롤백 로직을 고려할 수 있으나,
                    // 현재는 에러를 반환합니다.
                    return CustomResult.Failure(addProjectToUserResult.error)
                }

                return CustomResult.Success(projectId)
            }
            else -> {
                return CustomResult.Failure(Exception("로그인이 필요합니다."))
            }
        }
    }
} 