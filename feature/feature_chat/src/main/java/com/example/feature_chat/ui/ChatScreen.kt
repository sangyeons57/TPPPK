package com.example.feature_chat.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi // combinedClickable 사용
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable // 롱클릭 사용
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // LazyVerticalGrid items
import androidx.compose.foundation.lazy.items // LazyColumn items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate // 이미지 첨부 아이콘
import androidx.compose.material.icons.filled.Check // 체크 아이콘 추가
import androidx.compose.material.icons.filled.Close // 수정 취소, 제거 아이콘
import androidx.compose.material.icons.filled.ErrorOutline // 전송 실패 아이콘 (예시)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage // Coil 라이브러리 사용 for attachments
import coil.request.ImageRequest // Used by AsyncImage for attachments
import com.example.core_ui.components.user.SimpleUserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
// R import might be removed if UserProfileImage handles it all and no other direct R.drawable is used.
// For now, assume it might still be needed for fallbacks in UserProfileImage or other icons.
import com.example.core_ui.R
import com.example.feature_chat.ui.components.ConnectionStatusBar
// ViewModel 및 관련 모델 Import
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.GalleryImageUiModel
import com.example.feature_chat.viewmodel.WebSocketChatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.core_navigation.core.NavigationManger
import java.util.Locale
import android.util.Log // Added for logging
import com.example.core_ui.components.buttons.DebouncedBackButton
import java.time.Instant
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

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
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showEditDeleteDialog by remember { mutableStateOf<ChatMessageUiModel?>(null) } // ★ 타입 변경
    var showUserProfileDialog by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
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
                    val message = uiState.messages.find { it.chatId == event.messageId }
                    message?.let { showEditDeleteDialog = it }
                }
                is ChatEvent.ImagesSelected -> {}
                is ChatEvent.AttachmentClicked -> {}
                is ChatEvent.ImageSelected -> {}
                is ChatEvent.ImageDeselected -> {}
                is ChatEvent.SystemMessage -> snackbarHostState.showSnackbar(event.content)
            }
        }
    }

    LaunchedEffect(uiState.messages) {
        if (listState.firstVisibleItemIndex <= 1 && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            coroutineScope.launch {
                if(listState.layoutInfo.totalItemsCount > 0) {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(listState, uiState.isLoadingHistory, uiState.isLastPage) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                if (lastVisibleItem != null && lastVisibleItem.index == uiState.messages.size - 1 && !uiState.isLoadingHistory && !uiState.isLastPage) {
                    viewModel.loadMoreMessages()
                }
            }
    }

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
            ChatInputArea(
                modifier = Modifier.navigationBarsPadding().imePadding(),
                uiState = uiState,
                onMessageChange = viewModel::onMessageInputChange,
                onSendMessage = { 
                    if (uiState.isEditing) {
                        viewModel.confirmEditMessage()
                    } else {
                        viewModel.onSendMessageClick()
                    }
                },
                onAttachmentClick = viewModel::onAttachmentClick,
                onImageSelected = viewModel::onImageSelected,
                onImageDeselected = viewModel::onImageDeselected,
                onCancelEdit = viewModel::cancelEdit,
                onPickImages = { imagePickerLauncher.launch("image/*") }
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
                    listState = listState,
                    onMessageLongClick = viewModel::onMessageLongClick, 
                    onUserProfileClick = viewModel::onUserProfileClick 
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
                viewModel.startEditMessage(message.chatId, message.message) 
                showEditDeleteDialog = null
            },
            onDelete = {
                viewModel.confirmDeleteMessage(message.chatId) 
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
    listState: LazyListState,
    onMessageLongClick: (ChatMessageUiModel) -> Unit, 
    onUserProfileClick: (String) -> Unit 
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .testTag("message_list"),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp) // 입력창과의 간격 추가
    ) {
        if (uiState.isLoadingHistory) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }

        items(
            items = uiState.messages,
            key = { it.localId } 
        ) { message ->
            val isFirstInGroup = uiState.messages.indexOfFirst { it.localId == message.localId }
                .let { index ->
                    val nextMessage = uiState.messages.getOrNull(index + 1)
                    nextMessage == null || 
                    nextMessage.userId != message.userId ||
                    kotlin.math.abs(
                        (nextMessage.actualTimestamp.epochSecond) -
                        message.actualTimestamp.epochSecond
                    ) > 300
                }
            
            ChatMessageItemComposable(
                message = message,
                isFirstInGroup = isFirstInGroup,
                onLongClick = { onMessageLongClick(message) },
                onUserProfileClick = { onUserProfileClick(message.userId) }
            )
            
            Spacer(modifier = Modifier.height(if (isFirstInGroup) 16.dp else 0.dp))
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
                append(displayMessage)
                val urlRegex = """(https?://\S+)""".toRegex()
                urlRegex.findAll(displayMessage).forEach { matchResult ->
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

            Text(
                text = annotatedString,
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    if (message.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(12.dp)
                                .testTag("delivery_indicator"),
                            strokeWidth = 1.dp
                        )
                    } else if (message.sendFailed) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "전송 실패",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(12.dp)
                                .testTag("delivery_indicator")
                        )
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
    onPickImages: () -> Unit
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
                modifier = Modifier.fillMaxWidth().heightIn(max=200.dp) 
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
                OutlinedTextField(
                    value = uiState.pendingMessageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp) // 최대 높이 제한
                        .focusRequester(focusRequester)
                        .testTag("message_input_field"),
                    interactionSource = interactionSource,
                    placeholder = { Text("메시지 입력...") },
                    maxLines = 4, 
                    colors = TextFieldDefaults.colors( 
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, 
                        unfocusedIndicatorColor = Color.Transparent
                    )
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
            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
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
        Box(modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) { // 높이 지정
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
    val fakeListState = rememberLazyListState()
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
        ChatMessagesList(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            listState = fakeListState,
            onMessageLongClick = {},
            onUserProfileClick = {}
        )
    }
}

@Preview(showBackground = true, name="Chat Screen Preview")
@Composable
private fun ChatScreenFullPreview() {
    // 미리보기용 가짜 상태 데이터 생성
    val previewUiState = ChatUiState(
        channelId = "preview_channel",
        channelName = "미리보기 채팅방",
        messages = List(15) { i ->
            val isMy = i % 3 == 0
            ChatMessageUiModel(
                localId = (100 + i).toString(),
                chatId = "100$i",
                userId = (if (isMy) "1" else "${i + 2}"), // Int -> String 타입으로 수정
                userName = "사용자 ${if (isMy) 1 else i + 2}",
                userProfileUrl = null,
                message = "미리보기 메시지 내용입니다. ${15-i}",
                formattedTimestamp = "오후 ${ (15-i) % 12 + 1 }:${String.format(Locale.KOREAN,"%02d", (15-i)*3)}",
                isModified = i % 5 == 0,
                isMyMessage = isMy,
                isSending = false,
                sendFailed = i == 5, // 5번째 메시지 전송 실패 예시
                actualTimestamp = Instant.now() // 필수 파라미터 추가
            )
        }.reversed(), // 최신 메시지가 아래로 가도록 (LazyColumn reverseLayout=true 이므로)
        myUserId = "1", // Int -> String 타입으로 수정
        galleryImages = List(10) { GalleryImageUiModel(Uri.EMPTY, it.toString()) } // 가짜 갤러리 이미지
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
