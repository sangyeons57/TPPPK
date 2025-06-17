package com.example.domain.model.vo.friend

import kotlin.jvm.JvmInline

@JvmInline
value class FriendName(val value: String) {
    init {
        // Add any validation if necessary, e.g., length constraints
        require(value.isNotBlank()) { "Friend 이름은 비어있을 수 없습니다." }
    }
}
