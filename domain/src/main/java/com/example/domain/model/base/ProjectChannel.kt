package com.example.domain.model.base

import com.example.domain.model.enum.ProjectChannelType
import com.google.firebase.firestore.DocumentId
import java.time.Instant

import com.example.core_common.result.CustomResult
import java.time.ZoneId

/**
 * Represents a channel within a project category.
 *
 * @property id Unique identifier of the channel, typically set by Firestore.
 * @property channelName The name of the channel.
 * @property order The display order of the channel within its category. Lower numbers appear first.
 * @property channelType The type of the channel (e.g., MESSAGES, TASKS).
 * @property createdAt Timestamp of when the channel was created.
 * @property updatedAt Timestamp of when the channel was last updated.
 */
data class ProjectChannel(
    @DocumentId val id: String = "",
    val channelName: String = "",
    val order: Double = 0.0,
    val channelType: ProjectChannelType = ProjectChannelType.MESSAGES, // "MESSAGES", "TASKS" 등
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    /**
     * Updates the channel's details after validation.
     *
     * @param newName The proposed new name for the channel.
     * @param newOrder The proposed new order for the channel.
     * @param channelsInSameCategory A list of all other channels in the same category, used for order validation.
     * @return A [CustomResult] containing the updated [ProjectChannel] on success, or an [Exception] on failure.
     */
    fun updateDetails(
        newName: String,
        newOrder: Double,
        channelsInSameCategory: List<ProjectChannel>
    ): CustomResult<ProjectChannel, Exception> {
        // Validate name
        if (newName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Channel name cannot be empty."))
        }

        // Validate order
        if (newOrder <= 0) {
            return CustomResult.Failure(IllegalArgumentException("Channel order must be a positive number."))
        }

        // Check for order uniqueness within the same category (excluding itself if it's an update)
        val conflictingChannel = channelsInSameCategory.find { it.id != this.id && it.order == newOrder }
        if (conflictingChannel != null) {
            return CustomResult.Failure(IllegalArgumentException("Channel order '%.2f' already exists in this category.".format(newOrder)))
        }

        // All validations passed, create a new instance with updated values
        return CustomResult.Success(
            this.copy(
                channelName = newName,
                order = newOrder,
                updatedAt = Instant.now().atZone(ZoneId.systemDefault()).toInstant() // Ensure UTC or consistent timezone
            )
        )
    }
}

