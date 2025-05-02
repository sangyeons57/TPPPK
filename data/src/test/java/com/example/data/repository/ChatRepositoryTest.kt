package com.example.data.repository

import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * ChatRepository 기능 테스트
 *
 * 이 테스트는 FakeChatRepository를 사용하여 ChatRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class ChatRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var chatRepository: FakeChatRepository
    
    // 테스트 데이터
    private val testChannelId = "test-channel-123"
    private val testMessageId = 1001
    private val testMessage = ChatMessage(
        chatId = testMessageId,
        channelId = testChannelId,
        userId = 1,
        userName = "테스트 사용자",
        userProfileUrl = null,
        message = "테스트 메시지입니다.",
        sentAt = LocalDateTime.of(2023, 10, 15, 14, 30),
        isModified = false
    )
    
    // 추가 테스트 메시지
    private val additionalMessages = listOf(
        ChatMessage(
            chatId = 1002,
            channelId = testChannelId,
            userId = 2,
            userName = "다른 사용자",
            userProfileUrl = "https://example.com/profile.jpg",
            message = "안녕하세요!",
            sentAt = LocalDateTime.of(2023, 10, 15, 14, 35),
            isModified = false
        ),
        ChatMessage(
            chatId = 1003,
            channelId = testChannelId,
            userId = 1,
            userName = "테스트 사용자",
            userProfileUrl = null,
            message = "두 번째 메시지입니다.",
            sentAt = LocalDateTime.of(2023, 10, 15, 14, 40),
            isModified = false
        )
    )
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeChatRepository 초기화
        chatRepository = FakeChatRepository()
        
        // 테스트 메시지 추가
        chatRepository.addMessage(testChannelId, testMessage)
    }
    
    /**
     * 메시지 스트림 테스트
     */
    @Test
    fun `getMessagesStream should emit messages for channel`() = runBlocking {
        // When: 메시지 스트림 가져오기
        val messages = chatRepository.getMessagesStream(testChannelId).first()
        
        // Then: 올바른 메시지 목록 확인
        assertEquals(1, messages.size)
        assertEquals(testMessage, messages.first())
    }
    
    /**
     * 빈 채널의 메시지 스트림 테스트
     */
    @Test
    fun `getMessagesStream should emit empty list for empty channel`() = runBlocking {
        // Given: 비어있는 채널
        val emptyChannelId = "empty-channel"
        
        // When: 비어있는 채널의 메시지 스트림 가져오기
        val messages = chatRepository.getMessagesStream(emptyChannelId).first()
        
        // Then: 빈 목록 확인
        assertTrue(messages.isEmpty())
    }
    
    /**
     * 과거 메시지 조회
     */
    @Test
    fun `fetchPastMessages should return messages before given ID`() = runBlocking {
        // Given: 추가 메시지 설정
        chatRepository.addMessages(testChannelId, additionalMessages)
        
        // When: 특정 ID 이전의 메시지 가져오기
        val result = chatRepository.fetchPastMessages(testChannelId, beforeMessageId = 1003, limit = 10)
        
        // Then: 성공 및 올바른 메시지 목록 반환
        assertTrue(result.isSuccess)
        val pastMessages = result.getOrNull()
        assertNotNull(pastMessages)
        assertEquals(2, pastMessages?.size)
        
        // ID가 1003보다 작은 메시지들만 반환되어야 함
        pastMessages?.forEach { message ->
            assertTrue(message.chatId < 1003)
        }
    }
    
    /**
     * 제한된 수의 과거 메시지 조회 테스트
     */
    @Test
    fun `fetchPastMessages should respect limit parameter`() = runBlocking {
        // Given: 여러 메시지 추가
        val manyMessages = (1..10).map { id ->
            ChatMessage(
                chatId = id,
                channelId = testChannelId,
                userId = 1,
                userName = "테스트 사용자",
                userProfileUrl = null,
                message = "메시지 $id",
                sentAt = LocalDateTime.now().minusMinutes(id.toLong()),
                isModified = false
            )
        }
        
        chatRepository.clearChannelMessages(testChannelId)
        chatRepository.addMessages(testChannelId, manyMessages)
        
        // When: 제한된 수의 메시지 가져오기
        val result = chatRepository.fetchPastMessages(testChannelId, beforeMessageId = 11, limit = 5)
        
        // Then: 성공 및 지정된 limit만큼의 메시지 반환
        assertTrue(result.isSuccess)
        val limitedMessages = result.getOrNull()
        assertNotNull(limitedMessages)
        assertEquals(5, limitedMessages?.size)
    }
    
    /**
     * 메시지 전송 테스트
     */
    @Test
    fun `sendMessage should add new message and return it`() = runBlocking {
        // Given: 전송할 메시지 데이터
        val messageText = "새로운 메시지"
        val testUri = TestUri("file://test/image.jpg")
        
        // When: 메시지 전송 (테스트용 메서드 사용)
        val result = chatRepository.sendMessageTest(testChannelId, messageText, listOf(testUri))
        
        // Then: 성공 및 생성된 메시지 반환
        assertTrue(result.isSuccess)
        val sentMessage = result.getOrNull()
        assertNotNull(sentMessage)
        assertEquals(messageText, sentMessage?.message)
        assertEquals(1, sentMessage?.attachmentImageUrls?.size)
        
        // 스트림에 새 메시지가 추가되었는지 확인
        val updatedMessages = chatRepository.getMessagesStream(testChannelId).first()
        assertEquals(2, updatedMessages.size) // 기존 메시지 + 새 메시지
    }
    
    /**
     * 빈 메시지 전송 실패 테스트
     */
    @Test
    fun `sendMessage should fail with empty message and no attachments`() = runBlocking {
        // Given: 빈 메시지와 빈 첨부파일
        val emptyMessage = "   "
        
        // When: 메시지 전송 (테스트용 메서드 사용)
        val result = chatRepository.sendMessageTest(testChannelId, emptyMessage, emptyList<TestUri>())
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
    }
    
    /**
     * 메시지 수정 테스트
     */
    @Test
    fun `editMessage should update existing message content`() = runBlocking {
        // Given: 수정할 새 메시지 내용
        val newContent = "수정된 메시지 내용"
        
        // When: 메시지 수정
        val result = chatRepository.editMessage(testChannelId, testMessageId, newContent)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 메시지가 실제로 수정되었는지 확인
        val updatedMessages = chatRepository.getMessagesStream(testChannelId).first()
        val updatedMessage = updatedMessages.find { it.chatId == testMessageId }
        assertNotNull(updatedMessage)
        assertEquals(newContent, updatedMessage?.message)
        assertTrue(updatedMessage?.isModified == true)
    }
    
    /**
     * 존재하지 않는 메시지 수정 실패 테스트
     */
    @Test
    fun `editMessage should fail for non-existent message`() = runBlocking {
        // When: 존재하지 않는 메시지 수정
        val result = chatRepository.editMessage(testChannelId, 9999, "존재하지 않는 메시지")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 메시지 삭제 테스트
     */
    @Test
    fun `deleteMessage should remove message from channel`() = runBlocking {
        // When: 메시지 삭제
        val result = chatRepository.deleteMessage(testChannelId, testMessageId)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 메시지가 실제로 삭제되었는지 확인
        val remainingMessages = chatRepository.getMessagesStream(testChannelId).first()
        assertTrue(remainingMessages.isEmpty())
    }
    
    /**
     * 존재하지 않는 메시지 삭제 실패 테스트
     */
    @Test
    fun `deleteMessage should fail for non-existent message`() = runBlocking {
        // When: 존재하지 않는 메시지 삭제
        val result = chatRepository.deleteMessage(testChannelId, 9999)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 갤러리 이미지 조회 테스트
     */
    @Test
    fun `getLocalGalleryImages should return paginated images`() = runBlocking {
        // Given: 테스트 갤러리 이미지
        val galleryImages = (1..10).map { id ->
            MediaImage(
                id = id.toLong(),
                contentUri = TestUri("file://gallery/image$id.jpg").toString(),
                displayName = "Image $id",
                size = id * 1024L
            )
        }
        chatRepository.setGalleryImages(galleryImages)
        
        // When: 첫 페이지 조회 (5개)
        val result = chatRepository.getLocalGalleryImages(page = 0, pageSize = 5)
        
        // Then: 성공 및 지정된 개수만큼 이미지 반환
        assertTrue(result.isSuccess)
        val images = result.getOrNull()
        assertNotNull(images)
        assertEquals(5, images?.size)
    }
    
    /**
     * 갤러리 이미지 다음 페이지 조회 테스트
     */
    @Test
    fun `getLocalGalleryImages should return next page correctly`() = runBlocking {
        // Given: 테스트 갤러리 이미지
        val galleryImages = (1..10).map { id ->
            MediaImage(
                id = id.toLong(),
                contentUri = TestUri("file://gallery/image$id.jpg").toString(),
                displayName = "Image $id",
                size = id * 1024L
            )
        }
        chatRepository.setGalleryImages(galleryImages)
        
        // When: 다음 페이지 조회 (5개)
        val result = chatRepository.getLocalGalleryImages(page = 1, pageSize = 5)
        
        // Then: 성공 및 페이지의 이미지 반환
        assertTrue(result.isSuccess)
        val images = result.getOrNull()
        assertNotNull(images)
        assertEquals(5, images?.size)
        
        // 두 번째 페이지이므로 id가 6-10인 이미지들이 와야 함
        images?.forEachIndexed { index, image ->
            assertEquals((index + 6).toLong(), image.id)
        }
    }
    
    /**
     * 더 이상 데이터가 없는 페이지 조회 테스트
     */
    @Test
    fun `getLocalGalleryImages should return empty list for out of range page`() = runBlocking {
        // Given: 테스트 갤러리 이미지
        val galleryImages = (1..5).map { id ->
            MediaImage(
                id = id.toLong(),
                contentUri = TestUri("file://gallery/image$id.jpg").toString(),
                displayName = "Image $id",
                size = id * 1024L
            )
        }
        chatRepository.setGalleryImages(galleryImages)
        
        // When: 범위를 벗어난 페이지 조회
        val result = chatRepository.getLocalGalleryImages(page = 2, pageSize = 5)
        
        // Then: 성공 및 빈 목록 반환
        assertTrue(result.isSuccess)
        val images = result.getOrNull()
        assertNotNull(images)
        assertTrue(images?.isEmpty() == true)
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("Test error")
        chatRepository.setShouldSimulateError(true, testError)
        
        // When: 작업 수행
        val result = chatRepository.fetchPastMessages(testChannelId, 1000, 10)
        
        // Then: 실패 및 시뮬레이션된 에러 반환
        assertTrue(result.isFailure)
        assertEquals(testError, result.exceptionOrNull())
    }
} 