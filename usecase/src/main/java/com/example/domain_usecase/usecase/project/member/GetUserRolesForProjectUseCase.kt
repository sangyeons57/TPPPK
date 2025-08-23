package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * 현재 사용자의 프로젝트 내 모든 역할 ID를 가져오는 UseCase
 */
interface GetUserRolesForProjectUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<List<String>, Exception>
}

class GetUserRolesForProjectUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
) : GetUserRolesForProjectUseCase {
    override suspend fun invoke(projectId: DocumentId): CustomResult<List<String>, Exception> {
        // Resolve current user id
        val session = authRepository.getCurrentUserSession()
        val userId = when (session) {
            is CustomResult.Success -> session.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(session.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(session.progress)
        }

        // Get member
        val memberRes = memberRepository.findById(DocumentId.from(userId))
        val member = when (memberRes) {
            is CustomResult.Success -> memberRes.data
            is CustomResult.Failure -> return CustomResult.Failure(memberRes.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(memberRes.progress)
        }

        // Convert role IDs to Role strings
        val roleStrings: List<String> = member.roleIds.map { it.value }

        // Since Role is a complex class, we return the role ID strings
        // The caller can use these IDs to fetch full Role objects if needed
        return CustomResult.Success(roleStrings)
    }
}