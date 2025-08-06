package com.example.feature_chat.ui

// R import might be removed if UserProfileImage handles it all and no other direct R.drawable is used.
// For now, assume it might still be needed for fallbacks in UserProfileImage or other icons.
// ViewModel 및 관련 모델 Import
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.components.attachment.AttachmentRenderer
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.user.SimpleUserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.vo.MentionType
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.GalleryImageUiModel
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole
import com.example.feature_chat.ui.components.ChatStartSystemMessage
import com.example.feature_chat.ui.components.ConnectionStatusBar
import com.example.feature_chat.ui.components.DateSystemMessage
import com.example.feature_chat.ui.components.DefaultSystemMessage
import com.example.feature_chat.ui.components.MessageInput
import com.example.feature_chat.ui.components.MessageStatusRow
import com.example.feature_chat.ui.components.ProjectJoinSystemMessage
import com.example.feature_chat.viewmodel.WebSocketChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * 메시지의 효과적인 타임스탬프를 반환 (임시 메시지는 clientSentAt, 실제 메시지는 actualTimestamp)
 */
private fun getEffectiveTimestamp(message: ChatMessageUiModel): Instant {
    return when {
        message.isOptimistic && message.clientSentAt != null -> message.clientSentAt
        else -> message.actualTimestamp
    }
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
                    val userName = participants.find { participant -> participant.userId == id }?.displayName
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
private fun parseMentionsForDisplay(
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
                val userName = participants.find { participant -> participant.userId == mentionId }?.displayName
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

/**
 * ChatScreen: 채팅 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebSocketChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.messagesFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var hasScrolledToInitialPosition by remember { mutableStateOf(false) }

    // 키보드 상태 관리
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var showEditDeleteDialog by remember { mutableStateOf<ChatMessageUiModel?>(null) } // ★ 타입 변경
    var showUserProfileDialog by remember { mutableStateOf<String?>(null) }

    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> -> viewModel.onImagesSelected(uris) }
    )

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ChatEvent.ScrollToBottom -> coroutineScope.launch {
                    if(listState.layoutInfo.totalItemsCount > 0) {
                        listState.animateScrollToItem(0)
                    }
                }
                is ChatEvent.ShowEditDeleteDialog -> showEditDeleteDialog = event.message // ★ 타입 변경됨
                is ChatEvent.ShowUserProfileDialog -> showUserProfileDialog = event.userId
                is ChatEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ChatEvent.ClearFocus -> focusManager.clearFocus()
                is ChatEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is ChatEvent.ShowMessageActions -> {
                    // Note: With Paging3, we can't easily find messages in UI state
                    // Consider refactoring to pass the message directly with the event
                    // For now, skip this functionality until event system is updated
                }
                is ChatEvent.ImagesSelected -> {}
                is ChatEvent.AttachmentClicked -> {}
                is ChatEvent.ImageSelected -> {}
                is ChatEvent.ImageDeselected -> {}
                is ChatEvent.SystemMessage -> snackbarHostState.showSnackbar(event.content)
            }
        }
    }

    // Auto-scroll to bottom on initial data load without animation - only once
    LaunchedEffect(lazyPagingItems.itemCount, hasScrolledToInitialPosition) {
        // Only scroll when we first get items and haven't scrolled yet
        if (!hasScrolledToInitialPosition &&
            lazyPagingItems.itemCount > 0 &&
            lazyPagingItems.loadState.refresh is androidx.paging.LoadState.NotLoading
        ) {
            // Jump to bottom (index 0 in reverse layout) instantly without animation
            listState.scrollToItem(0)
            hasScrolledToInitialPosition = true
        }
    }

    // 키보드 상태 변경 시 스크롤 위치 조정
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && lazyPagingItems.itemCount > 0) {
            // 키보드가 나타날 때 최신 메시지로 스크롤
            delay(100) // 키보드 애니메이션 대기
            listState.animateScrollToItem(0)
        }
    }

    // Note: With Paging3, auto-scroll on new messages should be handled differently
    // Consider using LaunchedEffect with item count or specific events
    // Removed dependency on uiState.messages since it's now handled by Paging3

    // Paging3 handles automatic loading - no manual pagination needed

    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .testTag("chat_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.channelName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        },
        bottomBar = {
            if (uiState.error == null || uiState.error?.contains("WebSocket 구현 예정") == false) {
                MessageInput(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                    text = uiState.pendingMessageText,
                    isEditing = uiState.isEditing,
                    isEnabled = viewModel.canPerformWriteOperations(),
                    onTextChange = viewModel::onMessageInputChange,
                    onSendClick = {
                        if (uiState.isEditing) {
                            viewModel.confirmEditMessage()
                        } else {
                            viewModel.onSendMessageClick()
                        }
                    },
                    onAttachmentClick = viewModel::onAttachmentClick,
                    onCancelEdit = viewModel::cancelEdit,
                    onKeyboardStateChange = { keyboardVisible ->
                        isKeyboardVisible = keyboardVisible
                    },
                    onScrollToBottom = {
                        coroutineScope.launch {
                            if (lazyPagingItems.itemCount > 0) {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
                    onMentionSuggestionClick = viewModel::onMentionSuggestionClick,
                    participants = uiState.participants,
                    projectMembers = uiState.projectMembers,
                    projectRoles = uiState.projectRoles,
                    mentionSuggestions = uiState.mentionSuggestions,
                    isMentionSuggestionVisible = uiState.isMentionSuggestionVisible
                )
            } else {
                Log.d("ChatScreen", "Chat input area hidden as chat is pending WebSocket implementation.")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ConnectionStatusBar(
                connectionState = uiState.connectionState,
                queuedMessagesCount = uiState.queuedMessagesCount,
                onRetryConnection = { viewModel.retryConnection() }
            )

            // 테스트용 Anchor Jump 버튼들 (개발 완료 후 제거 가능)
            // TODO: 실제 배포 시에는 이 버튼들을 제거하거나 조건부로 숨김
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = { viewModel.jumpToLatest() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("최신으로", fontSize = 10.sp)
                }

                TextButton(
                    onClick = {
                        // 테스트용: 첫 번째 메시지 ID로 점프 (실제로는 검색이나 알림에서 호출)
                        if (lazyPagingItems.itemCount > 0) {
                            val firstMessage = lazyPagingItems[0]
                            firstMessage?.let { viewModel.jumpToMessage(it.messageId) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("첫 메시지로", fontSize = 10.sp)
                }
            }
            
            if (uiState.error != null && uiState.error?.contains("WebSocket 구현 예정") == true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "채팅 기능을 현재 사용할 수 없습니다.\n(WebSocket 구현 예정)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ChatMessagesList(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    lazyPagingItems = lazyPagingItems,
                    listState = listState,
                    onMessageLongClick = viewModel::onMessageLongClick,
                    onUserProfileClick = viewModel::onUserProfileClick,
                    onRetryMessage = viewModel::retryMessage,
                    initialMessageId = viewModel.getInitialMessageId()
                )
            }
        }
    }

    showEditDeleteDialog?.let { message ->
        EditDeleteChatDialog(
            message = message, 
            isMyMessage = message.isMyMessage, 
            onDismiss = { showEditDeleteDialog = null },
            onEdit = {
                viewModel.startEditMessage(message.messageId, message.message) 
                showEditDeleteDialog = null
            },
            onDelete = {
                viewModel.confirmDeleteMessage(message.messageId) 
                showEditDeleteDialog = null
            }
        )
    }

    showUserProfileDialog?.let { userId ->
        UserProfileDialog(userId = userId, onDismiss = { showUserProfileDialog = null })
    }
}

@Composable
fun UserProfileDialog(userId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사용자 프로필") },
        text = { Text("사용자 ID: $userId\n(상세 정보 표시는 미구현)") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

@Composable
fun ChatMessagesList(
    modifier: Modifier = Modifier,
    uiState: ChatUiState,
    lazyPagingItems: LazyPagingItems<ChatMessageUiModel>,
    listState: LazyListState,
    onMessageLongClick: (ChatMessageUiModel) -> Unit,
    onUserProfileClick: (String) -> Unit,
    onRetryMessage: (String) -> Unit = { _ -> },
    initialMessageId: String? = null
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .testTag("message_list"),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp), // 입력창과의 간격 추가
        userScrollEnabled = initialMessageId == null // 특정 메시지로 이동한 경우에만 스크롤 비활성화 (null이면 스크롤 가능, 설정되면 스크롤 비활성화)
    ) {
        // Paging3 loading states
        when (val loadState = lazyPagingItems.loadState.refresh) {
            is androidx.paging.LoadState.Loading -> {
                // Show skeleton UI instead of simple progress indicator
                items(5) { // Show 5 skeleton items
                    MessageSkeletonItem()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            is androidx.paging.LoadState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp), contentAlignment = Alignment.Center
                    ) {
                        Text("Loading error: ${loadState.error.localizedMessage}")
                    }
                }
            }

            else -> { /* Loading complete */
            }
        }

        // Show loading indicator for append (load more)
        when (lazyPagingItems.loadState.append) {
            is androidx.paging.LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            else -> { /* No loading */
            }
        }

        items(
            count = lazyPagingItems.itemCount,
            key = { index ->
                val message = lazyPagingItems[index]
                // 임시 메시지는 localId로, 실제 메시지는 chatId로 키 생성
                if (message?.isOptimistic == true) {
                    "temp_${message.messageId}"
                } else {
                    "actual_${message?.messageId}"
                }
            }
        ) { index ->
            val message = lazyPagingItems[index]
            message?.let {
                // Note: With Paging3, grouping logic needs to be handled differently
                // For now, treat each message as first in group until grouping is reimplemented
                val isFirstInGroup = true

                // 메시지 전송 상태에 따른 특별 처리
                val messageWithStatus = when {
                    it.isSending -> it.copy(formattedTimestamp = "전송 중...")
                    it.sendFailed -> it.copy(formattedTimestamp = "전송 실패")
                    else -> it
                }
                ChatMessageItemComposable(
                    message = messageWithStatus,
                    isFirstInGroup = isFirstInGroup,
                    onLongClick = { onMessageLongClick(it) },
                    onUserProfileClick = { onUserProfileClick(it.userId) },
                    onRetryMessage = onRetryMessage,
                    onJoinProject = { projectId ->
                        // 프로젝트 참여 로직 - 추후 ViewModel에서 처리
                        Log.d("ChatScreen", "프로젝트 참여 요청: $projectId")
                        // TODO: 프로젝트 참여 이벤트 처리
                    },
                    onMentionClick = { type, id ->
                        when (type) {
                            "user" -> onUserProfileClick(id)
                            "role" -> {
                                // Handle role mention click - could show role members or role details
                                // For now, no action
                            }
                        }
                    },
                    participants = uiState.participants,
                    projectMembers = uiState.projectMembers,
                    projectRoles = uiState.projectRoles
                )

                Spacer(modifier = Modifier.height(if (isFirstInGroup) 16.dp else 0.dp))
            }
        }
    }
}

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.formattedTimestamp,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
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

                    ChatMessageText(
                        processedText = processedText,
                        onMentionClick = onMentionClick
                    )

                    // 첨부파일 렌더링 (새로운 payload 기반 시스템)
                    val attachments = remember(message.payload) {
                        try {
                            val messagePayload = MessagePayload(message.payload)
                            messagePayload.getAttachments()
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
                                        Log.d("ChatScreen", "Attachment clicked: $url")
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

                    MessageStatusRow(
                        message = message,
                        onRetryMessage = onRetryMessage
                    )
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

        else -> {
            // 알 수 없는 메시지 타입은 기본 시스템 메시지로 표시
            DefaultSystemMessage(
                message = message.message,
                modifier = modifier.padding(vertical = 4.dp)
            )
        }
    }
}


