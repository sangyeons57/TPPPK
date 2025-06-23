package com.example.feature_home.model

import com.example.core_common.util.DateTimeUtil
import java.time.Instant

// Adjusted package


/**
 * UI data model for a Direct Message item.
 */
data class DmUiModel(
    val channelId: String,
    val partnerName: String?,
    val partnerProfileImageUrl: String?, // Added for potential UI needs
    val lastMessage: String?,
    val lastMessageTimestamp: Instant?,
    val unreadCount: Int = 0 // Example: useful for DMs
) {
    // Companion object for creating preview instances easily
    companion object {
        fun preview(): DmUiModel {
            return DmUiModel(
                channelId = "dm_preview_id_123",
                partnerName = "김미리",
                partnerProfileImageUrl = null, // Or a placeholder image URL
                lastMessage = "안녕하세요! 프로젝트 관련해서 이야기 나누고 싶습니다. 시간 괜찮으신가요?",
                lastMessageTimestamp = DateTimeUtil.nowInstant(),
                unreadCount = 2
            )
        }

        fun emptyPreviewList(): List<DmUiModel> = listOf(
            DmUiModel(
                channelId = "dm_preview_id_1",
                partnerName = "이철수",
                partnerProfileImageUrl = null,
                lastMessage = "네, 확인했습니다.",
                lastMessageTimestamp = DateTimeUtil.epochMilliToInstant(System.currentTimeMillis() - 1000 * 60 * 5), // 5 mins ago
                unreadCount = 0
            ),
            DmUiModel(
                channelId = "dm_preview_id_2",
                partnerName = "박영희",
                partnerProfileImageUrl = null,
                lastMessage = "좋아요! 그럼 그때 뵙겠습니다.",
                lastMessageTimestamp = DateTimeUtil.epochMilliToInstant(System.currentTimeMillis() - 1000 * 60 * 60 * 2), // 2 hours ago
                unreadCount = 3
            )
        )
    }
}
