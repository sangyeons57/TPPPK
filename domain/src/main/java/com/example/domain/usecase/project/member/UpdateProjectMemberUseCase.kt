package com.example.domain.usecase.project.member

import kotlin.Result

/**
 * UseCase for updating a project member's roles and channel access.
 */
interface UpdateProjectMemberUseCase {
    /**
     * Updates a project member's roles and/or channel access.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user (member) to update.
     * @param roleIds Optional. If provided, the member's roles will be updated to this list.
     * @param channelIdsToAdd Optional. If provided, these channel IDs will be added to the member's accessible channels.
     * @param channelIdsToRemove Optional. If provided, these channel IDs will be removed from the member's accessible channels.
     * @return A [Result] indicating success or failure. If multiple operations are performed,
     *         it returns the first failure or success if all operations succeed.
     */
    suspend operator fun invoke(
        projectId: String,
        userId: String,
        roleIds: List<String>? = null,
        channelIdsToAdd: List<String>? = null,
        channelIdsToRemove: List<String>? = null
    ): Result<Unit>
}
