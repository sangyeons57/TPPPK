package com.example.domain_usecase.usecase.project.authorization

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.model.base.Role
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * Helper use case to check if the currently authenticated user
 * has OWNER role in the given project, using only the projectId.
 */
interface IsCurrentUserOwnerUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception>
}

class IsCurrentUserOwnerUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository
) : IsCurrentUserOwnerUseCase {

    override suspend fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception> {
        // 1) Resolve current user id from session
        val session = authRepository.getCurrentUserSession()
        val userId = when (session) {
            is CustomResult.Success -> session.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(session.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(session.progress)
        }

        // 2) Scope member repository to project members and fetch member document
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        val memberResult = memberRepository.findById(DocumentId.from(userId))
        return when (memberResult) {
            is CustomResult.Success -> {
                val member = memberResult.data as Member
                val isOwner = member.roleIds.any { it.value == Role.OWNER }
                CustomResult.Success(isOwner)
            }

            is CustomResult.Failure -> CustomResult.Failure(memberResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(memberResult.progress)
        }
    }
}

