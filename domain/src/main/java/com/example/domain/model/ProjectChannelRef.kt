package com.example.domain.model.channel

import java.time.Instant

/**
 * 프로젝트 내 채널 참조를 나타내는 데이터 클래스입니다.
 * 새로운 채널 구조에서 프로젝트는 채널 ID만 참조합니다.
 */
data class ProjectChannelRef(
    /**
     * 참조 ID입니다.
     */
    val id: String,
    
    /**
     * 프로젝트 ID입니다.
     */
    val projectId: String,
    
    /**
     * 참조하는 채널 ID입니다.
     */
    val channelId: String,
    
    /**
     * 카테고리 ID입니다. 카테고리에 속하지 않는 직속 채널인 경우 null입니다.
     */
    val categoryId: String?,
    
    /**
     * 채널 표시 순서입니다.
     */
    val order: Int,
    
    /**
     * 생성 시간입니다.
     */
    val createdAt: Instant
) {
    /**
     * 이 채널이 직속 채널인지 여부를 반환합니다.
     */
    val isDirect: Boolean
        get() = categoryId == null
} 