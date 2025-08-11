package com.example.feature_chat.ui.components.message

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core_ui.components.attachment.AttachmentRenderer
import com.example.core_ui.components.user.SimpleUserProfileImage
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole
import com.example.feature_chat.ui.components.mention.parseMentionsForDisplay
import com.example.feature_chat.ui.components.system.ChatStartSystemMessage
import com.example.feature_chat.ui.components.system.DateSystemMessage
import com.example.feature_chat.ui.components.system.MemberInvitationSystemMessage
import com.example.feature_chat.ui.components.system.ProjectJoinSystemMessage


@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatMessageItemComposable(
    message: ChatMessageUiModel,
    isFirstInGroup: Boolean = true,
    onLongClick: () -> Unit,
    onUserProfileClick: () -> Unit,
    onMentionClick: (String, String) -> Unit = { _, _ -> }, // (type, id) -> Unit
    onRetryMessage: (String) -> Unit = { _ -> }, // 재전송 콜백 추가
    onJoinProject: (String) -> Unit = { _ -> }, // 프로젝트 참여 콜백 추가
    onAddMember: (String, String) -> Unit = { _, _ -> }, // 멤버 추가 콜백 추가 (projectId, targetUserId)
    onImageClick: (String, List<String>, Int) -> Unit = { _, _, _ -> }, // 이미지 클릭 콜백 추가
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList(),
    modifier: Modifier = Modifier
) {
    LocalContext.current

    // 메시지 타입에 따라 다른 UI 렌더링
    when (message.messageType) {
        MessageType.TEXT -> {
            // 기존 일반 메시지 UI
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .combinedClickable(
                        onClick = { /* 일반 클릭은 Bubble 자체에는 불필요할 수 있음 */ },
                        onLongClick = onLongClick
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFirstInGroup) {
                    SimpleUserProfileImage(
                        imageUrl = message.userProfileUrl,
                        contentDescription = "${message.userName} 프로필",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onUserProfileClick),
                    )
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column {
                    if (isFirstInGroup) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = message.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 시간과 상태를 이름 옆에 표시
                            MessageStatusRow(
                                message = message,
                                onRetryMessage = onRetryMessage,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val displayMessage = message.message
                    val processedText = parseMentionsForDisplay(
                        displayMessage,
                        participants,
                        projectMembers,
                        projectRoles
                    )

                    // 이미지가 있는 경우 이미지 컴포넌트 사용, 없으면 텍스트만 표시
                    if (message.hasImages) {
                        ImageMessageComponent(
                            message = message,
                            onImageClick = onImageClick
                        )
                    } else {
                        ChatMessageText(
                            processedText = processedText,
                            onMentionClick = onMentionClick
                        )
                    }

                    // 수정 표시 (메시지 내용 아래에 표시)
                    if (message.isModified) {
                        Text(
                            text = "(수정됨)",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // 첨부파일 렌더링 (새로운 payload 기반 시스템)
                    val attachments = remember(message.payload) {
                        try {
                            val messagePayload = MessagePayload(message.payload)
                            val attachmentList = messagePayload.getAttachments()
                            if (attachmentList.isNotEmpty()) {
                                android.util.Log.d(
                                    "ChatMessageItem",
                                    "🖼️ [UI표시] 메시지 아이템에서 첨부파일 렌더링: ${attachmentList.size}개"
                                )
                            }
                            attachmentList
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    if (attachments.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            attachments.forEach { attachment ->
                                AttachmentRenderer(
                                    attachment = attachment,
                                    modifier = Modifier.fillMaxWidth(),
                                    onAttachmentClick = { clickedAttachment ->
                                        // TODO: 첨부파일 클릭 처리 (이미지 확대, 파일 다운로드 등)
                                        val url = clickedAttachment["url"] as? String ?: ""
                                        android.util.Log.d("ChatScreen", "Attachment clicked: $url")
                                    }
                                )
                            }
                        }
                    } else if (message.attachmentImageUrls.isNotEmpty()) {
                        // 폴백: 기존 방식으로 이미지 표시
                        FlowRow(modifier = Modifier.padding(top = 4.dp), maxItemsInEachRow = 3) {
                            message.attachmentImageUrls.forEach { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "첨부 이미지",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(2.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        MessageType.SYSTEM_DATE -> {
            DateSystemMessage(
                payload = message.payload,
                modifier = modifier.padding(vertical = 4.dp)
            )
        }

        MessageType.SYSTEM_CHAT_START -> {
            ChatStartSystemMessage(
                payload = message.payload,
                modifier = modifier.padding(vertical = 8.dp)
            )
        }

        MessageType.SYSTEM_PROJECT_JOIN -> {
            ProjectJoinSystemMessage(
                payload = message.payload,
                onJoinProject = onJoinProject,
                modifier = modifier.padding(vertical = 6.dp)
            )
        }

        MessageType.SYSTEM_MEMBER_INVITATION -> {
            MemberInvitationSystemMessage(
                payload = message.payload,
                onAddMember = onAddMember,
                modifier = modifier.padding(vertical = 6.dp)
            )
        }

        MessageType.IMAGE -> {
            // 이미지 메시지 처리 - 기본적으로 TEXT와 동일하게 처리
            // TODO: 추후 이미지 전용 UI 구현 시 업데이트
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .combinedClickable(
                        onClick = { /* 일반 클릭은 Bubble 자체에는 불필요할 수 있음 */ },
                        onLongClick = onLongClick
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFirstInGroup) {
                    SimpleUserProfileImage(
                        imageUrl = message.userProfileUrl,
                        contentDescription = "${message.userName} 프로필",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onUserProfileClick),
                    )
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column {
                    if (isFirstInGroup) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = message.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            MessageStatusRow(
                                message = message,
                                onRetryMessage = onRetryMessage,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val displayMessage = message.message
                    val processedText = parseMentionsForDisplay(
                        displayMessage,
                        participants,
                        projectMembers,
                        projectRoles
                    )

                    // 이미지 메시지의 경우 이미지 우선 표시
                    if (message.hasImages) {
                        ImageMessageComponent(
                            message = message,
                            onImageClick = onImageClick
                        )
                    } else {
                        ChatMessageText(
                            processedText = processedText,
                            onMentionClick = onMentionClick
                        )
                    }

                    // 수정 표시 (메시지 내용 아래에 표시)
                    if (message.isModified) {
                        Text(
                            text = "(수정됨)",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        MessageType.SYSTEM -> {
            // 일반 시스템 메시지
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        MessageType.SYSTEM_PROJECT_LEAVE -> {
            // 프로젝트 떠나기 시스템 메시지
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        MessageType.SYSTEM_USER_INVITE -> {
            // 사용자 초대 시스템 메시지
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

    }
}

// MentionSuggestionsPopup은 MentionSuggestions.kt로 이동됨

// MentionSuggestionItem은 MentionSuggestions.kt로 이동됨

// 선택된 이미지 미리보기 아이템
@Composable
fun SelectedImagePreview(
    uri: android.net.Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(64.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = "선택된 이미지",
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                .padding(2.dp) // 아이콘 패딩
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "선택 해제",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ImageSelectionGrid는 ChatScreen.kt로 이동됨


// ImageSelectItem은 ChatScreen.kt로 이동됨
