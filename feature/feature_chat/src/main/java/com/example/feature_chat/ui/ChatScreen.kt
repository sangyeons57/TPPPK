package com.example.feature_chat.ui

// R import might be removed if UserProfileImage handles it all and no other direct R.drawable is used.
// For now, assume it might still be needed for fallbacks in UserProfileImage or other icons.
// ViewModel 및 관련 모델 Import
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.user.SimpleUserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.MentionType
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatParticipant
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.GalleryImageUiModel
import com.example.feature_chat.model.MentionSuggestion
import com.example.feature_chat.model.ProjectMember
import com.example.feature_chat.model.ProjectRole
import com.example.feature_chat.ui.components.ConnectionStatusBar
import com.example.feature_chat.ui.components.MentionStyledInputField
import com.example.feature_chat.ui.components.MessageInput
import com.example.feature_chat.viewmodel.WebSocketChatViewModel
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
private data class ParsedMention(
    val type: String,
    val id: String,
    val displayName: String,
    val start: Int,
    val end: Int
)

/**
 * Data class to hold processed text with mentions
 */
private data class ProcessedText(
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
                    text = uiState.messageInput,
                    isEditing = uiState.isEditing,
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
                    onRetryMessage = viewModel::retryMessage
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
    onRetryMessage: (String) -> Unit = { _ -> }
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .testTag("message_list"),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp) // 입력창과의 간격 추가
    ) {
        // Paging3 loading states
        when (val loadState = lazyPagingItems.loadState.refresh) {
            is androidx.paging.LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
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
                Log.d("ChatMessageItemComposable", "isFirstInGroup: $isFirstInGroup")
                ChatMessageItemComposable(
                    message = messageWithStatus,
                    isFirstInGroup = isFirstInGroup,
                    onLongClick = { onMessageLongClick(it) },
                    onUserProfileClick = { onUserProfileClick(it.userId) },
                    onRetryMessage = onRetryMessage,
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
    participants: List<ChatParticipant> = emptyList(),
    projectMembers: List<ProjectMember> = emptyList(),
    projectRoles: List<ProjectRole> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
        Log.d("ChatMessageItemComposable", "isFirstInGroup: $isFirstInGroup")
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
            val annotatedString = buildAnnotatedString {
                val processedText = parseMentionsForDisplay(
                    displayMessage,
                    participants,
                    projectMembers,
                    projectRoles
                )
                append(processedText.text)
                
                // Handle mention styling with enhanced visibility
                processedText.mentions.forEach { mention ->
                    addStringAnnotation("MENTION", "${mention.type}:${mention.id}", mention.start, mention.end)
                    
                    // Apply different styles for user and role mentions
                    val mentionStyle = when (mention.type) {
                        "user" -> SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                        "role" -> SpanStyle(
                            color = MaterialTheme.colorScheme.tertiary,
                            background = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                        else -> SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    addStyle(
                        style = mentionStyle,
                        start = mention.start,
                        end = mention.end
                    )
                }
                
                // Handle URL styling
                val urlRegex = """(https?://\S+)""".toRegex()
                urlRegex.findAll(processedText.text).forEach { matchResult ->
                    val url = matchResult.value
                    val startIndex = matchResult.range.first
                    val endIndex = matchResult.range.last + 1
                    addStringAnnotation("URL", url, startIndex, endIndex)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.tertiary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = startIndex,
                        end = endIndex
                    )
                }
            }

            ClickableText(
                text = annotatedString,
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                onClick = { position ->
                    // Handle mention clicks
                    annotatedString.getStringAnnotations("MENTION", position, position)
                        .firstOrNull()?.let { annotation ->
                            val parts = annotation.item.split(":")
                            if (parts.size == 2) {
                                val type = parts[0]
                                val id = parts[1]
                                onMentionClick(type, id)
                            }
                        }
                    
                    // Handle URL clicks
                    annotatedString.getStringAnnotations("URL", position, position)
                        .firstOrNull()?.let { annotation ->
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle error opening URL
                            }
                        }
                }
            )

            if (message.attachmentImageUrls.isNotEmpty()) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.isModified) {
                    Text(
                        text = "(수정됨)",
                        fontSize = 10.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                if(message.isMyMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when {
                            message.isSending -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .testTag("delivery_indicator"),
                                    strokeWidth = 1.dp
                                )
                            }

                            message.sendFailed -> {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = "전송 실패",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .testTag("delivery_indicator")
                                )
                                if (message.canRetry) {
                                    IconButton(
                                        onClick = { onRetryMessage(message.messageId) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "재전송",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            message.deliveryState is com.example.feature_chat.model.MessageDeliveryState.Retry -> {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "재전송 대기",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .testTag("delivery_indicator")
                                )
                            }

                            else -> {
                                // 성공적으로 전송된 경우 시간만 표시 (기존 로직 유지)
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInputArea(
    modifier: Modifier = Modifier,
    uiState: ChatUiState,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachmentClick: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onImageDeselected: (Uri) -> Unit,
    onCancelEdit: () -> Unit,
    onPickImages: () -> Unit,
    onMentionSuggestionClick: (MentionSuggestion) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // TextField가 눌렸을 때 키보드를 올리기 위한 로직
    if (interactionSource.collectIsPressedAsState().value) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = uiState.isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("edit_mode_input"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "메시지 수정 중...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "수정 취소")
                }
            }
        }

        AnimatedVisibility(visible = uiState.selectedAttachmentUris.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.selectedAttachmentUris.forEach { uri ->
                    SelectedImagePreview(uri = uri, onRemove = { onImageDeselected(uri) })
                }
            }
        }

        AnimatedVisibility(visible = uiState.isAttachmentAreaVisible) {
            ImageSelectionGrid(
                images = uiState.galleryImages, 
                selectedImages = uiState.selectedAttachmentUris.toSet(),
                onImageSelected = onImageSelected,
                onImageDeselected = onImageDeselected,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            )
        }

        // Mention suggestions popup - moved above input field
        AnimatedVisibility(visible = uiState.isMentionSuggestionVisible) {
            MentionSuggestionsPopup(
                suggestions = uiState.mentionSuggestions,
                onSuggestionClick = onMentionSuggestionClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp), // 높이 조절
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPickImages) { 
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "이미지 첨부")
                }
                Spacer(modifier = Modifier.width(8.dp))
                MentionStyledInputField(
                    value = uiState.pendingMessageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 100.dp) // 최대 높이 줄임
                        .focusRequester(focusRequester)
                        .testTag("message_input_field"),
                    interactionSource = interactionSource,
                    placeholder = "메시지 입력...",
                    maxLines = 3, // 줄 수 줄임
                    participants = uiState.participants,
                    projectMembers = uiState.projectMembers,
                    projectRoles = uiState.projectRoles
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendMessage,
                    enabled = !uiState.isSendingMessage && (uiState.pendingMessageText.isNotBlank() || uiState.selectedAttachmentUris.isNotEmpty()),
                    modifier = Modifier.testTag("send_button")
                ) {
                    if (uiState.isSendingMessage) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        val icon = if(uiState.isEditing) Icons.Filled.Check else Icons.AutoMirrored.Filled.Send
                        Icon(icon, contentDescription = if(uiState.isEditing) "수정 완료" else "전송")
                    }
                }
            }
        }
    }
}

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
            ChatInputArea(
                uiState = uiState,
                onMessageChange = {},
                onSendMessage = {},
                onAttachmentClick = {},
                onImageSelected = {},
                onImageDeselected = {},
                onCancelEdit = {},
                onPickImages = {}
            )
        }
    ) { paddingValues ->
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
