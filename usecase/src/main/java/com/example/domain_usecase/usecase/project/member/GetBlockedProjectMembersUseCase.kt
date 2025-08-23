package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * 프로젝트의 차단된 멤버 목록 조회 UseCase
 *
 * BLOCKED 상태의 멤버들만 조회합니다.
 */
interface GetBlockedProjectMembersUseCase {
    /**
     * 차단된 멤버들을 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 차단된 멤버 목록
     */
    suspend fun getBlockedMembers(projectId: DocumentId): CustomResult<List<Member>, Exception>
}

/**
 * 프로젝트의 차단된 멤버 목록 조회 UseCase 구현체
 */
class GetBlockedProjectMembersUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository
) : GetBlockedProjectMembersUseCase {

    /**
     * 차단된 멤버들을 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 차단된 멤버 목록
     */
    override suspend fun getBlockedMembers(projectId: DocumentId): CustomResult<List<Member>, Exception> {
        return memberRepository.findAllBlockedMembers()
    }
}