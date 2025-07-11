package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * 역할을 삭제하는 유스케이스 인터페이스
 */
interface DeleteRoleUseCase {
    suspend operator fun invoke(roleId: DocumentId): CustomResult<Unit, Exception>
}

/**
 * DeleteRoleUseCase의 구현체
 * @param projectRoleRepository 프로젝트 역할 데이터 접근을 위한 Repository
 */
class DeleteRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository,
    private val authRepository: AuthRepository
) : DeleteRoleUseCase {

    /**
     * 유스케이스를 실행하여 역할을 삭제합니다.
     * @param roleId 삭제할 역할의 ID
     * @return Result<Unit> 역할 삭제 처리 결과
     */
    override suspend fun invoke(roleId: DocumentId): CustomResult<Unit, Exception> {
        return when (val sessionResult = authRepository.getCurrentUserSession()){
            is CustomResult.Success -> {
                return projectRoleRepository.delete(roleId)
            }
            else -> {
                return CustomResult.Failure(Exception("로그인이 필요합니다."))
            }
        }
    }
}