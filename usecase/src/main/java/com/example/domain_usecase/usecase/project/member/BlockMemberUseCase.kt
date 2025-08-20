package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트 멤버 차단/금지 UseCase
 *
 * 프로젝트 소유자가 멤버를 차단하거나 금지하는 기능을 제공합니다.
 */
interface BlockMemberUseCase {
    /**
     * 멤버를 차단합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 차단할 사용자 ID
     * @return 차단 결과
     */
    suspend fun blockMember(
        projectId: DocumentId,
        targetUserId: String
    ): CustomResult<Unit, Exception>

    /**
     * 멤버를 금지합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 금지할 사용자 ID
     * @return 금지 결과
     */
    suspend fun banMember(
        projectId: DocumentId,
        targetUserId: String
    ): CustomResult<Unit, Exception>
}

/**
 * 프로젝트 멤버 차단/금지 UseCase 구현체
 */
class BlockMemberUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : BlockMemberUseCase {

    /**
     * 멤버를 차단합니다.
     * 차단된 멤버는 프로젝트에 다시 참여할 수 없습니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 차단할 사용자 ID
     * @return 차단 결과
     */
    override suspend fun blockMember(
        projectId: DocumentId,
        targetUserId: String
    ): CustomResult<Unit, Exception> {
        return projectRepository.blockMember(projectId, targetUserId, "blocked")
    }

    /**
     * 멤버를 금지합니다.
     * 금지된 멤버는 영구적으로 프로젝트에 참여할 수 없습니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 금지할 사용자 ID
     * @return 금지 결과
     */
    override suspend fun banMember(
        projectId: DocumentId,
        targetUserId: String
    ): CustomResult<Unit, Exception> {
        return projectRepository.blockMember(projectId, targetUserId, "banned")
    }
}