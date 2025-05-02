// 경로: domain/model/Channel.kt (ProjectSettingViewModel, ProjectStructure 관련 기반)
package com.example.domain.model

data class Channel(
    val id: String,
    val categoryId: String, // 속한 카테고리 ID
    val projectId: String,
    val name: String,
    val type: ChannelType,
    val order: Int // 채널 순서
)