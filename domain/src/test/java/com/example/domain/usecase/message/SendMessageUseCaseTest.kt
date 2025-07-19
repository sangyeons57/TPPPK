package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.repository.base.MessageRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SendMessageUseCaseTest {

    private lateinit var mockMessageRepository: MessageRepository
    private lateinit var sendMessageUseCase: SendMessageUseCase

    @Before
    fun setup() {
        mockMessageRepository = mockk()
        sendMessageUseCase = SendMessageUseCase(mockMessageRepository)
    }

    @Test
    fun `test successful message send returns success with message`() = runTest {
        // Given
        val senderId = UserId("user123")
        val content = MessageContent("Hello, World!")
        val messageId = DocumentId("msg456")
        
        // Mock the repository to return success
        coEvery { mockMessageRepository.save(any()) } returns CustomResult.Success(messageId)

        // When
        val result = sendMessageUseCase(senderId, content)

        // Then
        assertTrue(result is CustomResult.Success)
        val message = (result as CustomResult.Success).data
        assertEquals(senderId, message.senderId)
        assertEquals(content, message.content)
        assertNull(message.replyToMessageId)
        assertTrue(message.isNew)
        
        // Verify repository was called
        coVerify { mockMessageRepository.save(any()) }
    }

    @Test
    fun `test message send with reply creates reply relationship`() = runTest {
        // Given
        val senderId = UserId("user123")
        val content = MessageContent("This is a reply")
        val replyToMessageId = DocumentId("original_msg")
        val messageId = DocumentId("reply_msg")
        
        coEvery { mockMessageRepository.save(any()) } returns CustomResult.Success(messageId)

        // When
        val result = sendMessageUseCase(senderId, content, replyToMessageId)

        // Then
        assertTrue(result is CustomResult.Success)
        val message = (result as CustomResult.Success).data
        assertEquals(replyToMessageId, message.replyToMessageId)
        
        coVerify { 
            mockMessageRepository.save(
                match<Message> { it.replyToMessageId == replyToMessageId }
            ) 
        }
    }

    @Test
    fun `test repository failure returns failure result`() = runTest {
        // Given
        val senderId = UserId("user123")
        val content = MessageContent("Hello, World!")
        val exception = Exception("Database error")
        
        coEvery { mockMessageRepository.save(any()) } returns CustomResult.Failure(exception)

        // When
        val result = sendMessageUseCase(senderId, content)

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals(exception, (result as CustomResult.Failure).error)
    }

    @Test
    fun `test unexpected exception is caught and returned as failure`() = runTest {
        // Given
        val senderId = UserId("user123")
        val content = MessageContent("Hello, World!")
        val exception = RuntimeException("Unexpected error")
        
        coEvery { mockMessageRepository.save(any()) } throws exception

        // When
        val result = sendMessageUseCase(senderId, content)

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals(exception, (result as CustomResult.Failure).error)
    }

    @Test
    fun `test message properties are correctly set`() = runTest {
        // Given
        val senderId = UserId("user123")
        val content = MessageContent("Test message")
        val messageId = DocumentId("msg456")
        
        val capturedMessage = slot<Message>()
        coEvery { mockMessageRepository.save(capture(capturedMessage)) } returns CustomResult.Success(messageId)

        // When
        sendMessageUseCase(senderId, content)

        // Then
        val message = capturedMessage.captured
        assertEquals(senderId, message.senderId)
        assertEquals(content, message.content)
        assertTrue(message.isNew)
        assertNotNull(message.createdAt)
        assertNotNull(message.updatedAt)
        assertEquals(message.createdAt, message.updatedAt) // New message has same created/updated time
    }
}