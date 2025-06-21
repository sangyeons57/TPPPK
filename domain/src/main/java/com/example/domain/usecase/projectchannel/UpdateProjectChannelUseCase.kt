package com.example.domain.usecase.projectchannel

import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.base.ProjectChannelRepository
import com.example.core_common.result.CustomResult // Changed import for consistency
import javax.inject.Inject

/**
 * Use case for updating a project channel's details.
 * It handles the business logic for validating and preparing a project channel for update.
 */
interface UpdateProjectChannelUseCase {
    /**
     * Invokes the use case to update a project channel.
     *
     * @param channelToUpdate The original project channel object that needs to be updated.
     * @param newName The new name for the project channel. Must not be blank.
     * @param newOrder The new full order for the project channel (e.g., categoryOrder.channelSubOrder like 1.01).
     *                 Validation should ensure the integer part matches the parent category's order
     *                 and the fractional part is a valid sub-order.
     * @param parentCategoryOrder The order of the parent category (e.g., 1.0). Used for validating the integer part of newOrder.
     * @return A [CustomResult] indicating success (Unit) or failure (Exception).
     */
    suspend operator fun invoke(
        projectId: String,
        channelToUpdate: ProjectChannel,
        newName: String,
        newOrder: Double,
        parentCategoryOrder: Double, // To validate the integer part of newOrder
        channelsInSameCategory: List<ProjectChannel> // For ProjectChannel.updateDetails validation
    ): CustomResult<Unit, Exception>
}

/**
 * Implementation of [UpdateProjectChannelUseCase].
 *
 * @property projectChannelRepository The repository to interact with project channel data.
 */
class UpdateProjectChannelUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : UpdateProjectChannelUseCase {
    override suspend fun invoke(
        projectId: String,
        channelToUpdate: ProjectChannel,
        newName: String,
        newOrder: Double,
        parentCategoryOrder: Double,
        channelsInSameCategory: List<ProjectChannel>
    ): CustomResult<Unit, Exception> {

        // 1. Use ProjectChannel.updateDetails for initial validation and to get a candidate updated channel
        val validatedChannelResult = channelToUpdate.updateDetails(newName, newOrder, channelsInSameCategory)

        val candidateChannel = when (validatedChannelResult) {
            is CustomResult.Success -> validatedChannelResult.data
            is CustomResult.Failure -> return CustomResult.Failure(validatedChannelResult.error) // Propagate error from domain model
            else -> return CustomResult.Failure(Exception("Unexpected result from ProjectChannel"))
        }

        // 2. Perform use-case specific validation (e.g., parentCategoryOrder consistency)
        // Order format: categoryOrder.channelSubOrder (e.g., 1.01)
        // The integer part of candidateChannel.order must match the integer part of parentCategoryOrder.
        // The fractional part (sub-order) must be greater than 0.
        val candidateOrderIntPart = candidateChannel.order.toInt()
        val parentCategoryOrderIntPart = parentCategoryOrder.toInt()

        if (candidateOrderIntPart != parentCategoryOrderIntPart) {
            return CustomResult.Failure(IllegalArgumentException("Channel order's integer part ('${candidateChannel.order}') must match parent category's order ('$parentCategoryOrderIntPart.X')."))
        }

        val subOrder = candidateChannel.order - candidateOrderIntPart
        // Using a small epsilon for floating point comparison to ensure subOrder is strictly positive.
        if (subOrder <= 0.0001) { // e.g., 1.00 is not a valid channel order if category is 1.0
            return CustomResult.Failure(IllegalArgumentException("Channel sub-order ('${candidateChannel.order}') must be greater than 0 (e.g., X.01, X.02)."))
        }

        // 3. If all validations pass, persist the candidateChannel
        // The `updatedAt` field is already set by `candidateChannel.updateDetails`
        return projectChannelRepository.updateProjectChannel(projectId, candidateChannel)
    }
}
