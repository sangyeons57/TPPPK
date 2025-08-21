package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * 특정 프로젝트의 전체 멤버 목록을 단발성으로 조회하는 UseCase
 * Provider에서 repository의 collection이 프로젝트 컨텍스트로 설정되어 있어야 합니다.
 */
interface GetProjectMembersUseCase {
    suspend operator fun invoke(): CustomResult<List<Member>, Exception>
}

class GetProjectMembersUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : GetProjectMembersUseCase {
    override suspend operator fun invoke(): CustomResult<List<Member>, Exception> {
        return projectMemberRepository.findAll()
    }
}

