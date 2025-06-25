package com.example.feature_home.model

import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName

// Adjusted package


/**
 * UI data model for a Direct Message item.
 */
data class DmUiModel(
    val channelId: DocumentId,
    val partnerName: UserName?,
    val partnerProfileImageUrl: ImageUrl?, // Added for potential UI needs
    val unreadCount: Int = 0
) {
    // Companion object for creating preview instances easily
    companion object {
        fun preview(): DmUiModel {
            return DmUiModel(
                channelId = DocumentId("dm_preview_id_123"),
                partnerName = UserName("김미리"),
                partnerProfileImageUrl = null, // Or a placeholder image URL
                unreadCount = 2
            )
        }

        fun emptyPreviewList(): List<DmUiModel> = listOf(
            DmUiModel(
                channelId = DocumentId("dm_preview_id_1"),
                partnerName = UserName("이철수"),
                partnerProfileImageUrl = null,
                unreadCount = 1
            ),
            DmUiModel(
                channelId = DocumentId("dm_preview_id_2"),
                partnerName = UserName("박영희"),
                partnerProfileImageUrl = null,
                unreadCount = 0
            )
        )
    }
}
