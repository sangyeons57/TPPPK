package com.example.domain.model.ui.data

import com.example.domain.model.vo.Name
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import java.time.Instant

data class MemberUiModel(
    val userId: UserId,
    val userName: UserName,
    val roleNames: List<Name>,
    val joinedAt: Instant?
)
