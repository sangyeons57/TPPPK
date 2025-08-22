package com.example.feature_chat.ui.components.mention

import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole

/**
 * 멘션 관련 상수
 */
object MentionConstants {
    /**
     * 멘션 감지를 위한 정규식 패턴
     * 영문자, 숫자, 언더스코어, 점, 한글을 포함한 사용자명/역할명 지원
     */
    const val MENTION_REGEX_PATTERN = """@([a-zA-Z0-9_.가-힣]+)"""
}

/**
 * Data class to represent a parsed mention in the text
 */
data class ParsedMention(
    val type: String,
    val id: String,
    val displayName: String,
    val start: Int,
    val end: Int
)

/**
 * Data class to hold processed text with mentions
 */
data class ProcessedText(
    val text: String,
    val mentions: List<ParsedMention>
)

/**
 * Parses mention format @displayName and identifies mention types for styling
 * Returns processed text with mention position information for styling
 */
fun parseMentionsForDisplay(
    originalText: String,
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList()
): ProcessedText {
    val mentionRegex = MentionConstants.MENTION_REGEX_PATTERN.toRegex()
    val mentions = mutableListOf<ParsedMention>()

    mentionRegex.findAll(originalText).forEach { matchResult ->
        val fullMatch = matchResult.value // @displayName
        val displayName = matchResult.groupValues[1] // displayName (without @)

        // Determine mention type and ID by checking against available data
        val (mentionType, mentionId) = when {
            // Check if it's a role mention
            projectRoles.any { it.roleName == displayName } -> {
                val role = projectRoles.find { it.roleName == displayName }!!
                "role" to role.roleId
            }
            // Check if it's a user mention (participants for DM, projectMembers for projects)
            participants.any { it.displayName == displayName } -> {
                val participant = participants.find { it.displayName == displayName }!!
                "user" to participant.userId
            }

            projectMembers.any { it.displayName == displayName } -> {
                val member = projectMembers.find { it.displayName == displayName }!!
                "user" to member.userId
            }
            // Default to user type if not found
            else -> "user" to displayName
        }

        mentions.add(
            ParsedMention(
                type = mentionType,
                id = mentionId,
                displayName = displayName,
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        )
    }

    return ProcessedText(originalText, mentions)
}
