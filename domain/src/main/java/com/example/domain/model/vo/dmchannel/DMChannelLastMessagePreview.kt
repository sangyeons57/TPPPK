package com.example.domain.model.vo.dmchannel

import kotlin.jvm.JvmInline

/**
 * Represents a preview of the last message in a DM channel.
 *
 * This value class wraps a [String] and is used to display a snippet of the most recent message.
 */
@JvmInline
value class DMChannelLastMessagePreview(val value: String)
