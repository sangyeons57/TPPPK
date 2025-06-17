package com.example.domain.model.vo.friend

import kotlin.jvm.JvmInline

@JvmInline
value class FriendId(val value: String) {
    init {
        require(value.isNotBlank()) { "Friend ID는 비어있을 수 없습니다." }
    }
}
