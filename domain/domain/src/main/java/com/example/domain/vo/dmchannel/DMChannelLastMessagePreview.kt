package com.example.domain.vo.dmchannel

/**
 * Represents a preview of the last message in a DM channel.
 *
 * This value class wraps a [String] and is used to display a snippet of the most recent message.
 */
@JvmInline
value class DMChannelLastMessagePreview(val value: String)
