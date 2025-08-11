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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole

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
    text: String,
    isEditing: Boolean = false,
    isEnabled: Boolean = true,
    canSend: Boolean = false,
    onTextChange: (String) -> Unit = {},
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

            // 멘션 제안 팝업 (입력 필드 위에 표시)
            AnimatedVisibility(visible = isMentionSuggestionVisible) {
                MentionSuggestionsPopup(
                    suggestions = mentionSuggestions,
                    onSuggestionClick = onMentionSuggestionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

                // 텍스트 입력창
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            // 포커스가 해제되면 키보드도 숨김으로 간주
                            if (!focusState.isFocused) {
                                isKeyboardVisible = false
                            }
                        },
                    placeholder = {
                        Text(
                            text = if (!isEnabled) "연결 중입니다..."
                            else if (isEditing) "메시지 수정..."
                            else "메시지 입력..."
                        )
                    },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!canSend) return@KeyboardActions
                            if (isEditing) {
                                onCancelEdit()
                            } else {
                                onSendClick()
                            }
                            // 전송 후 포커스 해제 및 키보드 숨김
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // 전송 버튼
                Button(
                    onClick = {
                        if (!canSend) return@Button
                        if (isEditing) onCancelEdit() else onSendClick()
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