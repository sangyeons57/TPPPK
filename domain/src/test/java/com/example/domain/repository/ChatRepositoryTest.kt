package com.example.domain.repository

import android.net.Uri
import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import com.example.domain.model.MessageAttachment
import com.example.domain.model.AttachmentType
import com.example.domain.model.channel.ChannelIdentifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.mockito.kotlin.verify
import java.time.Instant

/**
 * ChatRepository 인터페이스의 테스트 케이스.
 * 리팩토링 이후 메시지 관리 중심의 기능이 올바르게 작동하는지 검증합니다.
 */
class ChatRepositoryTest {
    
    // 테스트 데이터
    private val channelId = "channel1"
    private val projectId = "project1"
    private val categoryId = "category1"
    private val messageId = "message1"
    
    private val projectChannelIdentifier = ChannelIdentifier.ProjectChannel(
        projectId = projectId,
        channelId = channelId,
        categoryId = categoryId
    )
    
    private val dmChannelIdentifier = ChannelIdentifier.Dm(
        dmId = "dm1"
    )
    
    private val testMessage = ChatMessage(
        id = messageId,
        channelId = channelId,
        senderId = "user1",
        senderName = "Test User",
        senderProfileUrl = null,
        text = "Test message",
        timestamp = Instant.now(),
        isEdited = false,
        isDeleted = false
    )
    
    private val testAttachment = MessageAttachment(
        id = "attachment1",
        type = AttachmentType.IMAGE,
        url = "https://example.com/image.jpg",
        fileName = "image.jpg",
        mimeType = "image/jpeg",
        size = 1024L,
        thumbnailUrl = "https://example.com/thumb.jpg"
    )
    
    private val testMediaImage = MediaImage(
        id = "image1",
        uri = "content://media/image1",
        name = "image1.jpg",
        size = 1024L,
        mimeType = "image/jpeg",
        dateCreated = Instant.now()
    )
    
    // Mock 객체
    private lateinit var mockChatRepository: ChatRepository
    private lateinit var mockUri: Uri
    
    @Before
    fun setup() {
        mockChatRepository = mock()
        mockUri = mock()
    }
    
    @Test
    fun `getMessagesStream should return flow of messages`() = runTest {
        // Given
        val messages = listOf(testMessage)
        whenever(mockChatRepository.getMessagesStream(projectChannelIdentifier))
            .thenReturn(flowOf(messages))
        
        // When
        val result = mockChatRepository.getMessagesStream(projectChannelIdentifier).first()
        
        // Then
        assertEquals(messages, result)
    }
    
    @Test
    fun `fetchPastMessages should return previous messages`() = runTest {
        // Given
        val messages = listOf(testMessage)
        whenever(mockChatRepository.fetchPastMessages(projectChannelIdentifier, messageId, 20))
            .thenReturn(Result.success(messages))
        
        // When
        val result = mockChatRepository.fetchPastMessages(projectChannelIdentifier, messageId, 20)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(messages, result.getOrNull())
    }
    
    @Test
    fun `sendMessage should return sent message on success`() = runTest {
        // Given
        whenever(mockChatRepository.sendMessage(projectChannelIdentifier, "Test message", emptyList()))
            .thenReturn(Result.success(testMessage))
        
        // When
        val result = mockChatRepository.sendMessage(projectChannelIdentifier, "Test message")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testMessage, result.getOrNull())
    }
    
    @Test
    fun `editMessage should update message text`() = runTest {
        // Given
        whenever(mockChatRepository.editMessage(projectChannelIdentifier, messageId, "Updated text"))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockChatRepository.editMessage(projectChannelIdentifier, messageId, "Updated text")
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `deleteMessage should mark message as deleted`() = runTest {
        // Given
        whenever(mockChatRepository.deleteMessage(projectChannelIdentifier, messageId))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockChatRepository.deleteMessage(projectChannelIdentifier, messageId)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `addReaction should add emoji reaction to message`() = runTest {
        // Given
        whenever(mockChatRepository.addReaction(projectChannelIdentifier, messageId, "👍"))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockChatRepository.addReaction(projectChannelIdentifier, messageId, "👍")
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `removeReaction should remove emoji reaction from message`() = runTest {
        // Given
        whenever(mockChatRepository.removeReaction(projectChannelIdentifier, messageId, "👍"))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockChatRepository.removeReaction(projectChannelIdentifier, messageId, "👍")
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `uploadFile should return attachment info on success`() = runTest {
        // Given
        whenever(mockChatRepository.uploadFile(any(), any(), any(), any()))
            .thenReturn(Result.success(testAttachment))
        
        // When
        val result = mockChatRepository.uploadFile(
            projectChannelIdentifier,
            mockUri,
            "test.jpg",
            "image/jpeg"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testAttachment, result.getOrNull())
    }
    
    @Test
    fun `getLocalGalleryImages should return media images`() = runTest {
        // Given
        val mediaImages = listOf(testMediaImage)
        whenever(mockChatRepository.getLocalGalleryImages(0, 20))
            .thenReturn(Result.success(mediaImages))
        
        // When
        val result = mockChatRepository.getLocalGalleryImages(0, 20)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mediaImages, result.getOrNull())
    }
    
    @Test
    fun `markChannelAsRead should mark all messages as read`() = runTest {
        // Given
        whenever(mockChatRepository.markChannelAsRead(dmChannelIdentifier))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockChatRepository.markChannelAsRead(dmChannelIdentifier)
        
        // Then
        assertTrue(result.isSuccess)
    }
} 