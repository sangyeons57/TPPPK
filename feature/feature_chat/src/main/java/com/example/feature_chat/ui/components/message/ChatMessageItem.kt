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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onCheckMembership: (String) -> Unit = { _ -> }, // 프로젝트 멤버십 확인 콜백 추가
    projectMembershipStatesFlow: StateFlow<Map<String, Boolean>> = kotlinx.coroutines.flow.MutableStateFlow(
        emptyMap()
    ), // 프로젝트 멤버십 상태 StateFlow
    onAddMember: (String, String) -> Unit = { _, _ -> }, // 멤버 추가 콜백 추가 (projectId, targetUserId)
    onImageClick: (String, List<String>, Int) -> Unit = { _, _, _ -> }, // 이미지 클릭 콜백 추가
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList(),
    modifier: Modifier = Modifier
) {
    LocalContext.current

    // 삭제된 메시지는 SQL 쿼리 단계에서 이미 필터링됨

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
                        android.util.Log.d(
                            "ChatMessageItem",
                            "🖼️ [UI렌더링] hasImages=true, ImageMessageComponent 호출"
                        )
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

                    // 수정 표시는 이제 MessageStatusRow(시간 옆)에서 처리됨

                    // 첨부파일 렌더링 (이미지가 아닌 파일만 - 이미지는 ImageMessageComponent에서 처리)
                    val nonImageAttachments = remember(message.payload) {
                        try {
                            val messagePayload = MessagePayload(message.payload)
                            val attachmentList = messagePayload.getAttachments()
                            // 이미지가 아닌 첨부파일만 필터링 (이중 렌더링 방지)
                            val filteredAttachments = attachmentList.filter { attachment ->
                                val kind = attachment[MessagePayload.KEY_KIND] as? String
                                kind != "image" // 이미지가 아닌 것만
                            }
                            if (filteredAttachments.isNotEmpty()) {
                                android.util.Log.d(
                                    "ChatMessageItem",
                                    "🖼️ [UI표시] 메시지 아이템에서 비이미지 첨부파일 렌더링: ${filteredAttachments.size}개"
                                )
                            }
                            filteredAttachments
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    if (nonImageAttachments.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            nonImageAttachments.forEach { attachment ->
                                AttachmentRenderer(
                                    attachment = attachment,
                                    modifier = Modifier.fillMaxWidth(),
                                    onAttachmentClick = { clickedAttachment ->
                                        // 파일 다운로드 또는 열기 처리
                                        val url = clickedAttachment["url"] as? String ?: ""
                                        android.util.Log.d(
                                            "ChatScreen",
                                            "Non-image attachment clicked: $url"
                                        )
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

        MessageType.PROJECT_INVITE -> {
            ProjectInviteMessage(
                payload = MessagePayload(message.payload),
                onJoinProject = onJoinProject,
                isSender = message.isMyMessage,
                onCheckMembership = onCheckMembership,
                projectMembershipStatesFlow = projectMembershipStatesFlow,
                modifier = modifier.padding(vertical = 6.dp)
            )
        }

        MessageType.SYSTEM_MEMBER_INVITATION -> {
            MemberInvitationSystemMessage(
                payload = message.payload,
                onAddMember = onAddMember,
                modifier = modifier,
                isSender = message.isMyMessage,
                // 프로필 정보 전달
                senderName = message.userName,
                senderProfileUrl = message.userProfileUrl,
                timestamp = message.formattedTimestamp,
                isFirstInGroup = isFirstInGroup,
                onUserProfileClick = onUserProfileClick
            )
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

/**
 * 프로젝트 초대 메시지 렌더링
 */
@Composable
private fun ProjectInviteMessage(
    payload: MessagePayload,
    onJoinProject: (String) -> Unit = {},
    isSender: Boolean = false,
    onCheckMembership: (String) -> Unit = { _ -> },
    projectMembershipStatesFlow: StateFlow<Map<String, Boolean>>,
    modifier: Modifier = Modifier
) {
    // Parse payload outside composable to avoid try-catch around composable calls
    val payloadData = remember(payload) {
        try {
            val payloadJson = payload.asJsonObject()
            mapOf(
                "projectName" to (payloadJson["projectName"]?.toString()?.removeSurrounding("\"")
                    ?: "프로젝트"),
                "inviterName" to (payloadJson["inviterName"]?.toString()?.removeSurrounding("\"")
                    ?: "알 수 없음"),
                "actionText" to (payloadJson["actionText"]?.toString()?.removeSurrounding("\"")
                    ?: "참여하기"),
                "projectId" to (payloadJson["projectId"]?.toString()?.removeSurrounding("\"")
                    ?: ""),
                "isValid" to true
            )
        } catch (e: Exception) {
            mapOf("isValid" to false)
        }
    }

    if (payloadData["isValid"] == true) {
        val projectId = payloadData["projectId"] as String

        // 멤버십 상태 확인 (LaunchedEffect로 초기화 시 한 번만 확인)
        LaunchedEffect(projectId) {
            onCheckMembership(projectId)
        }

        // StateFlow에서 현재 멤버십 상태 구독
        val projectMembershipStates by projectMembershipStatesFlow.collectAsStateWithLifecycle()
        val isAlreadyJoined = projectMembershipStates[projectId] ?: false
        
        ProjectInviteMessageComponent(
            projectName = payloadData["projectName"] as String,
            inviterName = payloadData["inviterName"] as String,
            actionText = payloadData["actionText"] as String,
            isSender = isSender,
            isAlreadyJoined = isAlreadyJoined,
            onJoinProject = { onJoinProject(projectId) },
            modifier = modifier
        )
    } else {
        // Fallback UI for parsing errors
        Text(
            text = "프로젝트 초대 메시지",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

// ImageSelectionGrid는 ChatScreen.kt로 이동됨


// ImageSelectItem은 ChatScreen.kt로 이동됨
