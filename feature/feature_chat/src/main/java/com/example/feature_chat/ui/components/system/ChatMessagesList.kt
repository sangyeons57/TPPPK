package com.example.feature_chat.ui.components.system

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.ChatUiState
import com.example.feature_chat.ui.components.message.ChatMessageItemComposable
import com.example.feature_chat.ui.components.message.MessageSkeletonItem
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
    onAddMember: (String, String) -> Unit = { _, _ -> }, // 멤버 추가 콜백 추가
    initialMessageId: String? = null
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .testTag("message_list"),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp), // 입력창과의 간격 추가
        userScrollEnabled = true // 스크롤 항상 가능하도록 해서 경계 도달 시 페이징이 막히지 않음
    ) {
        // Paging3 loading states + 로깅
        when (val loadState = lazyPagingItems.loadState.refresh) {
            is androidx.paging.LoadState.Loading -> {
                android.util.Log.d(
                    "ChatMessagesList",
                    "🔄 refresh Loading: items=${lazyPagingItems.itemCount}"
                )
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
                        android.util.Log.e(
                            "ChatMessagesList",
                            "❌ refresh Error: ${loadState.error}"
                        )
                    }
                }
            }

            else -> {
                android.util.Log.d(
                    "ChatMessagesList",
                    "✅ refresh NotLoading: items=${lazyPagingItems.itemCount}"
                )
            }
        }

        // Show loading indicator for append (load more)
        when (val appendState = lazyPagingItems.loadState.append) {
            is androidx.paging.LoadState.Loading -> {
                android.util.Log.d(
                    "ChatMessagesList",
                    "⬇️ append Loading: items=${lazyPagingItems.itemCount}"
                )
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

            is androidx.paging.LoadState.Error -> {
                android.util.Log.e("ChatMessagesList", "❌ append Error: ${appendState.error}")
            }

            else -> {
                android.util.Log.d(
                    "ChatMessagesList",
                    "✅ append NotLoading: items=${lazyPagingItems.itemCount}"
                )
            }
        }

        items(
            count = lazyPagingItems.itemCount,
            key = { index ->
                val message = lazyPagingItems[index]
                // 임시 메시지는 localId로, 실제 메시지는 chatId로 키 생성
                when {
                    message == null -> "loading_$index" // 로딩 중 null에 대해 인덱스 기반 고유 키
                    message.isOptimistic -> "temp_${message.messageId}"
                    else -> "actual_${message.messageId}"
                }
            }
        ) { index ->
            val message = lazyPagingItems[index]
            message?.let {
                // 날짜 구분선을 메시지와 함께 렌더링 (pseudo-item 충돌 방지)
                // reverseLayout에서 index+1이 시간상 더 과거 메시지임
                val shouldShowDateSeparator = shouldShowDateSeparatorBeforeMessage(
                    currentMessage = it,
                    nextMessage = if (index < lazyPagingItems.itemCount - 1) lazyPagingItems[index + 1] else null
                )

                // Note: With Paging3, grouping logic needs to be handled differently
                // For now, treat each message as first in group until grouping is reimplemented
                val isFirstInGroup = true

                // 메시지 전송 상태는 MessageStatusRow에서 처리하므로
                // formattedTimestamp는 항상 실제 시간을 표시
                val messageWithStatus = it

                // 단일 Column으로 날짜 구분선 + 메시지를 함께 렌더링
                androidx.compose.foundation.layout.Column {
                    if (shouldShowDateSeparator) {
                        DateSeparator(
                            displayText = formatDateForSeparator(it.actualTimestamp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ChatMessageItemComposable(
                        message = messageWithStatus,
                        isFirstInGroup = isFirstInGroup,
                        onLongClick = { onMessageLongClick(it) },
                        onUserProfileClick = { onUserProfileClick(it.userId) },
                        onRetryMessage = onRetryMessage,
                        onJoinProject = { projectId ->
                            // 프로젝트 참여 로직 - 추후 ViewModel에서 처리
                            android.util.Log.d("ChatScreen", "프로젝트 참여 요청: $projectId")
                            // TODO: 프로젝트 참여 이벤트 처리
                        },
                        onAddMember = onAddMember,
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
                }

                Spacer(modifier = Modifier.height(if (isFirstInGroup) 16.dp else 0.dp))
            }
        }
    }
}

/**
 * 현재 메시지 앞에 날짜 구분선을 표시해야 하는지 판단 (reverseLayout 기준)
 */
private fun shouldShowDateSeparatorBeforeMessage(
    currentMessage: ChatMessageUiModel,
    nextMessage: ChatMessageUiModel? // reverseLayout에서 시간상 더 과거 메시지
): Boolean {
    // 가장 과거 메시지(마지막 인덱스)에는 항상 날짜 구분선 표시 (일반적인 채팅 앱 관례)
    if (nextMessage == null) return true

    val currentDate = currentMessage.actualTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()
    val nextDate = nextMessage.actualTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()

    // 현재 메시지와 다음(더 과거) 메시지의 날짜가 다르면 구분선 표시
    return !currentDate.isEqual(nextDate)
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
