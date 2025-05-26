package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.channel.ProjectSpecificData
import java.util.UUID
import kotlin.collections.map

class AddChannelService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var projectId: String = ""

    fun initialize(currentCategories: List<Category>, projectId: String){
        this.currentCategories = currentCategories.toMutableList()
        this.projectId = projectId
        isInitialized = true;

    }

    operator fun invoke (channel: Channel) : Result<Channel>{
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }

        val newChannelId = UUID.randomUUID().toString()
        val now = DateTimeUtil.nowInstant()
        var newChannel: Channel? = null;
        
        val updatedCategories = currentCategories.map { category ->
            if (category.id == channel.projectSpecificData?.categoryId) {
                newChannel = Channel(
                    id = newChannelId,
                    name = channel.name,
                    description = null, // 기본값
                    type = ChannelType.PROJECT, // 채널은 카테고리 내에 속함
                    projectSpecificData = ProjectSpecificData(
                        projectId = projectId,
                        categoryId = channel.projectSpecificData?.categoryId,
                        order = category.channels.size, // 새 채널은 마지막 순서
                    ),
                    dmSpecificData = null, // 프로젝트 채널이므로 null
                    lastMessagePreview = null,
                    lastMessageTimestamp = null,
                    createdAt = now,
                    updatedAt = now,
                    createdBy = null // createdBy는 현재 ViewModel에서 알 수 없으므로 null
                )
                val updatedChannels = category.channels.toMutableList().apply {
                    add(newChannel)
                }
                category.copy(channels = updatedChannels, updatedAt = now) // 카테고리 업데이트 시간 변경
            } else {
                category
            }
        }
        
        currentCategories = updatedCategories.toMutableList()
        if(newChannel == null) return Result.failure(Exception("Channel not found"))
        return Result.success(newChannel)
    }

    fun getCategories(): List<Category> = currentCategories.toList()
}