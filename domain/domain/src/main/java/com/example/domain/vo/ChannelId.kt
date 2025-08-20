package com.example.domain.vo

/**
 * 채널 ID를 나타내는 Value Object
 */
@JvmInline
value class ChannelId(val value: String) {
    init {
        require(value.isNotBlank()) { "ChannelId cannot be blank" }
    }

    // ===== General, delimiter-based helpers =====
    // Standard format example for current app: "ProjectId:ChannelId"
    // You may extend to more segments in the future, e.g., "OrgId:ProjectId:ChannelId".

    /** Returns true if value contains ':' delimiter (composite id). */
    fun hasDelimiter(): Boolean = value.contains(':')

    /** Splits by ':' without trimming. */
    fun segments(): List<String> = value.split(':')

    /** Returns segment at index or null if out-of-range. */
    fun segmentOrNull(index: Int): String? = segments().getOrNull(index)

    fun firstOrNull(): String? = segmentOrNull(0)
    fun secondOrNull(): String? = segmentOrNull(1)
    fun thirdOrNull(): String? = segmentOrNull(2)

    /** Returns the last segment (useful to get the leaf channel id). */
    fun last(): String = segments().last()

    /** Returns the number of segments. */
    fun size(): Int = segments().size

    // ===== Backward-compatible, app-specific helpers =====

    /** Project-style composite? (current convention: "ProjectId:ChannelId") */
    fun isProject(): Boolean = hasDelimiter()

    /** ProjectId for current two-part convention or null if DM (single-part). */
    @Deprecated("Use firstOrNull() for generic access")
    fun projectIdOrNull(): String? = if (isProject()) firstOrNull() else null

    /** Leaf ChannelId for current two-part convention (or whole value for DM). */
    @Deprecated("Use last() for generic access")
    fun pureId(): String = if (isProject()) last() else value

    companion object {
        fun from(value: DocumentId) = ChannelId(value.value)

        /** Compose a composite ChannelId by ':' joining parts. */
        fun compose(vararg parts: String): ChannelId = ChannelId(parts.joinToString(":"))

        /** Specific convenience for current convention: "ProjectId:ChannelId". */
        fun compose(projectId: String, channelId: String): ChannelId =
            ChannelId("$projectId:$channelId")
    }
}
