package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 프로젝트의 차단된 멤버 목록 실시간 관찰 UseCase
 *
 * BLOCKED 상태의 멤버들만 실시간으로 관찰합니다.
 */
interface ObserveBlockedProjectMembersUseCase {
    /**
     * 차단된 멤버들을 실시간으로 관찰합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 차단된 멤버 목록 Flow
     */
    operator fun invoke(projectId: DocumentId): Flow<CustomResult<List<Member>, Exception>>
}

/**
 * 프로젝트의 차단된 멤버 목록 실시간 관찰 UseCase 구현체
 */
class ObserveBlockedProjectMembersUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository
) : ObserveBlockedProjectMembersUseCase {

    /**
     * 차단된 멤버들을 실시간으로 관찰합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 차단된 멤버 목록 Flow
     */
    override operator fun invoke(projectId: DocumentId): Flow<CustomResult<List<Member>, Exception>> {
        return memberRepository.observeBlockedMembers()
    }
}