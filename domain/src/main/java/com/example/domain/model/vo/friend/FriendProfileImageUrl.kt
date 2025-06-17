package com.example.domain.model.vo.friend

import kotlin.jvm.JvmInline

// Assuming URL can be empty or null if no image, so no isNotBlank check here.
// Specific URL validation can be added if required.
@JvmInline
value class FriendProfileImageUrl(val value: String)
