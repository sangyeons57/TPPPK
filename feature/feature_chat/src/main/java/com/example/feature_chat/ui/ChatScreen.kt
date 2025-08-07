package com.example.feature_chat.ui

// R import might be removed if UserProfileImage handles it all and no other direct R.drawable is used.
// For now, assume it might still be needed for fallbacks in UserProfileImage or other icons.
// ViewModel 및 관련 모델 Import
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.ui.components.common.ConnectionStatusBar
import com.example.feature_chat.ui.components.dialog.EditDeleteChatDialog
import com.example.feature_chat.ui.components.dialog.UserProfileDialog
import com.example.feature_chat.ui.components.input.MessageInput
import com.example.feature_chat.ui.components.system.ChatMessagesList
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
                is ChatEvent.RefreshMessages -> {
                    lazyPagingItems.refresh()
                }
            }
        }
    }

    // Auto-scroll to bottom on initial data load - enhanced timing with anchor coordination
    // 초기 자동 스크롤 억제: initialMessageId가 있으면 최신으로 스크롤하지 않음
    LaunchedEffect(
        lazyPagingItems.itemCount,
        lazyPagingItems.loadState.refresh,
        uiState.isAnchorJumpInProgress,
        viewModel.getInitialMessageId()
    ) {
        val hasAnchor = viewModel.getInitialMessageId() != null
        if (!hasScrolledToInitialPosition &&
            !hasAnchor && // 앵커가 없을 때만 최신으로 자동 스크롤
            lazyPagingItems.itemCount > 0 &&
            lazyPagingItems.loadState.refresh is androidx.paging.LoadState.NotLoading &&
            lazyPagingItems.loadState.append is androidx.paging.LoadState.NotLoading
        ) {
            if (uiState.isAnchorJumpInProgress) return@LaunchedEffect
            delay(100)
            listState.scrollToItem(0)
            hasScrolledToInitialPosition = true
            Log.d("ChatScreen", "✅ 초기 스크롤(최신) 완료 - 아이템 수: ${lazyPagingItems.itemCount}")
        }
    }

    // 추가: 초기 데이터 로딩 상태 모니터링
    LaunchedEffect(lazyPagingItems.loadState.refresh) {
        when (val loadState = lazyPagingItems.loadState.refresh) {
            is androidx.paging.LoadState.Loading -> {
                Log.d("ChatScreen", "🔄 초기 데이터 로딩 중...")
            }

            is androidx.paging.LoadState.NotLoading -> {
                Log.d("ChatScreen", "✅ 초기 데이터 로딩 완료 - 아이템 수: ${lazyPagingItems.itemCount}")
            }

            is androidx.paging.LoadState.Error -> {
                Log.e("ChatScreen", "❌ 초기 데이터 로딩 실패: ${loadState.error.message}")
            }
        }
    }

    // 앵커 대상 메시지가 로드되면 해당 인덱스로 스크롤 (initialMessageId 케이스 포함)
    LaunchedEffect(
        viewModel.anchorTargetMessageId.value,
        uiState.isAnchorJumpInProgress,
        lazyPagingItems.itemCount,
        lazyPagingItems.loadState.refresh,
        lazyPagingItems.loadState.append
    ) {
        val targetId = viewModel.anchorTargetMessageId.value
        if (targetId.isNullOrBlank()) return@LaunchedEffect
        if (uiState.isAnchorJumpInProgress) return@LaunchedEffect
        if (lazyPagingItems.itemCount <= 0) return@LaunchedEffect

        // 현재 스냅샷에서 타겟 메시지의 인덱스를 찾음
        val snapshot = lazyPagingItems.itemSnapshotList.items
        val anchorIndex = snapshot.indexOfFirst { it.messageId == targetId }
        if (anchorIndex >= 0) {
            // reverseLayout = true 이므로, index가 0이면 최신
            listState.scrollToItem(anchorIndex)
            hasScrolledToInitialPosition = true
            Log.d("ChatScreen", "🎯 앵커 스크롤 완료: targetId=$targetId, index=$anchorIndex")
        } else {
            // 아직 스냅샷에 없으면 추후 append/refresh 시 다시 시도됨
            Log.d("ChatScreen", "⏳ 앵커 대상 미존재 - 다음 로딩 때 재시도: $targetId")
        }
    }

    // 추가: isLoadingHistory 상태 모니터링
    LaunchedEffect(uiState.isLoadingHistory) {
        if (uiState.isLoadingHistory) {
            Log.d("ChatScreen", "📋 초기 채팅 아이템 로딩 시작")
        } else {
            Log.d("ChatScreen", "✅ 초기 채팅 아이템 로딩 완료")
        }
    }

    // 🎯 Anchor Jump 완료 시 스크롤 최적화
    LaunchedEffect(uiState.isAnchorJumpInProgress) {
        // Anchor Jump가 완료되면 UI 스크롤 타이밍 최적화
        if (!uiState.isAnchorJumpInProgress && hasScrolledToInitialPosition) {
            // Anchor 완료 후 약간의 지연으로 안정적인 스크롤
            delay(150)
            Log.d("ChatScreen", "🎯 Anchor Jump 완료 - UI 스크롤 최적화 완료")
        }
    }

    // 키보드 상태 변경 시 스크롤 위치 조정
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && lazyPagingItems.itemCount > 0) {
            // Anchor Jump 중이 아닐 때만 키보드 스크롤 수행
            if (!uiState.isAnchorJumpInProgress) {
                // 키보드가 나타날 때 최신 메시지로 스크롤
                delay(100) // 키보드 애니메이션 대기
                listState.animateScrollToItem(0)
                Log.d("ChatScreen", "⌨️ 키보드 표시로 스크롤 조정")
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

            // 개발 빌드에서만 노출되는 Anchor Jump 테스트 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = {
                        // 🎯 Anchor-aware 최신 스크롤 로직
                        if (!uiState.isAnchorJumpInProgress) {
                            coroutineScope.launch {
                                // UI에서 직접 최신 메시지로 스크롤
                                if (lazyPagingItems.itemCount > 0) {
                                    listState.animateScrollToItem(0)
                                    Log.d("ChatScreen", "📍 수동 최신 스크롤 실행")
                                }
                            }
                            // ViewModel의 jumpToLatest도 함께 호출하여 데이터 새로고침
                            viewModel.jumpToLatest()
                        } else {
                            Log.d("ChatScreen", "🚫 Anchor Jump 진행 중 - 최신 스크롤 스킵")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isAnchorJumpInProgress // Anchor Jump 중 비활성화
                ) {
                    Text(
                        "최신으로",
                        fontSize = 10.sp,
                        color = if (uiState.isAnchorJumpInProgress)
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.6f
                            )
                        else
                            MaterialTheme.colorScheme.primary
                    )
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

            when {
                uiState.error != null && uiState.error?.contains("WebSocket 구현 예정") == true -> {
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
                }

                uiState.isLoadingHistory -> {
                    // 초기 채팅 아이템 로딩 중일 때 표시할 UI
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "채팅 메시지를 불러오는 중...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "잠시만 기다려주세요",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                else -> {
                    // ChatMessagesList는 components/ChatMessagesList.kt로 분리됨
                    // 여기서는 import된 컴포넌트를 사용
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
    }

    showEditDeleteDialog?.let { message ->
        EditDeleteChatDialog(
            message = message, 
            isMyMessage = message.isMyMessage, 
            onDismiss = { showEditDeleteDialog = null },
            onEdit = {
                viewModel.startEditMessage(message) 
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

// --- Preview용 Content Composable ---
// Preview는 실제 ViewModel과 상호작용하지 않으므로,
// 가짜 데이터(Fake Data)를 사용하여 UI 모양만 확인합니다.
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
