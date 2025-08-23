package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트 멤버 차단 해제 UseCase
 *
 * 프로젝트 소유자가 차단된 멤버를 다시 활성화하는 기능을 제공합니다.
 */
interface UnblockMemberUseCase {
    /**
     * 멤버 차단을 해제합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 차단 해제할 사용자 ID
     * @return 차단 해제 결과
     */
    suspend fun unblockMember(
        projectId: DocumentId,
        targetUserId: String
    ): CustomResult<Unit, Exception>
}

/**
 * 프로젝트 멤버 차단 해제 UseCase 구현체
 */
class UnblockMemberUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : UnblockMemberUseCase {

    /**
     * 멤버 차단을 해제하고 프로젝트 접근을 복구합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 차단 해제할 사용자 ID
     * @return 차단 해제 결과
     */
    override suspend fun unblockMember(
        projectId: DocumentId,
        targetUserId: String
    ): CustomResult<Unit, Exception> {
        return projectRepository.unblockMember(projectId, targetUserId)
    }
}