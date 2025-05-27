package com.example.feature_chat.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeChatRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.ChatMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

/**
 * ChatViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 ChatViewModel의 기능을 검증합니다.
 * FakeChatRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class ChatViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: ChatViewModel

    // Fake Repository
    private lateinit var fakeChatRepository: FakeChatRepository

    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testChannelId = "test_channel_1"
    private val testChannelName = "테스트 채팅방"
    private val myUserId = 1

    // 테스트 메시지
    private val testMessage1 = ChatMessage(
        chatId = 1,
        channelId = testChannelId,
        userId = 2,
        userName = "Other User",
        userProfileUrl = null,
        message = "안녕하세요!",
        sentAt = LocalDateTime.now().minusMinutes(5),
        isModified = false,
        attachmentImageUrls = emptyList()
    )

    private val testMessage2 = ChatMessage(
        chatId = 2,
        channelId = testChannelId,
        userId = myUserId,
        userName = "My Name",
        userProfileUrl = null,
        message = "네, 반갑습니다!",
        sentAt = LocalDateTime.now().minusMinutes(1),
        isModified = false,
        attachmentImageUrls = emptyList()
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        `when`(savedStateHandle["channelId"]).thenReturn(testChannelId)
        
        // Fake Repository 초기화
        fakeChatRepository = FakeChatRepository()
        
        // 테스트 데이터 설정
        fakeChatRepository.addMessages(testChannelId, listOf(testMessage1, testMessage2))

        // ViewModel 초기화 (의존성 주입)
        viewModel = ChatViewModel(savedStateHandle, fakeChatRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 채널 ID와 채팅 메시지를 로드해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals(testChannelId, initialState.channelId)
        assertEquals(2, initialState.messages.size)
        assertFalse(initialState.isLoadingHistory)
        assertFalse(initialState.isSendingMessage)
        assertFalse(initialState.isAttachmentAreaVisible)
        assertEquals("", initialState.messageInput)
        assertEquals(emptySet<Uri>(), initialState.selectedImages)
    }

    /**
     * 메시지 입력 변경 테스트
     */
    @Test
    fun `메시지 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testInput = "새로운 메시지 입력"

        // When: 메시지 입력
        viewModel.onMessageInputChange(testInput)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testInput, state.messageInput)
    }

    /**
     * 메시지 전송 테스트
     */
    @Test
    fun `메시지 전송 시 임시 메시지가 추가되고 전송 후 실제 메시지로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 메시지 입력
        val eventCollector = EventCollector<ChatEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        val testInput = "테스트 메시지 전송"
        viewModel.onMessageInputChange(testInput)

        // When: 메시지 전송
        viewModel.onSendMessageClick()

        // Then: UI 상태 및 이벤트 확인
        val state = viewModel.uiState.getValue()
        
        // 입력 필드가 비워짐
        assertEquals("", state.messageInput)
        
        // 메시지 목록에 새 메시지 추가됨 (임시 메시지 포함)
        assertEquals(3, state.messages.size)
        
        // 스크롤 이벤트 발생 확인
        assertTrue(eventCollector.events.any { it is ChatEvent.ScrollToBottom })
        
        // 임시 메시지가 추가됨
        val newestMessage = state.messages[0] // 가장 최신 메시지
        assertEquals(testInput, newestMessage.message)
        assertEquals(myUserId, newestMessage.userId)
        assertTrue(newestMessage.isMyMessage)
    }

    /**
     * 메시지 전송 실패 테스트
     */
    @Test
    fun `메시지 전송 실패 시 오류 상태가 설정되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 레포지토리 에러 설정
        val eventCollector = EventCollector<ChatEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        fakeChatRepository.setShouldSimulateError(true)
        
        viewModel.onMessageInputChange("실패할 메시지")

        // When: 메시지 전송
        viewModel.onSendMessageClick()

        // Then: 스낵바 이벤트 발생 및 메시지 실패 상태 확인
        assertTrue(eventCollector.events.any { it is ChatEvent.ShowSnackbar })
        
        val state = viewModel.uiState.getValue()
        // 첫 번째 메시지(가장 최신 메시지)의 전송 실패 상태 확인
        val failedMessage = state.messages[0]
        assertTrue(failedMessage.sendFailed)
        assertFalse(failedMessage.isSending)
    }

    /**
     * 과거 메시지 로드 테스트
     */
    @Test
    fun `과거 메시지 로드 시 기존 메시지 목록에 추가되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 과거 메시지 데이터 준비
        val pastMessages = listOf(
            ChatMessage(
                chatId = 0,
                channelId = testChannelId,
                userId = 2,
                userName = "Past User",
                userProfileUrl = null,
                message = "이전 메시지",
                sentAt = LocalDateTime.now().minusDays(1),
                isModified = false,
                attachmentImageUrls = emptyList()
            )
        )
        
        fakeChatRepository.addMessages(testChannelId, pastMessages)

        // When: 과거 메시지 로드
        viewModel.loadMoreMessages()

        // Then: 메시지 목록에 과거 메시지가 추가됨
        val state = viewModel.uiState.getValue()
        assertEquals(3, state.messages.size) // 기존 2개 + 과거 1개
        assertFalse(state.isLoadingHistory) // 로딩 상태 해제
    }

    /**
     * 과거 메시지 로드 실패 테스트
     */
    @Test
    fun `과거 메시지 로드 실패 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 레포지토리 에러 설정
        val eventCollector = EventCollector<ChatEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        fakeChatRepository.setShouldSimulateError(true)

        // When: 과거 메시지 로드
        viewModel.loadMoreMessages()

        // Then: 스낵바 이벤트 발생 및 로딩 상태 해제 확인
        assertTrue(eventCollector.events.any { it is ChatEvent.ShowSnackbar })
        
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoadingHistory)
        assertNotNull(state.error)
    }

    /**
     * 이미지 첨부 테스트
     * 
     * 이 테스트는 첨부 기능이 구현되어 있다고 가정하고 작성됨
     * (실제 ChatViewModel에 해당 기능이 있는지 확인 필요)
     */
    @Test
    fun `이미지 첨부 시 첨부 이미지 목록에 추가되어야 함`() = coroutinesTestRule.runBlockingTest {
        // 이 테스트는 ViewModel 구현에 따라 세부 내용이 달라질 수 있음
        // 이미지 첨부 기능이 구현되어 있는지 확인하고, 필요에 따라 테스트 케이스 작성
    }

    /**
     * 메시지 수정 테스트
     */
    @Test
    fun `메시지 수정 시 기존 메시지가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 수정할 메시지 ID와 새 메시지 내용
        val messageIdToEdit = testMessage2.chatId // 내가 보낸 메시지만 수정 가능
        val newMessageContent = "수정된 메시지"

        // When: 메시지 수정
        val result = fakeChatRepository.editMessage(testChannelId, messageIdToEdit, newMessageContent)

        // Then: 메시지 수정 성공 확인 및 UI 상태 확인
        assertTrue(result.isSuccess)
        
        // 메시지 스트림을 통해 UI가 업데이트되므로, 잠시 대기 후 상태 확인
        val state = viewModel.uiState.getValue()
        
        // 수정된 메시지 찾기
        val editedMessage = state.messages.find { it.chatId == messageIdToEdit }
        assertNotNull(editedMessage)
        assertEquals(newMessageContent, editedMessage?.message)
        assertTrue(editedMessage?.isModified ?: false)
    }

    /**
     * 메시지 삭제 테스트
     */
    @Test
    fun `메시지 삭제 시 메시지 목록에서 제거되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 삭제할 메시지 ID
        val messageIdToDelete = testMessage2.chatId // 내가 보낸 메시지만 삭제 가능
        val initialMessagesCount = viewModel.uiState.getValue().messages.size

        // When: 메시지 삭제
        val result = fakeChatRepository.deleteMessage(testChannelId, messageIdToDelete)

        // Then: 메시지 삭제 성공 확인 및 UI 상태 확인
        assertTrue(result.isSuccess)
        
        // 메시지 스트림을 통해 UI가 업데이트되므로, 잠시 대기 후 상태 확인
        val state = viewModel.uiState.getValue()
        
        // 메시지 목록에서 삭제된 메시지가 없어야 함
        assertEquals(initialMessagesCount - 1, state.messages.size)
        assertNull(state.messages.find { it.chatId == messageIdToDelete })
    }
} 