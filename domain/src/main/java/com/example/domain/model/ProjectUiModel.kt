package com.example.domain.model.ui

// import com.example.domain.model.ChannelType // Not needed if ChannelUiModel is removed or if ChannelType is not used by ProjectUiModel directly

/**
 * 프로젝트 목록 또는 상세 화면에 표시될 프로젝트 UI 모델입니다.
 */
data class ProjectUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String? = null,
    // 추가적인 UI 관련 상태 (예: 로딩 상태, 선택 상태 등) 필요시 추가
)

// /**
//  * 프로젝트 구조 내 카테고리를 나타내는 UI 모델입니다.
//  * 각 카테고리는 채널 목록을 포함할 수 있습니다.
//  */
// data class CategoryUiModel(
//     val id: String,
//     val projectId: String,
//     val name: String,
//     val order: Int,
//     val channels: List<ChannelUiModel> = emptyList()
// )

// /**
//  * 프로젝트 내 채널(카테고리 소속 또는 직속)을 나타내는 UI 모델입니다.
//  */
// data class ChannelUiModel(
//     val id: String,
//     val categoryId: String?, // 직속 채널의 경우 null
//     val projectId: String,
//     val name: String,
//     val type: ChannelType,
//     val order: Int,
//     val isDirect: Boolean, // categoryId가 null이면 true
//     // 추가적인 UI 관련 상태 (예: 읽지 않은 메시지 수, 알림 상태 등) 필요시 추가
// ) 