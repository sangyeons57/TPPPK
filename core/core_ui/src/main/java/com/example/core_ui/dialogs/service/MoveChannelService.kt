package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.dialogs.viewmodel.ContextMenuState
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.Project
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.update

/**
 * 채널 순서를 변경하거나 다른 카테고리로 이동합니다.
 * @param fromCategoryId 원본 카테고리 ID
 * @param fromIndex 이동할 채널의 현재 인덱스
 * @param toCategoryId 대상 카테고리 ID
 * @param toIndex 이동할 목표 인덱스
 */
class MoveChannelService() {
    var isInitialized: Boolean = false;

    // 내부적으로 원본 ProjectStructure와 현재 편집 중인 categories, directChannels 리스트를 관리
    private var originalStructure: ProjectStructure? = null
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var currentDirectChannels: MutableList<Channel> = mutableListOf()
    private var projectId: String = "" // 프로젝트 ID도 필요

    fun initialize(initialStructure: ProjectStructure, projectId: String) {
        this.originalStructure = initialStructure
        this.projectId = projectId // projectId 설정
        this.currentCategories = initialStructure.categories.toMutableList()
        this.currentDirectChannels = initialStructure.directChannels.toMutableList()

        isInitialized = true;
    }

    suspend operator fun invoke(
        fromCategoryId: String? = null, // Null if dragging from direct channels
        fromChannelId: String,
        fromIndex: Int,
        toCategoryId: String? = null, // Null if dropping to direct channels
        toIndex: Int, // Index within target (category or direct list)
        ): Result<Boolean>{
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }
        if (fromCategoryId == toCategoryId && fromIndex == toIndex && fromCategoryId != null) return Result.failure(Exception("No change within the same category")) // No change within the same category
        if (fromCategoryId == null && toCategoryId == null && fromIndex == toIndex) return Result.failure(Exception("No change within direct channels")) // No change within direct channels

        val now = DateTimeUtil.nowInstant()
        var movedChannel: Channel? = null

        // 1. Remove channel from source
        if (fromCategoryId != null) {
            val sourceCategoryIndex = currentCategories.indexOfFirst { it.id == fromCategoryId }
            sourceCategoryIndex.takeIf { it == -1 }?. let {
                return Result.failure(Exception("Source category not found"))
            }
            val sourceCategory = currentCategories[sourceCategoryIndex]
            val mutableSourceChannels = sourceCategory.channels.toMutableList()
            movedChannel = mutableSourceChannels.find { it.id == fromChannelId } ?: return Result.failure(Exception("Channel not found"))
            mutableSourceChannels.remove(movedChannel)
            currentCategories[sourceCategoryIndex] = sourceCategory.copy(
                channels = mutableSourceChannels.mapIndexed { idx, ch -> ch.copy(projectSpecificData = ch.projectSpecificData?.copy(order = idx), updatedAt = now) },
                updatedAt = now
            )
        } else { // Source is direct channels
            movedChannel = currentDirectChannels.find { it.id == fromChannelId } ?: return Result.failure(Exception("Channel not found"))
            currentDirectChannels.remove(movedChannel)
            // Re-order direct channels is done after adding to target, or at the end if target is also direct
        }

        // 2. Add channel to target
        if (toCategoryId != null) {
            val targetCategoryIndex = currentCategories.indexOfFirst { it.id == toCategoryId }
            if (targetCategoryIndex == -1) return Result.failure(Exception("Target category not found")) // Should not happen if UI is correct
            val targetCategory = currentCategories[targetCategoryIndex]
            val mutableTargetChannels = targetCategory.channels.toMutableList()
            val updatedMovedChannel = movedChannel.copy(
                projectSpecificData = movedChannel.projectSpecificData?.copy(categoryId = toCategoryId, order = toIndex),
                updatedAt = now,
                type = ChannelType.PROJECT // Ensure type is PROJECT when moved to a category
            )
            mutableTargetChannels.add(toIndex.coerceIn(0, mutableTargetChannels.size), updatedMovedChannel)
            currentCategories[targetCategoryIndex] = targetCategory.copy(
                channels = mutableTargetChannels.mapIndexed { idx, ch -> ch.copy(projectSpecificData = ch.projectSpecificData?.copy(order = idx), updatedAt = now) },
                updatedAt = now
            )
        } else { // Target is direct channels
            val updatedMovedChannel = movedChannel.copy(
                projectSpecificData = null, // No categoryId or order within category for direct channels
                dmSpecificData = null, // Assuming direct channels in project structure are not DMs
                type = ChannelType.PROJECT, // Or a new type to distinguish if needed
                updatedAt = now

            )
            currentDirectChannels.add(toIndex.coerceIn(0, currentDirectChannels.size), updatedMovedChannel)
            // Re-order direct channels after add
            currentDirectChannels = currentDirectChannels.mapIndexed { idx, ch ->
                ch.copy(updatedAt = if(ch.id == updatedMovedChannel.id) now else ch.updatedAt) // Update timestamp for moved, map order if needed by a specific field
            }.toMutableList()
        }

        // If source was direct channels and target is also direct channels, re-order direct channels now
        if (fromCategoryId == null && toCategoryId == null) {
            val channelToMove = currentDirectChannels.find { it.id == fromChannelId } ?: return Result.failure(Exception("Channel not found"))
            currentDirectChannels.remove(channelToMove)
            currentDirectChannels.add(toIndex.coerceIn(0, currentDirectChannels.size), channelToMove.copy(updatedAt = now))
            currentDirectChannels = currentDirectChannels.mapIndexed { idx, ch ->
                // Potentially update an 'order' field if direct channels have one
                ch.copy(updatedAt = if (ch.updatedAt != now) ch.updatedAt else now)
            }.toMutableList()
        }

        return Result.success(true)
    }

    fun getCategoriesForUi(): List<Category> = currentCategories.toList()
    fun getDirectChannelsForUi(): List<Channel> = currentDirectChannels.toList()
}

