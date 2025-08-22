package com.example.feature_chat.ui.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole
import com.example.feature_chat.ui.components.mention.MentionConstants
import java.util.regex.Pattern

/**
 * 개선된 메시지 입력 컴포넌트
 * 키보드 높이 자동 조정, 포커스 관리, 스크롤 위치 조정, 멘션 기능을 포함합니다.
 *
 * @param text 입력 텍스트
 * @param isEditing 메시지 수정 모드 여부
 * @param isEnabled 입력 활성화 여부
 * @param onTextChange 텍스트 변경 이벤트
 * @param onSendClick 전송 버튼 클릭 이벤트
 * @param onAttachmentClick 첨부파일 버튼 클릭 이벤트
 * @param onCancelEdit 수정 취소 이벤트
 * @param onKeyboardStateChange 키보드 상태 변경 이벤트 (포커스 + 키보드 상태)
 * @param onScrollToBottom 스크롤을 최하단으로 이동하는 이벤트
 * @param onMentionSuggestionClick 멘션 제안 클릭 이벤트
 * @param participants 채팅 참가자 목록 (DM 채널용)
 * @param projectMembers 프로젝트 멤버 목록 (프로젝트 채널용)
 * @param projectRoles 프로젝트 역할 목록 (프로젝트 채널용)
 * @param mentionSuggestions 멘션 제안 목록
 * @param isMentionSuggestionVisible 멘션 제안 표시 여부
 */
@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    isEditing: Boolean = false,
    isEnabled: Boolean = true,
    canSend: Boolean = false,
    onValueChange: (TextFieldValue) -> Unit = {},
    onSendClick: () -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onKeyboardStateChange: (Boolean) -> Unit = {},
    onScrollToBottom: () -> Unit = {},
    onMentionSuggestionClick: (MentionSuggestion) -> Unit = {},
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList(),
    mentionSuggestions: List<MentionSuggestion> = emptyList(),
    isMentionSuggestionVisible: Boolean = false,
    maxMentionItems: Int = 7,
    // 키보드 멘션 네비게이션
    onMentionKeyboardNavigation: ((String) -> Unit)? = null, // "up", "down", "enter"
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 키보드 상태 감지를 위한 포커스 상태
    var isFocused by remember { mutableStateOf(false) }
    var isKeyboardVisible by remember { mutableStateOf(false) }

    // 키보드 높이에 따른 애니메이션 (더 부드러운 전환)
    val animatedPadding by animateDpAsState(
        targetValue = if (isFocused && isKeyboardVisible) 12.dp else 4.dp,
        animationSpec = tween(durationMillis = 300),
        label = "keyboard_padding"
    )

    // 키보드 상태 변경 시 콜백 호출
    LaunchedEffect(isFocused, isKeyboardVisible) {
        val keyboardState = isFocused && isKeyboardVisible
        onKeyboardStateChange(keyboardState)

        // 키보드가 나타날 때 스크롤을 최하단으로 이동
        if (keyboardState) {
            onScrollToBottom()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = animatedPadding)
        ) {
            // 수정 모드 표시줄
            AnimatedVisibility(visible = isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = "메시지 수정 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            onCancelEdit()
                            // 수정 취소 시 포커스 해제 및 키보드 숨김
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "수정 취소",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 멘션 제안 팝업은 ChatScreen의 content overlay로 이동 (bottomBar 클리핑 회피)

            // 메시지 입력 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 첨부파일 버튼
                IconButton(
                    onClick = onAttachmentClick,
                    enabled = isEnabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "첨부파일 추가",
                        tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.5f
                        )
                    )
                }

                // 텍스트 입력창 (멘션 하이라이팅 지원)
                var internalTextFieldValue by remember { mutableStateOf(textFieldValue) }

                // 외부 상태와 동기화
                LaunchedEffect(textFieldValue) {
                    if (internalTextFieldValue != textFieldValue) {
                        internalTextFieldValue = textFieldValue
                    }
                }

                BasicTextField(
                    value = internalTextFieldValue,
                    onValueChange = { newValue ->
                        internalTextFieldValue = newValue
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .border(
                            width = 1.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(12.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            // 포커스가 해제되면 키보드도 숨김으로 간주
                            if (!focusState.isFocused) {
                                isKeyboardVisible = false
                            }
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            // 멘션 제안이 보이는 상태에서 키보드 네비게이션 처리
                            if (isMentionSuggestionVisible && keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionUp -> {
                                        onMentionKeyboardNavigation?.invoke("up")
                                        true // 이벤트 소비
                                    }

                                    Key.DirectionDown -> {
                                        onMentionKeyboardNavigation?.invoke("down")
                                        true // 이벤트 소비
                                    }

                                    Key.Enter -> {
                                        onMentionKeyboardNavigation?.invoke("enter")
                                        true // 이벤트 소비
                                    }

                                    else -> false // 다른 키는 기본 처리
                                }
                            } else {
                                false // 멘션 모드가 아니면 기본 처리
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!canSend) return@KeyboardActions
                            onSendClick()
                            // 전송 후 포커스 해제 및 키보드 숨김
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (internalTextFieldValue.text.isEmpty()) {
                            Text(
                                text = if (!isEnabled) "연결 중입니다..."
                                else if (isEditing) "메시지 수정..."
                                else "메시지 입력...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Display highlighted text for mentions
                        Text(
                            text = createHighlightedText(internalTextFieldValue.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                )

                // 전송 버튼
                Button(
                    onClick = {
                        if (!canSend) return@Button
                        onSendClick()
                        // 전송 후 포커스 해제 및 키보드 숨김
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    enabled = isEnabled && canSend,
                    shape = CircleShape,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isEditing) "수정 완료" else "메시지 전송"
                    )
                }
            }
        }
    }

    // 키보드 상태 감지 (포커스 상태와 함께)
    LaunchedEffect(isFocused) {
        if (isFocused) {
            // 포커스가 설정되면 키보드가 나타날 것으로 예상
            // 실제 키보드 상태는 시스템에서 감지하기 어려우므로 포커스 상태로 추정
            isKeyboardVisible = true
        }
    }

    // 외부에서 키보드 숨김을 위한 함수
    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
        isKeyboardVisible = false
    }
}

/**
 * 텍스트에서 멘션을 감지하고 하이라이팅된 AnnotatedString 생성
 */
@Composable
private fun createHighlightedText(
    text: String,
    mentionColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    mentionBackgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = 0.2f
    )
): AnnotatedString {
    return buildAnnotatedString {
        val mentionPattern = Pattern.compile(MentionConstants.MENTION_REGEX_PATTERN)
        val matcher = mentionPattern.matcher(text)
        var lastIndex = 0

        while (matcher.find()) {
            // 멘션 이전 텍스트 추가
            if (matcher.start() > lastIndex) {
                append(text.substring(lastIndex, matcher.start()))
            }

            // 멘션 텍스트를 하이라이팅하여 추가
            withStyle(
                style = SpanStyle(
                    color = mentionColor,
                    background = mentionBackgroundColor
                )
            ) {
                append(matcher.group())
            }

            lastIndex = matcher.end()
        }

        // 마지막 멘션 이후 텍스트 추가
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
} 
