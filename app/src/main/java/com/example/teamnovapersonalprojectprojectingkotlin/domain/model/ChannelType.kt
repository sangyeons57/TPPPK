// 경로: domain/model/ChannelType.kt (ProjectSettingViewModel, EditChannelViewModel 등 기반)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

enum class ChannelType(val displayName: String) {
    TEXT("텍스트 채널"),
    VOICE("음성 채널")
    // 필요시 다른 타입 추가 (예: 공지 채널, 스테이지 채널)
}