package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category


class DeleteChannelService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var projectId: String = ""

    fun initialize(currentCategories: List<Category>, projectId: String){
        this.currentCategories = currentCategories.toMutableList()
        this.projectId = projectId
        isInitialized = true;
    }

    operator fun invoke(categoryId: String, channelId: String): Result<Boolean>{
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }

        val now = DateTimeUtil.nowInstant() 
        val updatedCategories = currentCategories.map { category ->
            if (category.id == categoryId) {
                // 채널 삭제 후 남은 채널들의 순서를 재정렬
                val updatedChannels = category.channels
                    .filter { it.id != channelId }
                    .mapIndexed { index, channel ->
                        channel.copy(
                            projectSpecificData = channel.projectSpecificData?.copy(order = index),
                            updatedAt = now
                        )
                    }
                category.copy(channels = updatedChannels, updatedAt = now)
            } else {
                category
            }
        }
        
        currentCategories = updatedCategories.toMutableList()
        return Result.success(true)
    }

}