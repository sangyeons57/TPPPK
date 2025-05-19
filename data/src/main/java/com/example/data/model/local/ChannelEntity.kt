package com.example.data.model.local

import androidx.room.Entity
import androidx.room.Index
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.channel.DmSpecificData
import com.example.domain.model.channel.ProjectSpecificData
import java.time.Instant

/**
 * 채널 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 * 모든 유형의 채널(DM, 프로젝트, 카테고리)에 사용됩니다.
 */
@Entity(
    tableName = "channels",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["projectId"]) // 프로젝트 ID로 인덱스 생성
    ]
)
data class ChannelEntity(
    /**
     * 채널 ID (Firestore Document ID)
     */
    val id: String,

    /**
     * 채널 이름
     */
    val name: String,

    /**
     * 채널 설명
     */
    val description: String? = null,

    /**
     * 채널 타입 (DM, PROJECT, CATEGORY)
     */
    val type: String,

    /**
     * 채널 모드 (TEXT, VOICE 등)
     */
    val channelMode: ChannelMode = ChannelMode.UNKNOWN,

    /**
     * 프로젝트 ID (프로젝트/카테고리 채널인 경우에만 사용)
     */
    val projectId: String? = null,

    /**
     * 카테고리 ID (카테고리 채널인 경우에만 사용)
     */
    val categoryId: String? = null,

    /**
     * 채널 표시 순서 (프로젝트/카테고리 채널인 경우에만 사용)
     */
    val channelOrder: Int = 0,

    /**
     * 채널 참여자 ID 목록 (DM 채널인 경우에만 사용)
     * 쉼표로 구분된 문자열
     */
    val participantIds: String = "",

    /**
     * 마지막 메시지 미리보기
     */
    val lastMessagePreview: String? = null,

    /**
     * 마지막 메시지 타임스탬프 (에포크 밀리초)
     */
    val lastMessageTimestamp: Long = 0,

    /**
     * 채널 생성 시간 (에포크 밀리초)
     */
    val createdAt: Long = 0,

    /**
     * 채널 업데이트 시간 (에포크 밀리초)
     */
    val updatedAt: Long = 0,

    /**
     * 채널 생성자 ID
     */
    val createdBy: String? = null,

    /**
     * 로컬 캐시 저장 시간 (Epoch millis)
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * ChannelEntity를 도메인 모델 Channel로 변환
     * 
     * @return Channel 도메인 모델
     */
    fun toDomain(): Channel {
        // 참여자 ID 문자열을 리스트로 변환
        val participantsList = if (participantIds.isNotEmpty()) {
            participantIds.split(",")
        } else {
            emptyList()
        }
        
        // 채널 타입에 따라 특화 데이터 생성
        val channelType = try {
            ChannelType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            ChannelType.PROJECT // 기본값으로 PROJECT 사용
        }
        
        // 프로젝트 특화 데이터
        val projectData = if (channelType == ChannelType.PROJECT || channelType == ChannelType.PROJECT) {
            projectId?.let {
                ProjectSpecificData(
                    projectId = it,
                    categoryId = categoryId,
                    order = channelOrder,
                    channelMode = channelMode
                )
            }
        } else null
        
        // DM 특화 데이터
        val dmData = if (channelType == ChannelType.DM) {
            DmSpecificData(participantIds = participantsList)
        } else null
        
        return Channel(
            id = id,
            name = name,
            description = description,
            type = channelType,
            projectSpecificData = projectData,
            dmSpecificData = dmData,
            lastMessagePreview = lastMessagePreview,
            lastMessageTimestamp = if (lastMessageTimestamp > 0) Instant.ofEpochMilli(lastMessageTimestamp) else null,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            createdBy = createdBy
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
                name = channel.name,
                description = channel.description,
                type = channel.type.name,
                // 프로젝트 특화 데이터
                projectId = channel.projectSpecificData?.projectId,
                categoryId = channel.projectSpecificData?.categoryId,
                channelOrder = channel.projectSpecificData?.order ?: 0,
                channelMode = channel.projectSpecificData?.channelMode ?: ChannelMode.UNKNOWN,
                // DM 특화 데이터
                participantIds = channel.dmSpecificData?.participantIds?.joinToString(",") ?: "",
                // 기타 필드
                lastMessagePreview = channel.lastMessagePreview,
                lastMessageTimestamp = channel.lastMessageTimestamp?.toEpochMilli() ?: 0,
                createdAt = channel.createdAt.toEpochMilli(),
                updatedAt = channel.updatedAt.toEpochMilli(),
                createdBy = channel.createdBy,
                // cachedAt은 기본값 사용
            )
        }
    }
} 