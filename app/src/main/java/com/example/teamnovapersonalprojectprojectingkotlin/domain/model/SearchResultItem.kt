// 경로: domain/model/SearchResultItem.kt (신규 생성 또는 이동)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

import java.time.LocalDateTime // 예시: 메시지 시간

// 검색 결과를 나타내는 Sealed Interface (Domain Model)
sealed interface SearchResultItem {
    val id: String // 각 결과 항목을 식별할 고유 ID (LazyColumn Key 용도)
}

data class MessageResult(
    override val id: String, // 메시지 ID + 채널 ID 조합 등 고유하게
    val channelId: String,
    val channelName: String,
    val messageId: Int,
    val senderName: String,
    val messageContent: String, // 검색어 하이라이팅은 UI 레이어에서 처리
    val timestamp: LocalDateTime
) : SearchResultItem

data class UserResult(
    override val id: String, // 사용자 ID
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val status: String? // 사용자 상태
) : SearchResultItem

// 필요시 다른 결과 타입 추가 (예: ChannelResult, FileResult)