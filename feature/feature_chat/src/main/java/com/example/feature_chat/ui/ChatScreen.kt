package com.example.feature_chat.ui

/** 다른 문제 전부 해결하고나면 VIewmodel구현 왜냐하면 chat은 다음주에 할일 이기때문
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage // Coil 라이브러리 사용 for attachments
import coil.request.ImageRequest // Used by AsyncImage for attachments
import com.example.core_ui.components.user.UserProfileImage // Import the new composable
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
// R import might be removed if UserProfileImage handles it all and no other direct R.drawable is used.
// For now, assume it might still be needed for fallbacks in UserProfileImage or other icons.
import com.example.core_ui.R
// ViewModel 및 관련 모델 Import
import com.example.feature_chat.model.ChatEvent
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.model.GalleryImageUiModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.core_navigation.core.AppNavigator
import java.util.Locale
import android.util.Log // Added for logging
import com.example.core_ui.components.buttons.DebouncedBackButton
import java.time.Instant

/**
 * ChatScreen: 채팅 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator,
    viewModel: ChatViewModel = hiltViewModel()
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
                    // 메시지가 실제로 추가된 후 스크롤하도록 약간의 지연 추가 고려
                    // delay(100) // 예시
                    if(listState.layoutInfo.totalItemsCount > 0) {
                        listState.animateScrollToItem(0)
                    }
                }
                is ChatEvent.ShowEditDeleteDialog -> showEditDeleteDialog = event.message // ★ 타입 변경됨
                is ChatEvent.ShowUserProfileDialog -> showUserProfileDialog = event.userId
                is ChatEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ChatEvent.ClearFocus -> focusManager.clearFocus()
                is ChatEvent.NavigateBack -> appNavigator.navigateBack()
                is ChatEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is ChatEvent.ShowMessageActions -> {
                    // 메시지 ID와 텍스트를 사용해 다이얼로그 표시
                    val message = uiState.messages.find { it.chatId == event.messageId }
                    message?.let { showEditDeleteDialog = it }
                }
                is ChatEvent.ImagesSelected -> {}
                is ChatEvent.AttachmentClicked -> {}
                is ChatEvent.ImageSelected -> {}
                is ChatEvent.ImageDeselected -> {}
            }
        }
    }

    // 새 메시지 수신 시 자동 스크롤 (더 정교한 조건 추가 가능)
    LaunchedEffect(uiState.messages) {
        if (listState.firstVisibleItemIndex <= 1 && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            coroutineScope.launch {
                if(listState.layoutInfo.totalItemsCount > 0) {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    // 과거 메시지 로드를 위한 스크롤 리스너
    LaunchedEffect(listState, uiState.isLoadingHistory, uiState.isLastPage) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                // 마지막 항목이 보이고, 로딩 중이 아니며, 마지막 페이지가 아닐 때 로드 요청
                if (lastVisibleItem != null && lastVisibleItem.index == uiState.messages.size - 1 && !uiState.isLoadingHistory && !uiState.isLastPage) {
                    viewModel.loadMoreMessages()
                }
            }
    }

    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
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
            // TODO: WebSocket 구현 후 채팅 입력 영역 활성화
            if (uiState.error == null || uiState.error?.contains("WebSocket 구현 예정") == false) {
            ChatInputArea(
                modifier = Modifier.navigationBarsPadding().imePadding(),
                uiState = uiState,
                onMessageChange = viewModel::onMessageInputChange,
                onSendMessage = { // 전송/수정 분기 처리
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
                // 채팅 비활성화 시 입력 영역을 보여주지 않거나, 다른 UI를 표시할 수 있습니다.
                // 여기서는 간단히 아무것도 표시하지 않도록 합니다.
                Log.d("ChatScreen", "Chat input area hidden as chat is pending WebSocket implementation.")
            }
        }
    ) { innerPadding ->
        // TODO: WebSocket 구현 후 메시지 목록 활성화
        if (uiState.error != null && uiState.error?.contains("WebSocket 구현 예정") == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            uiState = uiState,
            listState = listState,
            onMessageLongClick = viewModel::onMessageLongClick, // ViewModel 함수 직접 전달
            onUserProfileClick = viewModel::onUserProfileClick // ViewModel 함수 직접 전달
        )
        }
    }

    // 수정/삭제 다이얼로그
    showEditDeleteDialog?.let { message ->
        EditDeleteChatDialog(
            message = message, // ★ 타입 변경됨
            isMyMessage = message.isMyMessage, // UI 모델의 플래그 사용
            onDismiss = { showEditDeleteDialog = null },
            onEdit = {
                viewModel.startEditMessage(message.chatId, message.message) // chatID 사용
                showEditDeleteDialog = null
            },
            onDelete = {
                viewModel.confirmDeleteMessage(message.chatId) // chatID 사용
                showEditDeleteDialog = null
            }
        )
    }

    // 사용자 프로필 다이얼로그
    showUserProfileDialog?.let { userId ->
        UserProfileDialog(userId = userId, onDismiss = { showUserProfileDialog = null })
    }
}

// 사용자 프로필 다이얼로그 (임시 구현)
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

/**
 * ChatMessagesList: 채팅 메시지 목록 (Stateless)
 */
