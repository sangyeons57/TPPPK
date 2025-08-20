package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트에서 멤버를 제거하는 UseCase
 *
 * Firebase Functions의 leaveProject 함수를 targetUserId 매개변수와 함께 호출하여
 * 프로젝트 소유자가 다른 멤버를 안전하게 제거할 수 있도록 합니다.
 */
interface RemoveMemberUseCase {
    /**
     * 프로젝트에서 지정된 멤버를 제거합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 제거할 사용자 ID
     * @return 성공 시 Unit, 실패 시 Exception
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        targetUserId: UserId
    ): CustomResult<Unit, Exception>
}

/**
 * 프로젝트 멤버 제거 UseCase 구현체
 */
class RemoveMemberUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : RemoveMemberUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        targetUserId: UserId
    ): CustomResult<Unit, Exception> {
        return try {
            // Repository를 통해 안전하게 멤버 제거
            // Repository 구현체에서 Firebase Functions 호출 처리
            projectRepository.removeMember(projectId.value, targetUserId.value)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}