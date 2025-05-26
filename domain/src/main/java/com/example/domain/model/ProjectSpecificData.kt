package com.example.domain.model.channel

import com.example.domain.model.ChannelMode


/**
 * 프로젝트/카테고리 채널에 특화된 데이터를 담는 클래스입니다.
 * 프로젝트와 카테고리 구조를 명확하게 표현합니다.
 */
data class ProjectSpecificData(
    /**
     * 채널이 속한 프로젝트 ID
     */
    val projectId: String,
    
    /**
     * 채널이 속한 카테고리 ID (null인 경우 프로젝트 직속 채널)
     */
    val categoryId: String? = null,
    
    /**
     * 카테고리/프로젝트 내 채널 표시 순서
     */
    val order: Int = 0,
    
    /**
     * 채널 모드입니다. TEXT, VOICE 등 채널의 실제 형식을 나타냅니다.
     * FirestoreConstants.ChannelModeValues의 값 중 하나일 수 있습니다.
     */
    val channelMode: ChannelMode = ChannelMode.TEXT
) 