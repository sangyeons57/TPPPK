package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.MemberRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for adding a member to a project.
 */
interface AddProjectMemberUseCase {
    /**
     * Adds a user to a project with the specified initial roles.
     *
     * @param userId The ID of the user to add.
     * @param initialRoleIds The list of role IDs to assign to the member initially.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(userId: String, initialRoleIds: List<String>): CustomResult<Unit, Exception>
}
/**
 * Implementation of [AddProjectMemberUseCase].
 */
class AddProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : AddProjectMemberUseCase {

    override suspend fun invoke(
        userId: String,
        initialRoleIds: List<String>
    ): CustomResult<Unit, Exception> {
        val member = Member.create(
            memberId = UserId(userId),
            initialRoleIds = initialRoleIds.map {
                DocumentId(it)
            }
        )
        return when (val createResult = projectMemberRepository.create(member.id, member)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(createResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(createResult.progress)
        }
    }
}
