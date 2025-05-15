// 경로: domain/model/ProjectStructure.kt (ProjectSettingViewModel 용, 선택적)
// 또는 Category, Channel 리스트를 개별적으로 반환받을 수도 있음
package com.example.domain.model

/**
 * 프로젝트의 전체 구조(카테고리, 직속 채널)를 나타내는 데이터 클래스
 */
data class ProjectStructure(
    /**
     * 프로젝트 내 카테고리 목록 (각 카테고리는 채널 목록을 포함할 수 있음).
     */
    val categories: List<Category> = emptyList(),

    /**
     * 프로젝트에 직접 속한 채널 목록 (카테고리 없음).
     */
    val directChannels: List<Channel> = emptyList()
)