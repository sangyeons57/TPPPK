package com.example.data.model.local

import androidx.room.Entity
import androidx.room.Index
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType

/**
 * 채널 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(
    tableName = "channels",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["categoryId"])
    ]
)
data class ChannelEntity(
    /**
     * 채널 ID
     */
    val id: String,
    
    /**
     * 카테고리 ID
     */
    val categoryId: String,
    
    /**
     * 프로젝트 ID
     */
    val projectId: String,
    
    /**
     * 채널 이름
     */
    val name: String,
    
    /**
     * 채널 타입
     */
    val type: String,
    
    /**
     * 채널 순서
     */
    val order: Int,
    
    /**
     * 로컬 캐시 저장 시간
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * ChannelEntity를 도메인 모델 Channel로 변환
     * 
     * @return Channel 도메인 모델
     */
    fun toDomain(): Channel {
        return Channel(
            id = id,
            categoryId = categoryId,
            projectId = projectId,
            name = name,
            type = when (type.uppercase()) {
                "VOICE" -> ChannelType.VOICE
                else -> ChannelType.TEXT
            },
            order = order
        )
    }

    companion object {
        /**
         * 도메인 모델 Channel을 ChannelEntity로 변환
         * 
         * @param channel 변환할 Channel 객체
         * @return ChannelEntity
         */
        fun fromDomain(channel: Channel): ChannelEntity {
            return ChannelEntity(
                id = channel.id,
                categoryId = channel.categoryId,
                projectId = channel.projectId,
                name = channel.name,
                type = channel.type.name,
                order = channel.order
            )
        }
    }
} 