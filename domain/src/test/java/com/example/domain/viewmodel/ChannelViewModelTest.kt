package com.example.domain.viewmodel

import com.example.domain.model.Channel
import com.example.domain.model.ChatMessage
import com.example.domain.repository.FakeChannelRepository
import com.example.domain.usecase.channel.GetProjectChannelUseCase
import com.example.domain.usecase.channel.MarkChannelAsReadUseCase
import com.example.domain.usecase.chat.AddReactionUseCase
import com.example.domain.usecase.chat.DeleteMessageUseCase
import com.example.domain.usecase.chat.EditMessageUseCase
import com.example.domain.usecase.chat.FetchPastMessagesUseCase
import com.example.domain.usecase.chat.GetMessagesStreamUseCase
import com.example.domain.usecase.chat.RemoveReactionUseCase
import com.example.domain.usecase.chat.SendMessageUseCase
import com.example.domain.usecase.user.GetCurrentUserIdUseCase
import com.example.feature_chat.viewmodel.ChannelState
import com.example.feature_chat.viewmodel.ChannelViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * ChannelViewModel에 대한 단위 테스트입니다.
 */
@ExperimentalCoroutinesApi
class ChannelViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var channelRepository: FakeChannelRepository
    
    // UseCase mocks
    private lateinit var getProjectChannelUseCase: GetProjectChannelUseCase
    private lateinit var getMessagesStreamUseCase: GetMessagesStreamUseCase
    private lateinit var fetchPastMessagesUseCase: FetchPastMessagesUseCase
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var editMessageUseCase: EditMessageUseCase
    private lateinit var deleteMessageUseCase: DeleteMessageUseCase
    private lateinit var markChannelAsReadUseCase: MarkChannelAsReadUseCase
    private lateinit var addReactionUseCase: AddReactionUseCase
    private lateinit var removeReactionUseCase: RemoveReactionUseCase
    private lateinit var getCurrentUserIdUseCase: GetCurrentUserIdUseCase
    
    // ViewModel
    private lateinit var viewModel: ChannelViewModel
    
    // Test data
    private val testChannelId = "test_channel_id"
    private val testUserId = "test_user_id"
    private val testChannel = Channel(
        id = testChannelId,
        name = "Test Channel",
        description = "Test description",
        ownerId = testUserId,
        participantIds = listOf(testUserId, "other_user_id"),
        lastMessagePreview = null,
        lastMessageTimestamp = null,
        metadata = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    private val testMessages = listOf(
        ChatMessage(
            id = "message1",
            channelId = testChannelId,
            senderId = testUserId,
            senderName = "Test User",
            senderProfileUrl = null,
            text = "Test message 1",
            timestamp = LocalDateTime.now().minusMinutes(5),
            reactions = emptyMap(),
            attachments = emptyList(),
            replyToMessageId = null,
            isEdited = false,
            isDeleted = false
        ),
        ChatMessage(
            id = "message2",
            channelId = testChannelId,
            senderId = "other_user_id",
            senderName = "Other User",
            senderProfileUrl = null,
            text = "Test message 2",
            timestamp = LocalDateTime.now(),
            reactions = emptyMap(),
            attachments = emptyList(),
            replyToMessageId = null,
            isEdited = false,
            isDeleted = false
        )
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        channelRepository = FakeChannelRepository(testDispatcher)
        
        // Set up mocks
        getProjectChannelUseCase = mock()
        getMessagesStreamUseCase = mock()
        fetchPastMessagesUseCase = mock()
        sendMessageUseCase = mock()
        editMessageUseCase = mock()
        deleteMessageUseCase = mock()
        markChannelAsReadUseCase = mock()
        addReactionUseCase = mock()
        removeReactionUseCase = mock()
        getCurrentUserIdUseCase = mock()
        
        // Default mock responses
        whenever(getCurrentUserIdUseCase()).thenReturn(testUserId)
        whenever(getProjectChannelUseCase(testChannelId)).thenReturn(flowOf(Result.success(testChannel)))
        whenever(getMessagesStreamUseCase(testChannelId)).thenReturn(flowOf(Result.success(testMessages)))
        
        // Initialize ViewModel
        viewModel = ChannelViewModel(
            getChannelUseCase = getProjectChannelUseCase,
            getMessagesStreamUseCase = getMessagesStreamUseCase,
            fetchPastMessagesUseCase = fetchPastMessagesUseCase,
            sendMessageUseCase = sendMessageUseCase,
            editMessageUseCase = editMessageUseCase,
            deleteMessageUseCase = deleteMessageUseCase,
            markChannelAsReadUseCase = markChannelAsReadUseCase,
            addReactionUseCase = addReactionUseCase,
            removeReactionUseCase = removeReactionUseCase,
            getCurrentUserIdUseCase = getCurrentUserIdUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadChannel loads channel and messages successfully`() = runTest {
        // When
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is ChannelState.Loaded)
        
        if (uiState is ChannelState.Loaded) {
            assertEquals(testChannel, uiState.channel)
            assertEquals(testMessages, uiState.messages)
            assertEquals(testUserId, uiState.currentUserId)
        }
        
        // Verify usecase calls
        verify(getProjectChannelUseCase).invoke(testChannelId)
        verify(getMessagesStreamUseCase).invoke(testChannelId)
    }
    
    @Test
    fun `sendMessage calls sendMessageUseCase with correct parameters`() = runTest {
        // Given
        val messageText = "New test message"
        whenever(sendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(
            flowOf(Result.success(Unit))
        )
        
        // Set up the ViewModel
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateMessageText(messageText)
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(sendMessageUseCase).invoke(
            channelId = testChannelId,
            senderId = testUserId,
            message = messageText,
            replyToMessageId = null
        )
        
        // Verify message input cleared
        assertEquals("", viewModel.messageInput.value.text)
    }
    
    @Test
    fun `startEditMessage sets message input correctly`() = runTest {
        // Given
        val messageId = "message1"
        val messageText = "Test message 1"
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.startEditMessage(messageId)
        
        // Then
        val messageInput = viewModel.messageInput.value
        assertEquals(messageText, messageInput.text)
        assertEquals(messageId, messageInput.editingMessageId)
        assertTrue(messageInput.isEditing)
    }
    
    @Test
    fun `editMessage calls editMessageUseCase with correct parameters`() = runTest {
        // Given
        val messageId = "message1"
        val originalText = "Test message 1"
        val newText = "Edited message"
        
        whenever(editMessageUseCase(any(), any(), any())).thenReturn(
            flowOf(Result.success(Unit))
        )
        
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Start editing
        viewModel.startEditMessage(messageId)
        assertEquals(originalText, viewModel.messageInput.value.text)
        
        // When
        viewModel.updateMessageText(newText)
        viewModel.sendMessage() // This should call editMessage since we're in edit mode
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(editMessageUseCase).invoke(
            channelId = testChannelId,
            messageId = messageId,
            newText = newText
        )
        
        // Verify message input cleared and edit mode reset
        assertEquals("", viewModel.messageInput.value.text)
        assertEquals(null, viewModel.messageInput.value.editingMessageId)
        assertEquals(false, viewModel.messageInput.value.isEditing)
    }
    
    @Test
    fun `deleteMessage calls deleteMessageUseCase with correct parameters`() = runTest {
        // Given
        val messageId = "message1"
        whenever(deleteMessageUseCase(any(), any())).thenReturn(
            flowOf(Result.success(Unit))
        )
        
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.deleteMessage(messageId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(deleteMessageUseCase).invoke(
            channelId = testChannelId,
            messageId = messageId
        )
    }
    
    @Test
    fun `toggleReaction adds reaction when not already added`() = runTest {
        // Given
        val messageId = "message1"
        val reaction = "👍"
        
        whenever(addReactionUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(Result.success(Unit))
        )
        
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.toggleReaction(messageId, reaction)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(addReactionUseCase).invoke(
            channelId = testChannelId,
            messageId = messageId,
            userId = testUserId,
            reaction = reaction
        )
    }
    
    @Test
    fun `toggleReaction removes reaction when already added`() = runTest {
        // Given
        val messageId = "message1"
        val reaction = "👍"
        
        // Create a message with existing reaction
        val messageWithReaction = testMessages[0].copy(
            reactions = mapOf(reaction to listOf(testUserId))
        )
        
        // Override getMessage flow to return message with reaction
        val updatedMessages = listOf(messageWithReaction, testMessages[1])
        whenever(getMessagesStreamUseCase(testChannelId)).thenReturn(
            flowOf(Result.success(updatedMessages))
        )
        
        whenever(removeReactionUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(Result.success(Unit))
        )
        
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.toggleReaction(messageId, reaction)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(removeReactionUseCase).invoke(
            channelId = testChannelId,
            messageId = messageId,
            userId = testUserId,
            reaction = reaction
        )
    }
    
    @Test
    fun `markAsRead calls markChannelAsReadUseCase with correct parameters`() = runTest {
        // Given
        whenever(markChannelAsReadUseCase(any(), any())).thenReturn(
            flowOf(Result.success(Unit))
        )
        
        viewModel.loadChannel(testChannelId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.markAsRead()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(markChannelAsReadUseCase).invoke(
            channelId = testChannelId,
            userId = testUserId
        )
    }
} 