package com.example.domain.model.ui

import java.time.Instant

/**
 * DM 대화 목록 아이템을 위한 UI 모델입니다.
 */
data class DmUiModel(
    val channelId: String, // DM 채널의 고유 ID
    val partnerName: String, // 대화 상대방의 이름
    val partnerProfileImageUrl: String?, // 대화 상대방의 프로필 이미지 URL
    val lastMessage: String?, // 마지막 메시지 내용 (간략히)
    val lastMessageTimestamp: Instant,
    // 추가적인 UI 관련 상태 (예: 온라인 상태, 알림 음소거 여부 등) 필요시 추가
)

/**
 * 메인 화면의 전체 UI 상태를 나타냅니다.
 */
data class MainUiState(
    val isLoadingDms: Boolean = true,
    val dmConversations: List<DmUiModel> = emptyList(),
    val dmsError: String? = null,

    val isLoadingProjects: Boolean = true,
    val projects: List<ProjectUiModel> = emptyList(),
    val projectsError: String? = null,

    val currentScreen: MainScreenType = MainScreenType.DMS // 기본 화면은 DM 목록
)

/**
 * 메인 화면에 표시될 수 있는 화면 타입을 정의합니다.
 */
enum class MainScreenType {
    DMS, PROJECTS, CALENDAR, PROFILE // 예시: 하단 탭에 따른 화면들
}