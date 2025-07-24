package com.example.feature_chat.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole

/**
 * Enhanced input field with mention handling for single-unit deletion
 * Works with String values to maintain compatibility with existing ViewModel
 */

/**
 * Creates a visual transformation that highlights mentions in the input field
 */
private fun createMentionVisualTransformation(
    participants: List<ChatParticipant>,
    projectMembers: List<ProjectMember>,
    projectRoles: List<ProjectRole>,
    userMentionColor: Color,
    userMentionBackground: Color,
    roleMentionColor: Color,
    roleMentionBackground: Color
): VisualTransformation {
    return VisualTransformation { text ->
        val mentionRegex = """@(\w+)""".toRegex()
        val annotatedString = buildAnnotatedString {
            append(text.text)
            
            mentionRegex.findAll(text.text).forEach { matchResult ->
                val mentionName = matchResult.groupValues[1]
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                
                // Check if it's a valid mention
                val isUserMention = participants.any { it.displayName == mentionName } ||
                        projectMembers.any { it.displayName == mentionName }
                val isRoleMention = projectRoles.any { it.roleName == mentionName }
                
                when {
                    isUserMention -> {
                        addStyle(
                            style = SpanStyle(
                                color = userMentionColor,
                                background = userMentionBackground,
                                fontWeight = FontWeight.SemiBold
                            ),
                            start = start,
                            end = end
                        )
                    }
                    isRoleMention -> {
                        addStyle(
                            style = SpanStyle(
                                color = roleMentionColor,
                                background = roleMentionBackground,
                                fontWeight = FontWeight.SemiBold
                            ),
                            start = start,
                            end = end
                        )
                    }
                }
            }
        }
        
        TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentionStyledInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLines: Int = 3,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null
) {
    // Track TextFieldValue for cursor position
    var textFieldValue by remember(value) { 
        mutableStateOf(TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))) 
    }
    
    // Sync with external value changes
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))
        }
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Handle mention deletion and update
            val processedValue = handleMentionDeletion(
                oldValue = textFieldValue,
                newValue = newValue,
                participants = participants,
                projectMembers = projectMembers,
                projectRoles = projectRoles
            )
            
            textFieldValue = processedValue
            onValueChange(processedValue.text)
        },
        modifier = modifier
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                    handleBackspaceForMentions(
                        currentValue = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onValueChange(newValue.text)
                        },
                        participants = participants,
                        projectMembers = projectMembers,
                        projectRoles = projectRoles
                    )
                } else {
                    false
                }
            },
        placeholder = { 
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        visualTransformation = createMentionVisualTransformation(
            participants = participants,
            projectMembers = projectMembers,
            projectRoles = projectRoles,
            userMentionColor = MaterialTheme.colorScheme.primary,
            userMentionBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            roleMentionColor = MaterialTheme.colorScheme.tertiary,
            roleMentionBackground = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Handles backspace key events for mention deletion
 */
private fun handleBackspaceForMentions(
    currentValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    participants: List<ChatParticipant>,
    projectMembers: List<ProjectMember>,
    projectRoles: List<ProjectRole>
): Boolean {
    val text = currentValue.text
    val selection = currentValue.selection
    
    // If no selection or selection is not at the end of a mention, use default behavior
    if (selection.collapsed && selection.start > 0) {
        // Check if cursor is at the end of a mention (with or without trailing space)
        val beforeCursor = text.substring(0, selection.start)
        val mentionRegex = """@(\w+)\s?$""".toRegex()
        val match = mentionRegex.find(beforeCursor)
        
        if (match != null) {
            val mentionName = match.groupValues[1]
            // Verify it's a valid mention
            val isValidMention = participants.any { it.displayName == mentionName } ||
                    projectMembers.any { it.displayName == mentionName } ||
                    projectRoles.any { it.roleName == mentionName }
            
            if (isValidMention) {
                // Delete the entire mention including any trailing space
                val newText = text.removeRange(match.range.first, match.range.last + 1)
                val newSelection = androidx.compose.ui.text.TextRange(match.range.first)
                
                onValueChange(
                    TextFieldValue(
                        text = newText,
                        selection = newSelection
                    )
                )
                return true // Consume the event
            }
        }
    }
    
    return false // Use default behavior
}

/**
 * Handles general text changes to prevent partial mention deletion
 */
private fun handleMentionDeletion(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
    participants: List<ChatParticipant>,
    projectMembers: List<ProjectMember>,
    projectRoles: List<ProjectRole>
): TextFieldValue {
    // If text length increased, no deletion occurred
    if (newValue.text.length >= oldValue.text.length) {
        return newValue
    }
    
    // Find mentions in the old text (including any trailing spaces)
    val mentionRegex = """@(\w+)\s?""".toRegex()
    val oldMentions = mentionRegex.findAll(oldValue.text).toList()
    
    for (mention in oldMentions) {
        val mentionName = mention.groupValues[1]
        val isValidMention = participants.any { it.displayName == mentionName } ||
                projectMembers.any { it.displayName == mentionName } ||
                projectRoles.any { it.roleName == mentionName }
        
        if (!isValidMention) continue
        
        val mentionStart = mention.range.first
        val mentionEnd = mention.range.last + 1
        
        // Check if this mention was partially deleted
        val newTextInMentionRange = newValue.text.getOrNull(mentionStart)?.let { firstChar ->
            if (firstChar == '@') {
                val endIndex = minOf(mentionEnd, newValue.text.length)
                newValue.text.substring(mentionStart, endIndex)
            } else null
        }
        
        // If mention was partially deleted, remove it entirely
        if (newTextInMentionRange != null && 
            newTextInMentionRange != mention.value &&
            newTextInMentionRange.startsWith("@")) {
            
            val textWithoutMention = newValue.text.removeRange(
                mentionStart, 
                minOf(mentionEnd, newValue.text.length)
            )
            
            return TextFieldValue(
                text = textWithoutMention,
                selection = androidx.compose.ui.text.TextRange(mentionStart)
            )
        }
    }
    
    return newValue
}