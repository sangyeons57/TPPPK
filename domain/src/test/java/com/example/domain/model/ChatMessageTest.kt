package com.example.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/**
 * ChatMessage 클래스 단위 테스트
 *
 * 이 테스트 클래스는 ChatMessage 도메인 모델의 생성, 속성, 그리고 데이터 클래스로서의 
 * 기능(equals, hashCode, copy, toString)을 검증합니다.
 */
class ChatMessageTest {
    
    /**
     * 기본 생성자 테스트
     */
    @Test
    fun `constructor with required fields should initialize message properly`() {
        // Given: 필수 필드 데이터
        val chatId = 12345
        val channelId = "channel789"
        val userId = 101
        val userName = "홍길동"
        val userProfileUrl = "https://example.com/profile.jpg"
        val message = "안녕하세요!"
        val sentAt = LocalDateTime.of(2023, 10, 15, 14, 30)
        val isModified = false
        
        // When: 기본 필드로 ChatMessage 객체 생성
        val chatMessage = ChatMessage(
            chatId = chatId,
            channelId = channelId,
            userId = userId,
            userName = userName,
            userProfileUrl = userProfileUrl,
            message = message,
            sentAt = sentAt,
            isModified = isModified
        )
        
        // Then: 필수 필드가 주어진 값으로 초기화되어야 함
        assertEquals(chatId, chatMessage.chatId)
        assertEquals(channelId, chatMessage.channelId)
        assertEquals(userId, chatMessage.userId)
        assertEquals(userName, chatMessage.userName)
        assertEquals(userProfileUrl, chatMessage.userProfileUrl)
        assertEquals(message, chatMessage.message)
        assertEquals(sentAt, chatMessage.sentAt)
        assertEquals(isModified, chatMessage.isModified)
        
        // 선택적 필드는 기본값으로 초기화되어야 함
        assertEquals(emptyList<String>(), chatMessage.attachmentImageUrls)
    }
    
    /**
     * 모든 필드 생성자 테스트
     */
    @Test
    fun `constructor with all fields should initialize message properly`() {
        // Given: 모든 필드 데이터
        val chatId = 12345
        val channelId = "channel789"
        val userId = 101
        val userName = "홍길동"
        val userProfileUrl = "https://example.com/profile.jpg"
        val message = "안녕하세요!"
        val sentAt = LocalDateTime.of(2023, 10, 15, 14, 30)
        val isModified = true
        val attachmentImageUrls = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        )
        
        // When: 모든 필드를 지정하여 ChatMessage 객체 생성
        val chatMessage = ChatMessage(
            chatId = chatId,
            channelId = channelId,
            userId = userId,
            userName = userName,
            userProfileUrl = userProfileUrl,
            message = message,
            sentAt = sentAt,
            isModified = isModified,
            attachmentImageUrls = attachmentImageUrls
        )
        
