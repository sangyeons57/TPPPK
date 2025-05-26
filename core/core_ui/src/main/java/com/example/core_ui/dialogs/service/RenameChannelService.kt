package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category
import com.example.domain.model.Channel


class RenameChannelService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var projectId: String = ""

    fun initialize(currentCategories: List<Category>, projectId: String){
        this.currentCategories = currentCategories.toMutableList()
        this.projectId = projectId
        isInitialized = true;
    }

    operator fun invoke(categoryId: String, channelId: String, newName: String): Result<Channel> {
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }
        if (newName.isBlank()) return Result.failure(Exception("New name is blank"))
        val now = DateTimeUtil.nowInstant()
        var selectedChannel: Channel? = null;
        val updatedCategories = currentCategories.map { category ->
            if (category.id == categoryId) {
                val updatedChannels = category.channels.map { channel ->
                    if (channel.id == channelId) {
                        channel.copy(name = newName, updatedAt = now)
                    } else {
                        channel
                    }
                }
                category.copy(channels = updatedChannels, updatedAt = now)
            } else {
                category
            }
        }
        
        currentCategories = updatedCategories.toMutableList()
        if(selectedChannel == null) return Result.failure(Exception("Channel not found"))
        return Result.success(selectedChannel)
    }
}