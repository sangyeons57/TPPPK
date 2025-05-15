package com.example.feature_chat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.model.ChatMessage
import com.example.domain.repository.ChannelRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 메시지 전송 및 수신에 관한 UI 통합 테스트
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MessageSendReceiveTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestChannelActivity>()

    @Inject
    lateinit var channelRepository: ChannelRepository

    private lateinit var testChannelId: String
    private lateinit var testChannelName: String

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Create a test channel for all tests
        testChannelName = "메시지 테스트 채널 ${System.currentTimeMillis()}"
        
        runBlocking {
            val result = channelRepository.createChannel(
                name = testChannelName,
                description = "메시지 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(composeTestRule.activity.viewModel.getCurrentUserId()),
                metadata = null
            )
            
            testChannelId = result.getOrNull()?.id ?: ""
            assert(testChannelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // Navigate to channel list and refresh
        composeTestRule.onNodeWithText("채널").performClick()
        
        // Wait for channel to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(testChannelName).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sendMessage_shouldAppearInMessageList() {
        // Given - Navigate to the test channel
        composeTestRule.onNodeWithText(testChannelName).performClick()
        
        // Wait for channel to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(testChannelName).fetchSemanticsNodes().size == 1
        }
        
        // When - Type and send a message
        val messageText = "테스트 메시지 ${System.currentTimeMillis()}"
        composeTestRule.onNodeWithText("메시지 입력...").performTextInput(messageText)
        composeTestRule.onNodeWithContentDescription("메시지 전송").performClick()
        
        // Then - Message should appear in the message list
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(messageText).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify message exists in repository
        runBlocking {
            val messages = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull() ?: emptyList()
            
            assert(messages.any { it.text == messageText }) {
                "전송한 메시지가 repository에 존재하지 않습니다."
            }
        }
    }

    @Test
    fun editMessage_shouldUpdateMessageText() {
        // Given - Send a message to edit
        val originalText = "수정할 메시지 ${System.currentTimeMillis()}"
        val editedText = "수정된 메시지 ${System.currentTimeMillis()}"
        var messageId: String
        
        // Navigate to the test channel
        composeTestRule.onNodeWithText(testChannelName).performClick()
        
        // Wait for channel screen to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(testChannelName).fetchSemanticsNodes().size == 1
        }
        
        // Send a message
        composeTestRule.onNodeWithText("메시지 입력...").performTextInput(originalText)
        composeTestRule.onNodeWithContentDescription("메시지 전송").performClick()
        
        // Wait for message to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(originalText).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Get message ID
        runBlocking {
            val messages = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull() ?: emptyList()
            
            val message = messages.find { it.text == originalText }
            assert(message != null) { "전송한 메시지를 찾을 수 없습니다." }
            
            messageId = message?.id ?: ""
        }
        
        // When - Long press on the message and edit it
        composeTestRule.onNodeWithText(originalText).performLongClick()
        composeTestRule.onNodeWithText("수정하기").performClick()
        
        // Clear input and type new text
        composeTestRule.onNode(hasText(originalText) and hasSetTextAction()).performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(editedText)
        composeTestRule.onNodeWithContentDescription("메시지 전송").performClick()
        
        // Then - Message should be updated
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(editedText).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify message was updated in repository
        runBlocking {
            val message = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull()?.find { it.id == messageId }
            
            assert(message?.text == editedText) {
                "메시지가 업데이트되지 않았습니다."
            }
            assert(message?.isEdited == true) {
                "메시지가 수정됨으로 표시되지 않았습니다."
            }
        }
    }

    @Test
    fun deleteMessage_shouldMarkAsDeleted() {
        // Given - Send a message to delete
        val messageText = "삭제할 메시지 ${System.currentTimeMillis()}"
        var messageId: String
        
        // Navigate to the test channel
        composeTestRule.onNodeWithText(testChannelName).performClick()
        
        // Wait for channel screen to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(testChannelName).fetchSemanticsNodes().size == 1
        }
        
        // Send a message
        composeTestRule.onNodeWithText("메시지 입력...").performTextInput(messageText)
        composeTestRule.onNodeWithContentDescription("메시지 전송").performClick()
        
        // Wait for message to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(messageText).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Get message ID
        runBlocking {
            val messages = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull() ?: emptyList()
            
            val message = messages.find { it.text == messageText }
            assert(message != null) { "전송한 메시지를 찾을 수 없습니다." }
            
            messageId = message?.id ?: ""
        }
        
        // When - Long press on the message and delete it
        composeTestRule.onNodeWithText(messageText).performLongClick()
        composeTestRule.onNodeWithText("삭제하기").performClick()
        composeTestRule.onNodeWithText("확인").performClick()
        
        // Then - Message should be marked as deleted in repository
        runBlocking {
            val message = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull()?.find { it.id == messageId }
            
            assert(message?.isDeleted == true) {
                "메시지가 삭제됨으로 표시되지 않았습니다."
            }
        }
    }

    @Test
    fun addReaction_shouldUpdateMessageReactions() {
        // Given - Send a message to react to
        val messageText = "리액션 테스트 메시지 ${System.currentTimeMillis()}"
        val reaction = "👍"
        var messageId: String
        
        // Navigate to the test channel
        composeTestRule.onNodeWithText(testChannelName).performClick()
        
        // Wait for channel screen to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(testChannelName).fetchSemanticsNodes().size == 1
        }
        
        // Send a message
        composeTestRule.onNodeWithText("메시지 입력...").performTextInput(messageText)
        composeTestRule.onNodeWithContentDescription("메시지 전송").performClick()
        
        // Wait for message to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(messageText).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Get message ID
        runBlocking {
            val messages = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull() ?: emptyList()
            
            val message = messages.find { it.text == messageText }
            assert(message != null) { "전송한 메시지를 찾을 수 없습니다." }
            
            messageId = message?.id ?: ""
        }
        
        // When - Long press on the message and add reaction
        composeTestRule.onNodeWithText(messageText).performLongClick()
        composeTestRule.onNodeWithText("리액션 추가").performClick()
        composeTestRule.onNodeWithText(reaction).performClick()
        
        // Then - Reaction should be added to the message
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(reaction).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify reaction was added in repository
        runBlocking {
            val message = channelRepository.getChannelMessages(testChannelId)
                .first().getOrNull()?.find { it.id == messageId }
            
            val hasReaction = message?.reactions?.get(reaction)?.contains(
                composeTestRule.activity.viewModel.getCurrentUserId()
            ) == true
            
            assert(hasReaction) {
                "리액션이 메시지에 추가되지 않았습니다."
            }
        }
    }
} 