// ChatInputArea 컴포넌트는 MessageInput으로 대체되었습니다.

/**
 * Mention suggestions popup component
 */
@Composable
fun MentionSuggestionsPopup(
    suggestions: List<MentionSuggestion>,
    onSuggestionClick: (MentionSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .padding(vertical = 8.dp)
        ) {
            items(suggestions) { suggestion ->
                MentionSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

/**
 * Individual mention suggestion item
 */
@Composable
fun MentionSuggestionItem(
    suggestion: MentionSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile or role icon
        if (suggestion.type == MentionType.USER) {
            SimpleUserProfileImage(
                imageUrl = suggestion.profileUrl,
                contentDescription = "${suggestion.displayName} 프로필",
                modifier = Modifier.size(32.dp)
            )
        } else {
            // Role icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "@",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Name and subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (suggestion.subtitle != null) {
                Text(
                    text = suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Type indicator
        Text(
            text = if (suggestion.type == MentionType.USER) "사용자" else "역할",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// 선택된 이미지 미리보기 아이템
@Composable
fun SelectedImagePreview(
    uri: Uri,
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
            Icon(Icons.Default.Close, contentDescription = "선택 해제", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * ImageSelectionGrid: 갤러리 이미지 선택 UI (Stateless)
 */
@Composable
fun ImageSelectionGrid(
    modifier: Modifier = Modifier,
    images: List<GalleryImageUiModel>, // ★ 타입 변경
    selectedImages: Set<Uri>,
    onImageSelected: (Uri) -> Unit,
    onImageDeselected: (Uri) -> Unit
) {
    if (images.isEmpty()) {
        Box(modifier
            .fillMaxSize()
            .height(200.dp), contentAlignment = Alignment.Center) { // 높이 지정
            // TODO: 갤러리 로딩 상태 표시
            Text("갤러리 이미지를 불러오는 중이거나 이미지가 없습니다.")
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp), // 반응형 그리드
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .heightIn(max = 200.dp), // 최대 높이 지정
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = images, key = { it.id }) { image -> // ★ 타입 변경됨
            ImageSelectItem(
                imageUri = image.uri,
                isSelected = image.uri in selectedImages, // 선택 상태 확인
                onClick = {
                    if (image.uri in selectedImages) {
                        onImageDeselected(image.uri)
                    } else {
                        onImageSelected(image.uri)
                    }
                }
            )
        }
    }
}


/**
 * ImageSelectItem: 갤러리 이미지 아이템 (Stateless)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectItem(
    modifier: Modifier = Modifier,
    imageUri: Uri,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // 정사각형 유지
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = "갤러리 이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 선택 표시
        if (isSelected) {
            Box( // 반투명 오버레이
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
            Box( // 체크 아이콘 배경
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon( // 체크 아이콘
                    Icons.Default.Check,
                    contentDescription = "선택됨",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


/**
 * EditDeleteChatDialog: 메시지 수정/삭제 다이얼로그 (Stateless)
 */
@Composable
fun EditDeleteChatDialog(
    message: ChatMessageUiModel, // ★ 타입 변경
    isMyMessage: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메시지 옵션") },
        text = { Text("\"${message.message.take(30)}${if (message.message.length > 30) "..." else ""}\"", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        confirmButton = {
            if (isMyMessage) { // 내 메시지일 경우에만 수정/삭제 버튼 표시
                Row(horizontalArrangement = Arrangement.End, modifier=Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_message_option")
                    ) { Text("수정") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_message_option")
                    ) { Text("삭제") }
                }
            } else { // 다른 사람 메시지면 확인 버튼만 (또는 신고 버튼 등 추가 가능)
                TextButton(onClick = onDismiss) { Text("확인") }
            }
        },
        dismissButton = {
            if (isMyMessage) { // 내 메시지일 경우 취소 버튼
                TextButton(onClick = onDismiss) { Text("취소") }
            }
            // 다른 사람 메시지면 dismiss 버튼 불필요
        }
    )
}

// --- Preview용 Content Composable ---
// Preview는 실제 ViewModel과 상호작용하지 않으므로,
// 가짜 데이터(Fake Data)를 사용하여 UI 모양만 확인합니다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContentPreview(uiState: ChatUiState){
    rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.channelName) },
                navigationIcon = { DebouncedBackButton(onClick = {}) } // Preview, so no actual navigation
            )
        },
        bottomBar = {
            MessageInput(
                text = uiState.pendingMessageText,
                isEditing = uiState.isEditing,
                isEnabled = true,
                onTextChange = {},
                onSendClick = {},
                onAttachmentClick = {},
                onCancelEdit = {},
                onKeyboardStateChange = {},
                onScrollToBottom = {},
                onMentionSuggestionClick = {},
                participants = uiState.participants,
                projectMembers = uiState.projectMembers,
                projectRoles = uiState.projectRoles,
                mentionSuggestions = uiState.mentionSuggestions,
                isMentionSuggestionVisible = uiState.isMentionSuggestionVisible
            )
        }
    ) {
        // TODO: Create mock LazyPagingItems for preview
        // ChatMessagesList requires LazyPagingItems which can't be easily mocked in Preview
        Text("Preview not available with Paging3 - use real device/emulator")
    }
}

@Preview(showBackground = true, name="Chat Screen Preview")
@Composable
private fun ChatScreenFullPreview() {
    // 미리보기용 가짜 상태 데이터 생성 (Paging3에서는 messages 필드 제거됨)
    val previewUiState = ChatUiState(
        channelId = "preview_channel",
        channelName = "미리보기 채팅방",
        myUserId = "1"
        // Note: messages are now handled by Paging3 flow
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ChatContentPreview(uiState = previewUiState)
    }
}

@Preview(showBackground = true, name="Image Selection Grid Preview")
@Composable
private fun ImageSelectionGridPreview() {
    val images = List(10) {
        GalleryImageUiModel( // ★ 타입 변경
            "https://picsum.photos/id/$it/200".toUri(), // 가짜 이미지 URL 사용
            it.toString()
        )
    }
    val selected = remember { mutableStateOf(setOf<Uri>()) }
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ImageSelectionGrid(
            images = images,
            selectedImages = selected.value,
            onImageSelected = { selected.value = selected.value + it },
            onImageDeselected = { selected.value = selected.value - it }
        )
    }
}

@Composable
fun MessageSkeletonItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Profile picture skeleton
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        )

        Column {
            // Username and timestamp skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message content skeleton - varying lengths for more realistic look
            val messageWidths = listOf(200.dp, 150.dp, 180.dp)
            val randomWidth = messageWidths.random()

            Box(
                modifier = Modifier
                    .width(randomWidth)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
