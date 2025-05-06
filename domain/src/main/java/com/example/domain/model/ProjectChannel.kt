package com.example.domain.model

/**
 * 프로젝트 채널 정보를 나타내는 데이터 클래스 (도메인 모델)
 */
data class ProjectChannel(
    val id: String,
    val name: String,
    val type: ChannelType
) 