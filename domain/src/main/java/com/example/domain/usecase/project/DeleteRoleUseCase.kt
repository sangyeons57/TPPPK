package com.example.domain.usecase.project

import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

/**
 * 역할을 삭제하는 유스케이스 인터페이스
 */
interface DeleteRoleUseCase {
    suspend operator fun invoke(roleId: String): Result<Unit>
}

/**
 * DeleteRoleUseCase의 구현체
 * @param projectRoleRepository 프로젝트 역할 데이터 접근을 위한 Repository
 */
class DeleteRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : DeleteRoleUseCase {

    /**
     * 유스케이스를 실행하여 역할을 삭제합니다.
     * @param roleId 삭제할 역할의 ID
     * @return Result<Unit> 역할 삭제 처리 결과
     */
    override suspend fun invoke(roleId: String): Result<Unit> {
        return projectRoleRepository.deleteRole(roleId)
    }
} 