package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.MemberRepository
import javax.inject.Inject

/**
 * 프로젝트 멤버를 삭제(추방)하는 유스케이스 인터페이스
 */
interface DeleteProjectMemberUseCase {
    suspend operator fun invoke(projectId: String, userId: String): CustomResult<Unit, Exception>
}

/**
 * DeleteProjectMemberUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class DeleteProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : DeleteProjectMemberUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 멤버를 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param userId 삭제할 사용자의 ID
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(projectId: String, userId: String): CustomResult<Unit, Exception> {
        return projectMemberRepository.delete(DocumentId(userId))
    }
} 