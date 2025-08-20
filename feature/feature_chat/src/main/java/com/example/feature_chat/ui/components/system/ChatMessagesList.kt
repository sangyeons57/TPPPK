package com.example.feature_chat.ui.components.system

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.example.core_common.constants.PagingConstants
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.ui.components.message.ChatMessageItemComposable
import com.example.feature_chat.ui.components.message.MessageSkeletonItem
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatMessagesList(
    modifier: Modifier = Modifier,
    uiState: ChatUiState,
    lazyPagingItems: LazyPagingItems<ChatMessageUiModel>,
    listState: LazyListState,
    onMessageLongClick: (ChatMessageUiModel) -> Unit,
    onUserProfileClick: (String) -> Unit,
    onRetryMessage: (String) -> Unit = { _ -> },
    onJoinProject: (String) -> Unit = { _ -> },
    onCheckMembership: (String) -> Unit = { _ -> }, // 프로젝트 멤버십 확인 콜백 추가
    projectMembershipStatesFlow: StateFlow<Map<String, Boolean>> = kotlinx.coroutines.flow.MutableStateFlow(
        emptyMap()
    ), // 프로젝트 멤버십 상태 StateFlow
    onAddMember: (String, String) -> Unit = { _, _ -> }, // 멤버 추가 콜백 추가
    onImageClick: (String, List<String>, Int) -> Unit = { _, _, _ -> }, // 이미지 클릭 콜백 추가
    initialMessageId: String? = null
) {
    rememberCoroutineScope()

    // Note: Avoid manual scroll repositioning; rely on Paging3 + stable keys to prevent bounce.
    
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .testTag("message_list"),
        state = listState,
        reverseLayout = true, // 채팅 표준: 최신 메시지가 하단에, 과거 메시지가 상단에
        contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp), // 입력창과의 간격 추가
        userScrollEnabled = true // 스크롤 항상 가능하도록 해서 경계 도달 시 페이징이 막히지 않음
    ) {
        // Paging3 loading states + 로깅
        when (val loadState = lazyPagingItems.loadState.refresh) {
            is androidx.paging.LoadState.Loading -> {
                // Show skeleton UI instead of simple progress indicator
                items(7) { // Show 5 skeleton items
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
                        android.util.Log.e(
                            "ChatMessagesList",
                            "❌ refresh Error: ${loadState.error}"
                        )
                    }
                }
            }

            else -> {
                // No-op
            }
        }

        items(
            count = lazyPagingItems.itemCount,
            key = { index ->
                val message = lazyPagingItems[index]
                // 향상된 키 안정성: 메시지 상태와 타임스탬프 기반 키 생성
                when {
                    message == null -> "loading_$index" // 로딩 중 null에 대해 인덱스 기반 고유 키
                    message.isOptimistic -> {
                        // 임시 메시지: clientSentAt가 없으면 actualTimestamp로 고정 키 생성
                        val ts = message.clientSentAt?.toEpochMilli()
                            ?: message.actualTimestamp.toEpochMilli()
                        "temp_${message.messageId}_$ts"
                    }
                    else -> {
                        // 실제 메시지: 서버 타임스탬프와 함께 키 생성하여 중복 방지
                        val timestamp = message.actualTimestamp.toEpochMilli()
                        "actual_${message.messageId}_$timestamp"
                    }
                }
            },
            contentType = { index ->
                val item = lazyPagingItems[index]
                item?.messageType ?: "loading"
            }
        ) { index ->
            val message = lazyPagingItems[index]
            message?.let {
                // 날짜 구분선을 메시지와 함께 렌더링 (pseudo-item 충돌 방지)
                // DESC 정렬 기준으로 이전 아이템이 더 최신 메시지임
                val shouldShowDateSeparator = shouldShowDateSeparatorBeforeMessage(
                    currentMessage = it,
                    previousMessage = if (index > 0) lazyPagingItems[index - 1] else null
                )
                // 리스트 최하단(마지막 메시지)에서는 날짜 구분선 표시를 생략
                val isLastItem = index == lazyPagingItems.itemCount - 1
                val showDateSeparator = shouldShowDateSeparator && !isLastItem

                // Note: With Paging3, grouping logic needs to be handled differently
                // For now, treat each message as first in group until grouping is reimplemented
                val isFirstInGroup = true

                // 메시지 전송 상태는 MessageStatusRow에서 처리하므로
                // formattedTimestamp는 항상 실제 시간을 표시
                val messageWithStatus = it

                // 단일 Column으로 날짜 구분선 + 메시지를 함께 렌더링
                androidx.compose.foundation.layout.Column {
                    // 먼저 메시지를 렌더링하고, 그 아래(ReverseLayout 기준으로 이전 아이템과의 경계)에 날짜 구분선을 표시
                    ChatMessageItemComposable(
                        message = messageWithStatus,
                        isFirstInGroup = isFirstInGroup,
                        onLongClick = { onMessageLongClick(it) },
                        onUserProfileClick = { onUserProfileClick(it.userId) },
                        onRetryMessage = onRetryMessage,
                        onJoinProject = onJoinProject,
                        onCheckMembership = onCheckMembership,
                        projectMembershipStatesFlow = projectMembershipStatesFlow,
                        onAddMember = onAddMember,
                        onImageClick = onImageClick,
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

                    if (showDateSeparator) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DateSeparator(
                            displayText = formatDateForSeparator(it.actualTimestamp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isFirstInGroup) 16.dp else 0.dp))
            }
        }
        
        // "채팅 시작" 텍스트 - reverseLayout에서 상단(과거 메시지 영역)에 표시
        // 진짜 endOfPagination이면서 MAX_SIZE_THRESHOLD 미만일 때만 표시
        val appendState = lazyPagingItems.loadState.append
        if (appendState is androidx.paging.LoadState.NotLoading && 
            appendState.endOfPaginationReached && 
            lazyPagingItems.itemCount > 0 && 
            lazyPagingItems.itemCount < PagingConstants.MAX_SIZE_THRESHOLD) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "채팅 시작", 
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 현재 메시지 앞에 날짜 구분선을 표시해야 하는지 판단 (ASC 기준)
 */
private fun shouldShowDateSeparatorBeforeMessage(
    currentMessage: ChatMessageUiModel,
    previousMessage: ChatMessageUiModel? // ASC에서 시간상 더 과거 메시지
): Boolean {
    // 첫 메시지(이전 항목 없음)에는 날짜 구분선 표시하지 않음 (리스트 상단 헤더가 아님)
    if (previousMessage == null) return false

    val currentDate = currentMessage.actualTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()
    val prevDate = previousMessage.actualTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()

    // 이전(더 최신) 메시지와 날짜가 다르면 경계 아래에 구분선 표시
    return !currentDate.isEqual(prevDate)
}

/**
 * 날짜를 구분선에 표시할 형식으로 포맷팅
 */
private fun formatDateForSeparator(instant: Instant): String {
    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (localDate) {
        today -> "오늘"
        yesterday -> "어제"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN)
            localDate.format(formatter)
        }
    }
}
