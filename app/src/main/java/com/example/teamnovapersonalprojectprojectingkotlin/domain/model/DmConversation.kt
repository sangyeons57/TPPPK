// 경로: domain/model/DmConversation.kt (HomeViewModel의 DmItem 기반)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

import java.time.LocalDateTime

data class DmConversation(
    val channelId: String, // DM 채널 ID
    val partnerUserId: String,
    val partnerUserName: String,
    val partnerProfileImageUrl: String?,
    val lastMessage: String?, // 마지막 메시지 내용 (간략히)
    val lastMessageTimestamp: LocalDateTime?, // 마지막 메시지 시간
    val unreadCount: Int
)