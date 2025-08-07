package com.example.feature_chat.ui.components.mention

import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole

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
 * Mention Display Gateway System
 * Handles conversion between internal [type:id] format and user-visible @name format
 */
object MentionDisplayGateway {

    /**
     * Converts internal [type:id] format to user-visible @name format for display
     */
    fun convertToDisplayFormat(
        internalText: String,
        participants: List<ChatParticipant> = emptyList(),
        projectMembers: List<ProjectMember> = emptyList(),
        projectRoles: List<ProjectRole> = emptyList()
    ): String {
        val mentionRegex = """\[(user|role):([^\]]+)\]""".toRegex()

        return mentionRegex.replace(internalText) { matchResult ->
            val type = matchResult.groupValues[1]
            val id = matchResult.groupValues[2]

            when (type) {
                "user" -> {
                    // Try to find actual name in participants/members
                    val userName =
                        participants.find { participant -> participant.userId == id }?.displayName
                            ?: projectMembers.find { member -> member.userId == id }?.displayName
                            ?: id // fallback to id if name not found
                    "@$userName"
                }

                "role" -> {
                    // Try to find actual role name
                    val roleName = projectRoles.find { role -> role.roleId == id }?.roleName
                        ?: id // fallback to id if role name not found
                    "@$roleName"
                }

                else -> "@$id"
            }
        }
    }

    /**
     * Converts user-visible @name format back to internal [type:id] format for storage
     */
    fun convertToInternalFormat(
        displayText: String,
        participants: List<ChatParticipant> = emptyList(),
        projectMembers: List<ProjectMember> = emptyList(),
        projectRoles: List<ProjectRole> = emptyList()
    ): String {
        // This would be used when user types @name and we need to convert it back
        // For now, we handle this through the suggestion system
        return displayText
    }

    /**
     * Extracts mentions from internal format and returns display names
     */
    fun extractMentionDisplayNames(
        internalText: String,
        participants: List<ChatParticipant> = emptyList(),
        projectMembers: List<ProjectMember> = emptyList(),
        projectRoles: List<ProjectRole> = emptyList()
    ): Map<String, String> {
        val mentionRegex = """\[(user|role):([^\]]+)\]""".toRegex()
        val mentionMap = mutableMapOf<String, String>()

        mentionRegex.findAll(internalText).forEach { matchResult ->
            val type = matchResult.groupValues[1]
            val id = matchResult.groupValues[2]
            val key = "[$type:$id]"

            val displayName = when (type) {
                "user" -> {
                    participants.find { participant -> participant.userId == id }?.displayName
                        ?: projectMembers.find { member -> member.userId == id }?.displayName
                        ?: id
                }

                "role" -> {
                    projectRoles.find { role -> role.roleId == id }?.roleName ?: id
                }

                else -> id
            }

            mentionMap[key] = displayName
        }

        return mentionMap
    }
}

/**
 * Parses mention format [type:id] and converts to @displayName for display
 * Returns processed text with mention position information for styling
 */
fun parseMentionsForDisplay(
    originalText: String,
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList()
): ProcessedText {
    val mentionRegex = """\[(user|role):([^\]]+)\]""".toRegex()
    val mentions = mutableListOf<ParsedMention>()
    var processedText = originalText
    var offset = 0

    mentionRegex.findAll(originalText).forEach { matchResult ->
        val fullMatch = matchResult.value
        val mentionType = matchResult.groupValues[1]
        val mentionId = matchResult.groupValues[2]

        // Use MentionDisplayGateway to get proper display name
        val displayName = when (mentionType) {
            "user" -> {
                val userName =
                    participants.find { participant -> participant.userId == mentionId }?.displayName
                        ?: projectMembers.find { member -> member.userId == mentionId }?.displayName
                        ?: mentionId
                "@$userName"
            }

            "role" -> {
                val roleName = projectRoles.find { role -> role.roleId == mentionId }?.roleName
                    ?: mentionId
                "@$roleName"
            }

            else -> "@$mentionId"
        }

        // Calculate positions in the processed text
        val mentionStart = matchResult.range.first - offset
        val mentionEnd = mentionStart + displayName.length

        // Replace the [type:id] format with @displayName
        processedText = processedText.replaceFirst(fullMatch, displayName)

        mentions.add(
            ParsedMention(
                type = mentionType,
                id = mentionId,
                displayName = displayName,
                start = mentionStart,
                end = mentionEnd
            )
        )

        // Update offset for next replacements
        offset += fullMatch.length - displayName.length
    }

    return ProcessedText(processedText, mentions)
}
