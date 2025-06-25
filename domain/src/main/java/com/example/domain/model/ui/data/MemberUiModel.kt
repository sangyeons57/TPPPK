package com.example.domain.model.ui.data

import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import java.time.Instant

data class MemberUiModel(
    val userId: UserId,
    val userName: UserName,
    val profileImageUrl: ImageUrl?,
    val roleNames: List<Name>, // List of role names for display
    val joinedAt: Instant? // Keep joinedAt if needed, or remove
)