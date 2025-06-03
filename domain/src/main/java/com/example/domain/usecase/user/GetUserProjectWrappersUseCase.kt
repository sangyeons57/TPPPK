package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get a stream of project wrappers for a given user.
 * Project wrappers are lightweight references to projects the user is part of.
 */
class GetUserProjectWrappersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Invokes the use case to get a stream of project wrappers.
     * @param userId The ID of the user whose project wrappers are to be fetched.
     * @return A Flow emitting CustomResult containing a list of ProjectsWrapper or an Exception.
     */
    operator fun invoke(userId: String): Flow<CustomResult<List<ProjectsWrapper>, Exception>> {
        return userRepository.getProjectWrappersStream(userId)
    }
}