        // Then: 모든 필드가 주어진 값으로 초기화되어야 함
        assertEquals(chatId, chatMessage.chatId)
        assertEquals(channelId, chatMessage.channelId)
        assertEquals(userId, chatMessage.userId)
        assertEquals(userName, chatMessage.userName)
        assertEquals(userProfileUrl, chatMessage.userProfileUrl)
        assertEquals(message, chatMessage.message)
        assertEquals(sentAt, chatMessage.sentAt)
        assertEquals(isModified, chatMessage.isModified)
        assertEquals(attachmentImageUrls, chatMessage.attachmentImageUrls)
    }
    
    /**
     * userProfileUrl 필드가 null인 경우 테스트
     */
    @Test
    fun `message with null profile url should be created correctly`() {
        // Given: userProfileUrl이 null인 데이터
        val chatId = 12345
        val channelId = "channel789"
        val userId = 101
        val userName = "프로필 없는 사용자"
        val message = "안녕하세요!"
        val sentAt = LocalDateTime.of(2023, 10, 15, 14, 30)
        val isModified = false
        
        // When: userProfileUrl을 null로 지정하여 ChatMessage 객체 생성
        val chatMessage = ChatMessage(
            chatId = chatId,
            channelId = channelId,
            userId = userId,
            userName = userName,
            userProfileUrl = null,
            message = message,
            sentAt = sentAt,
            isModified = isModified
        )
        
        // Then: userProfileUrl이 null이어야 함
        assertNull(chatMessage.userProfileUrl)
    }
    
    /**
     * equals 및 hashCode 테스트
     */
    @Test
    fun `equals and hashCode should work correctly`() {
        // Given: 동일한 데이터를 가진 두 ChatMessage 객체
        val sentAt = LocalDateTime.of(2023, 10, 15, 14, 30)
        
        val chatMessage1 = ChatMessage(
            chatId = 12345,
            channelId = "channel789",
            userId = 101,
            userName = "홍길동",
            userProfileUrl = "https://example.com/profile.jpg",
            message = "안녕하세요!",
            sentAt = sentAt,
            isModified = false
        )
        
        val chatMessage2 = ChatMessage(
            chatId = 12345,
            channelId = "channel789",
            userId = 101,
            userName = "홍길동",
            userProfileUrl = "https://example.com/profile.jpg",
            message = "안녕하세요!",
            sentAt = sentAt,
            isModified = false
        )
        
        val differentChatMessage = ChatMessage(
            chatId = 54321,
            channelId = "channel789",
            userId = 101,
            userName = "홍길동",
            userProfileUrl = "https://example.com/profile.jpg",
            message = "다른 메시지입니다",
            sentAt = sentAt,
            isModified = true
        )
        
        // Then: 동일한 데이터를 가진 객체는 equals와 hashCode가 동일해야 함
        assertEquals(chatMessage1, chatMessage2)
        assertEquals(chatMessage1.hashCode(), chatMessage2.hashCode())
        
        // Then: 다른 데이터를 가진 객체는 equals가 false를 반환해야 함
        assertNotEquals(chatMessage1, differentChatMessage)
    }
    
    /**
     * copy 메서드 테스트
     */
    @Test
    fun `copy should create new instance with specified changes`() {
        // Given: 원본 ChatMessage 객체
        val sentAt = LocalDateTime.of(2023, 10, 15, 14, 30)
        
        val original = ChatMessage(
            chatId = 12345,
            channelId = "channel789",
            userId = 101,
            userName = "홍길동",
            userProfileUrl = "https://example.com/profile.jpg",
            message = "원본 메시지",
            sentAt = sentAt,
            isModified = false
        )
        
        // When: 일부 필드만 변경하여 복사
        val updatedMessage = "수정된 메시지"
        val copied = original.copy(
            message = updatedMessage,
            isModified = true
        )
        
        // Then: 지정한 필드만 변경되고 나머지는 유지되어야 함
        assertEquals(original.chatId, copied.chatId)
        assertEquals(original.channelId, copied.channelId)
        assertEquals(original.userId, copied.userId)
        assertEquals(original.userName, copied.userName)
        assertEquals(original.userProfileUrl, copied.userProfileUrl)
        assertEquals(updatedMessage, copied.message)
        assertEquals(original.sentAt, copied.sentAt)
        assertTrue(copied.isModified)
        
        // Then: 완전히 새로운 객체여야 함
        assertNotSame(original, copied)
    }
    
    /**
     * toString 메서드 테스트
     */
    @Test
    fun `toString should contain all fields`() {
        // Given: 모든 필드가 채워진 ChatMessage 객체
        val sentAt = LocalDateTime.of(2023, 10, 15, 14, 30)
        val attachmentImageUrls = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        )
        
        val chatMessage = ChatMessage(
            chatId = 12345,
            channelId = "channel789",
            userId = 101,
            userName = "홍길동",
            userProfileUrl = "https://example.com/profile.jpg",
            message = "안녕하세요!",
            sentAt = sentAt,
            isModified = true,
            attachmentImageUrls = attachmentImageUrls
        )
        
        // When: toString 호출
        val result = chatMessage.toString()
        
        // Then: 모든 필드의 이름과 값이 포함되어야 함
        assertTrue(result.contains("chatId=12345"))
        assertTrue(result.contains("channelId=channel789"))
        assertTrue(result.contains("userId=101"))
        assertTrue(result.contains("userName=홍길동"))
        assertTrue(result.contains("userProfileUrl=https://example.com/profile.jpg"))
        assertTrue(result.contains("message=안녕하세요!"))
        assertTrue(result.contains("sentAt=$sentAt"))
        assertTrue(result.contains("isModified=true"))
        assertTrue(result.contains("attachmentImageUrls=[https://example.com/image1.jpg, https://example.com/image2.jpg]"))
    }
} 