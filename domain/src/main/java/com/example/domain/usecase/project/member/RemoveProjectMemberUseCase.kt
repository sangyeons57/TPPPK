package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.MemberRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for removing a member from a project.
 */
interface RemoveProjectMemberUseCase {
    /**
     * Removes a user from a project.
     *
     * @param userId The ID of the user to remove.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(userId: String): CustomResult<Unit, Exception>
}

/**
 * Implementation of [RemoveProjectMemberUseCase].
 */
class RemoveProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : RemoveProjectMemberUseCase {

    override suspend fun invoke(userId: String): CustomResult<Unit, Exception> {
        return when (val result = projectMemberRepository.delete(DocumentId(userId))) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}
