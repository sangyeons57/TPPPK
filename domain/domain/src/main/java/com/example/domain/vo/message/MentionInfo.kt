package com.example.domain.model.vo.message

import com.example.domain.model.vo.MentionType

data class MentionInfo(
    val type: MentionType,
    val id: String,
    val displayName: String
) {
    companion object {
        const val KEY_TYPE = "type"
        const val KEY_ID = "id"
        const val KEY_DISPLAY_NAME = "displayName"

        fun create(type: MentionType, id: String, displayName: String): MentionInfo {
            return MentionInfo(type, id, displayName)
        }
    }
}
