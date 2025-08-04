package com.example.domain.model.vo

/**
 * 채널 ID를 나타내는 Value Object
 */
@JvmInline
value class ChannelId(val value: String) {
    init {
        require(value.isNotBlank()) { "ChannelId cannot be blank" }
    }
} 