@Composable
fun ChatMessagesList(
    modifier: Modifier = Modifier,
    uiState: ChatUiState,
    listState: LazyListState,
    onMessageLongClick: (ChatMessageUiModel) -> Unit, // ★ 타입 변경
    onUserProfileClick: (String) -> Unit // Int -> String 타입으로 수정
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 8.dp),
        state = listState,
        reverseLayout = true // 최신 메시지가 하단에, 스크롤은 위로
    ) {
        // 스크롤 상단에 로딩 인디케이터 (과거 메시지 로딩)
        if (uiState.isLoadingHistory) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }

        // 메시지 목록 표시
        items(
            items = uiState.messages,
            key = { it.localId } // ★ 안정적인 키 사용
        ) { message ->
            ChatMessageItemComposable(
                message = message, // ★ 타입 변경됨
                onLongClick = { onMessageLongClick(message) },
                onUserProfileClick = { onUserProfileClick(message.userId) } // userId는 String 타입
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * ChatMessageItemComposable: 단일 채팅 메시지 UI (Stateless)
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatMessageItemComposable(
    message: ChatMessageUiModel, // ★ 타입 변경
    onLongClick: () -> Unit,
    onUserProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 메시지 행 정렬 (내가 보낸 메시지 vs 다른 사람 메시지) - 예시
    // 실제 구현 시 Row 대신 Box나 ConstraintLayout 등으로 더 복잡하게 구성될 수 있음
    val alignment = if (message.isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isMyMessage) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(alignment) // 정렬 적용
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.8f) // 최대 너비 제한
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 프로필 이미지 (내가 보낸 메시지에는 숨길 수도 있음)
            if (!message.isMyMessage) {
                UserProfileImage(
                    profileImageUrl = message.userProfileUrl,
                    contentDescription = "${message.userName} 프로필",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onUserProfileClick),
                    // contentScale is handled by UserProfileImage default or can be passed if needed
                )
            }

            // 메시지 내용 영역
            Column(
                modifier = Modifier
                    .background(backgroundColor, MaterialTheme.shapes.medium) // 배경색 및 둥근 모서리
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .combinedClickable( // 롱클릭 적용
                        onClick = { /* 일반 클릭은 Bubble 자체에는 불필요할 수 있음 */ },
                        onLongClick = onLongClick
                    )
            ) {
                // 사용자 이름 (다른 사람 메시지에만 표시)
                if (!message.isMyMessage) {
                    Text(
                        text = message.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 메시지 본문 및 링크 처리
                val displayMessage = message.message
                val annotatedString = buildAnnotatedString {
                    append(displayMessage)
                    val urlRegex = "(https?://\\S+)".toRegex()
                    urlRegex.findAll(displayMessage).forEach { matchResult ->
                        val url = matchResult.value
                        val startIndex = matchResult.range.first
                        val endIndex = matchResult.range.last + 1
                        addStringAnnotation("URL", url, startIndex, endIndex)
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.tertiary, // 링크 색상 변경
                                textDecoration = TextDecoration.Underline
                            ),
                            start = startIndex,
                            end = endIndex
                        )
                    }
                }

                Text(
                    text = annotatedString,
                    style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant) // 기본 텍스트 색상
                )

                // 첨부 이미지
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

                Spacer(modifier = Modifier.height(4.dp))

                // 시간 및 상태 표시 (정렬 조정)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isMyMessage) Arrangement.End else Arrangement.Start, // 시간 오른쪽/왼쪽 정렬
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isModified) {
                        Text(
                            text = " (수정됨)",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = message.formattedTimestamp, // ★ 포맷된 시간 사용
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    // 전송 중 또는 실패 아이콘 (내 메시지일 때만)
                    if(message.isMyMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        if (message.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                        } else if (message.sendFailed) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = "전송 실패", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}


/**
 * ChatInputArea: 메시지 입력 및 첨부 영역 (Stateless)
 */
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
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        // 수정 중 알림 바
        AnimatedVisibility(visible = uiState.isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

        // 선택된 이미지 미리보기 영역
        AnimatedVisibility(visible = uiState.selectedImages.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.selectedImages.forEach { uri ->
                    SelectedImagePreview(uri = uri, onRemove = { onImageDeselected(uri) })
                }
            }
        }

        // 이미지 선택 그리드 영역
        AnimatedVisibility(visible = uiState.isAttachmentAreaVisible) {
            ImageSelectionGrid(
                images = uiState.galleryImages, // ★ 타입 변경됨
                selectedImages = uiState.selectedImages,
                onImageSelected = onImageSelected,
                onImageDeselected = onImageDeselected,
                modifier = Modifier.fillMaxWidth().heightIn(max=200.dp) // 최대 높이 제한
            )
        }

        // 메시지 입력 및 전송 버튼 영역
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 이미지 첨부 버튼 (갤러리 열기)
                IconButton(onClick = onPickImages) { // 항상 갤러리 열도록 변경
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "이미지 첨부")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.messageInput,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지 입력...") },
                    maxLines = 4, // 여러 줄 입력 가능
                    colors = TextFieldDefaults.colors( // 배경 투명하게
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, // 밑줄 제거
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 전송 또는 수정 완료 버튼
                IconButton(
                    onClick = {
                        onSendMessage() // ViewModel의 전송/수정 로직 호출
                        // focusManager.clearFocus() // ViewModel에서 이벤트로 처리하는 것이 더 좋음
                    },
                    // 내용이 있거나, 이미지가 선택되었거나, 수정 중일 때 활성화. 단, 전송 중에는 비활성화
                    enabled = !uiState.isSendingMessage && (uiState.messageInput.isNotBlank() || uiState.selectedImages.isNotEmpty())
                ) {
                    // 전송 중이면 로딩 인디케이터, 아니면 아이콘 표시
                    if (uiState.isSendingMessage) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        // 수정 중일 때는 체크 아이콘, 아닐 때는 전송 아이콘
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
                    TextButton(onClick = onEdit) { Text("수정") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
*/