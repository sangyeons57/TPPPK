package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.MemberRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for retrieving a specific project member.
 */
interface GetProjectMemberUseCase {
    /**
     * Retrieves a specific member from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user (member) to retrieve.
     * @return A [Result] containing the [ProjectMember] if found, or null if not found, or an error.
     */
    suspend operator fun invoke(userId: String): Flow<CustomResult<Member, Exception>>
}
/**
 * Implementation of [GetProjectMemberUseCase].
 */
class GetProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : GetProjectMemberUseCase {

    /**
     * Retrieves a specific member from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user (member) to retrieve.
     * @return A [Result] containing the [ProjectMember] if found, or null if not found, or an error.
     */
    override suspend fun invoke(userId: String): Flow<CustomResult<Member, Exception>>  {
        // The repository's getProjectMember already handles forceRefresh if needed by its own logic.
        // Here we directly call it.
        return when(val observeResult = projectMemberRepository.observe(DocumentId.from(userId)).first()){
            is CustomResult.Success -> flowOf(CustomResult.Success(observeResult.data as Member))
            is CustomResult.Failure -> flowOf(CustomResult.Failure(observeResult.error))
            is CustomResult.Initial -> flowOf(CustomResult.Initial)
            is CustomResult.Loading -> flowOf(CustomResult.Loading)
            is CustomResult.Progress -> flowOf(CustomResult.Progress(observeResult.progress))
        }
    }
}
