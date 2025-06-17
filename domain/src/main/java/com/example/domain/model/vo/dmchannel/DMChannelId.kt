package com.example.domain.model.vo.dmchannel

import kotlin.jvm.JvmInline

/**
 * Represents the unique identifier for a DM channel.
 *
 * This value class wraps a [String] and ensures that the ID is not blank.
 */
@JvmInline
value class DMChannelId(val value: String) {
    init {
        require(value.isNotBlank()) { "DMChannel ID는 비어있을 수 없습니다." }
    }
}
