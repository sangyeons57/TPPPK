package com.example.feature_chat.ui

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_chat.viewmodel.ChatMessageUiModel
import com.example.feature_chat.viewmodel.ChatUiState
import com.example.feature_chat.viewmodel.GalleryImageUiModel
import org.junit.Rule
import org.junit.Test

/**
 * ChatScreen UI 테스트
 *
 * 이 테스트는 ChatScreen의 주요 UI 컴포넌트와 상호작용을 검증합니다.
 * 테스트에서는 주로 stateless 컴포넌트를 직접 테스트합니다.
 */
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 기본 채팅 화면 메시지 목록 렌더링 테스트
     */
    @Test
    fun chatMessagesList_renders_messages() {
        // Given: 메시지가 포함된 UI 상태
        val messages = listOf(
            createChatMessage(1, "첫 번째 메시지", isMyMessage = true),
            createChatMessage(2, "두 번째 메시지", isMyMessage = false, userName = "홍길동")
        )
        
        val uiState = ChatUiState(
            messages = messages,
            channelName = "테스트 채널"
        )
        
        // When: ChatMessagesList 렌더링
        composeTestRule.setContent {
            ChatMessagesList(
                uiState = uiState,
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                onMessageLongClick = {},
                onUserProfileClick = {}
            )
        }
        
        // Then: 메시지가 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("첫 번째 메시지").assertIsDisplayed()
        composeTestRule.onNodeWithText("두 번째 메시지").assertIsDisplayed()
        composeTestRule.onNodeWithText("홍길동").assertIsDisplayed()
    }
    
    /**
     * 단일 메시지 렌더링 테스트 - 내 메시지
     */
    @Test
    fun chatMessageItem_renders_myMessage() {
        // Given: 내가 보낸 메시지
        val message = createChatMessage(1, "테스트 메시지", isMyMessage = true)
        
        // When: ChatMessageItemComposable 렌더링
        composeTestRule.setContent {
            ChatMessageItemComposable(
                message = message,
                onLongClick = {},
                onUserProfileClick = {}
            )
        }
        
        // Then: 내 메시지 형태로 렌더링되는지 확인 (홍길동 표시 없음)
        composeTestRule.onNodeWithText("테스트 메시지").assertIsDisplayed()
        composeTestRule.onNodeWithText("홍길동").assertDoesNotExist()
        composeTestRule.onNodeWithText("10:30").assertIsDisplayed() // 시간 표시 확인
    }
    
    /**
     * 단일 메시지 렌더링 테스트 - 다른 사람 메시지
     */
    @Test
    fun chatMessageItem_renders_otherUserMessage() {
        // Given: 다른 사람이 보낸 메시지
        val message = createChatMessage(2, "안녕하세요", isMyMessage = false, userName = "김철수")
        
        // When: ChatMessageItemComposable 렌더링
        composeTestRule.setContent {
            ChatMessageItemComposable(
                message = message,
                onLongClick = {},
                onUserProfileClick = {}
            )
        }
        
        // Then: 다른 사람의 메시지 형태로 렌더링되는지 확인 (이름 표시)
        composeTestRule.onNodeWithText("안녕하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("김철수").assertIsDisplayed() // 사용자 이름 표시
        composeTestRule.onNodeWithText("10:30").assertIsDisplayed()
    }
    
    /**
     * 메시지 전송 중 상태 테스트
     */
    @Test
    fun chatMessageItem_whenSending_showsIndicator() {
        // Given: 전송 중인 메시지
        val message = createChatMessage(
            id = 3, 
            message = "전송 중인 메시지", 
            isMyMessage = true,
            isSending = true
        )
        
        // When: ChatMessageItemComposable 렌더링
        composeTestRule.setContent {
            ChatMessageItemComposable(
                message = message,
                onLongClick = {},
                onUserProfileClick = {}
            )
        }
        
        // Then: 전송 중 상태가 표시되는지 확인
        composeTestRule.onNodeWithText("전송 중인 메시지").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("전송 중") or hasTestTag("전송중인디케이터")).assertExists()
    }
    
    /**
     * 채팅 입력 영역 테스트
     */
    @Test
    fun chatInputArea_renders_messageInput() {
        // Given: 기본 UI 상태
        val uiState = ChatUiState(
            messageInput = "안녕하세요"
        )
        
        // When: ChatInputArea 렌더링
        composeTestRule.setContent {
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
        
        // Then: 메시지 입력 영역이 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("안녕하세요").assertIsDisplayed()
    }
    
    /**
     * 메시지 전송 콜백 테스트
     */
    @Test
    fun chatInputArea_whenSendButtonClicked_triggersCallback() {
        // Given: 콜백 호출 추적 변수
        var sendMessageClicked = false
        val uiState = ChatUiState(
            messageInput = "테스트 메시지"
        )
        
        // When: ChatInputArea 렌더링 및 버튼 클릭
        composeTestRule.setContent {
            ChatInputArea(
                uiState = uiState,
                onMessageChange = {},
                onSendMessage = { sendMessageClicked = true },
                onAttachmentClick = {},
                onImageSelected = {},
                onImageDeselected = {},
                onCancelEdit = {},
                onPickImages = {}
            )
        }
        
        // 전송 버튼 클릭
        composeTestRule.onNodeWithContentDescription("메시지 전송").performClick()
        
        // Then: 전송 콜백이 호출되었는지 확인
        assert(sendMessageClicked) { "전송 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 메시지 수정 모드 테스트
     */
    @Test
    fun chatInputArea_whenEditing_showsEditingState() {
        // Given: 메시지 수정 중인 상태
        val uiState = ChatUiState(
            messageInput = "수정할 메시지",
            isEditing = true
        )
        
        // When: ChatInputArea 렌더링
        composeTestRule.setContent {
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
        
        // Then: 수정 중임을 나타내는 UI 요소 확인
        composeTestRule.onNodeWithText("메시지 수정 중...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("수정 취소").assertIsDisplayed()
    }
    
    /**
     * 이미지 선택 영역 테스트
     */
    @Test
    fun chatInputArea_whenImagesSelected_showsSelectedImages() {
        // Given: 선택된 이미지가 있는 상태
        val uiState = ChatUiState(
            messageInput = "",
            selectedImages = listOf(Uri.parse("content://test/image1"), Uri.parse("content://test/image2"))
        )
        
        // When: ChatInputArea 렌더링
        composeTestRule.setContent {
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
        
        // Then: 선택된 이미지 미리보기 영역이 표시되는지 확인
        composeTestRule.onNode(hasTestTag("선택된 이미지 미리보기") or hasTestTag("선택된 이미지 영역")).assertExists()
    }
    
    /**
     * 수정 취소 버튼 클릭 테스트
     */
    @Test
    fun chatInputArea_whenCancelEditClicked_triggersCallback() {
        // Given: 메시지 수정 중인 상태
        var cancelEditClicked = false
        val uiState = ChatUiState(
            messageInput = "수정 중인 메시지",
            isEditing = true
        )
        
        // When: ChatInputArea 렌더링 및 취소 버튼 클릭
        composeTestRule.setContent {
            ChatInputArea(
                uiState = uiState,
                onMessageChange = {},
                onSendMessage = {},
                onAttachmentClick = {},
                onImageSelected = {},
                onImageDeselected = {},
                onCancelEdit = { cancelEditClicked = true },
                onPickImages = {}
            )
        }
        
        // 수정 취소 버튼 클릭
        composeTestRule.onNodeWithContentDescription("수정 취소").performClick()
        
        // Then: 취소 콜백이 호출되었는지 확인
        assert(cancelEditClicked) { "수정 취소 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 이미지 첨부 버튼 테스트
     */
    @Test
    fun chatInputArea_whenAttachButtonClicked_triggersCallback() {
        // Given: 콜백 호출 추적 변수
        var pickImagesClicked = false
        val uiState = ChatUiState()
        
        // When: ChatInputArea 렌더링 및 첨부 버튼 클릭
        composeTestRule.setContent {
            ChatInputArea(
                uiState = uiState,
                onMessageChange = {},
                onSendMessage = {},
                onAttachmentClick = {},
                onImageSelected = {},
                onImageDeselected = {},
                onCancelEdit = {},
                onPickImages = { pickImagesClicked = true }
            )
        }
        
        // 이미지 첨부 버튼 클릭
        composeTestRule.onNodeWithContentDescription("이미지 첨부").performClick()
        
        // Then: 이미지 선택 콜백이 호출되었는지 확인
        assert(pickImagesClicked) { "이미지 첨부 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 메시지 입력 변경 테스트
     */
    @Test
    fun chatInputArea_whenTextInputChanged_triggersCallback() {
        // Given: 콜백 호출 추적 변수
        var messageChanged = false
        val uiState = ChatUiState(messageInput = "")
        
        // When: ChatInputArea 렌더링 및 텍스트 입력
        composeTestRule.setContent {
            ChatInputArea(
                uiState = uiState,
                onMessageChange = { messageChanged = true },
                onSendMessage = {},
                onAttachmentClick = {},
                onImageSelected = {},
                onImageDeselected = {},
                onCancelEdit = {},
                onPickImages = {}
            )
        }
        
        // 메시지 입력
        composeTestRule.onNodeWithText("메시지 입력...").performTextInput("안녕하세요")
        
        // Then: 메시지 변경 콜백이 호출되었는지 확인
        assert(messageChanged) { "메시지 입력 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 긴 메시지 표시 테스트
     */
    @Test
    fun chatMessageItem_withLongMessage_displaysCorrectly() {
        // Given: 긴 메시지
        val longMessage = "이것은 매우 긴 메시지입니다. 여러 줄에 걸쳐 표시되어야 하며 UI가 올바르게 조정되는지 확인합니다. " +
                "메시지가 너무 길면 레이아웃이 깨질 수 있기 때문에 이 테스트는 중요합니다."
        val message = createChatMessage(4, longMessage, isMyMessage = true)
        
        // When: ChatMessageItemComposable 렌더링
        composeTestRule.setContent {
            ChatMessageItemComposable(
                message = message,
                onLongClick = {},
                onUserProfileClick = {}
            )
        }
        
        // Then: 긴 메시지가 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText(longMessage).assertIsDisplayed()
    }
    
    /**
     * 이미지 첨부가 있는 메시지 테스트
     */
    @Test
    fun chatMessageItem_withImageAttachments_displaysImages() {
        // Given: 이미지 첨부가 있는 메시지
        val imageUrls = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        )
        val message = createChatMessage(
            id = 5,
            message = "이미지 첨부 테스트",
            isMyMessage = true,
            attachmentImageUrls = imageUrls
        )
        
        // When: ChatMessageItemComposable 렌더링
        composeTestRule.setContent {
            ChatMessageItemComposable(
                message = message,
                onLongClick = {},
                onUserProfileClick = {}
            )
        }
        
        // Then: 메시지와 이미지 첨부 영역이 표시되는지 확인
        composeTestRule.onNodeWithText("이미지 첨부 테스트").assertIsDisplayed()
        
        // 이미지 첨부 컨테이너 확인
        composeTestRule.onNode(hasTestTag("첨부이미지영역") or hasContentDescription("첨부 이미지")).assertExists()
    }
    
    /**
     * 메시지 수정/삭제 다이얼로그 테스트
     */
    @Test
    fun editDeleteChatDialog_showsOptionsAndTriggersCallbacks() {
        // Given: 콜백 호출 추적 변수
        var onEditClicked = false
        var onDeleteClicked = false
        var onDismissClicked = false
        
        val message = createChatMessage(
            id = 6,
            message = "수정 삭제 테스트",
            isMyMessage = true
        )
        
        // When: EditDeleteChatDialog 렌더링
        composeTestRule.setContent {
            EditDeleteChatDialog(
                message = message,
                isMyMessage = true,
                onDismiss = { onDismissClicked = true },
                onEdit = { onEditClicked = true },
                onDelete = { onDeleteClicked = true }
            )
        }
        
        // Then: 다이얼로그 옵션이 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("메시지 옵션").assertIsDisplayed()
        composeTestRule.onNodeWithText("수정").assertIsDisplayed()
        composeTestRule.onNodeWithText("삭제").assertIsDisplayed()
        
        // 수정 버튼 클릭
        composeTestRule.onNodeWithText("수정").performClick()
        assert(onEditClicked) { "수정 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 다시 렌더링해서 삭제 버튼 테스트
        composeTestRule.setContent {
            EditDeleteChatDialog(
                message = message,
                isMyMessage = true,
                onDismiss = { onDismissClicked = true },
                onEdit = { onEditClicked = true },
                onDelete = { onDeleteClicked = true }
            )
        }
        
        // 삭제 버튼 클릭
        composeTestRule.onNodeWithText("삭제").performClick()
        assert(onDeleteClicked) { "삭제 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    // 테스트용 ChatMessageUiModel 생성 헬퍼 함수
    private fun createChatMessage(
        id: Int,
        message: String,
        isMyMessage: Boolean,
        userName: String = "나",
        isSending: Boolean = false,
        sendFailed: Boolean = false,
        isModified: Boolean = false,
        attachmentImageUrls: List<String> = emptyList()
    ): ChatMessageUiModel {
        return ChatMessageUiModel(
            localId = "local_$id",
            chatId = "chat_$id",
            message = message,
            userId = id,
            userName = userName,
            userProfileUrl = null,
            timestamp = System.currentTimeMillis(),
            formattedTimestamp = "10:30",
            isMyMessage = isMyMessage,
            isSending = isSending,
            sendFailed = sendFailed,
            isModified = isModified,
            attachmentImageUrls = attachmentImageUrls
        )
    }
} 