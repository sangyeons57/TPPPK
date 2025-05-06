package com.example.domain.model

/**
 * 프로젝트 카테고리 정보를 나타내는 데이터 클래스 (도메인 모델)
 * 하위 채널 목록을 포함합니다.
 */
data class ProjectCategory(
    val id: String,
    val name: String,
    val channels: List<ProjectChannel> = emptyList()
) 