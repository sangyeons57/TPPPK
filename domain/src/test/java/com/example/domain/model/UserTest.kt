package com.example.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * User 클래스 단위 테스트
 *
 * 이 테스트 클래스는 User 도메인 모델의 생성, 속성, 그리고 데이터 클래스로서의 
 * 기능(equals, hashCode, copy, toString)을 검증합니다.
 */
class UserTest {
    
    /**
     * 기본 생성자 테스트
     */
    @Test
    fun `default constructor should create empty user`() {
        // When: 기본 생성자로 User 객체 생성
        val user = User()
        
        // Then: 모든 필드가 기본값으로 초기화되어야 함
        assertEquals("", user.userId)
        assertEquals("", user.name)
        assertEquals("", user.email)
        assertNull(user.profileImageUrl)
        assertNull(user.status)
        assertNull(user.statusMessage)
    }
    
    /**
     * 모든 필드 생성자 테스트
     */
    @Test
    fun `constructor with all fields should initialize user properly`() {
        // Given: 테스트 데이터
        val userId = "test123"
        val name = "홍길동"
        val email = "test@example.com"
        val profileImageUrl = "https://example.com/profile.jpg"
        val status = "online"
        val statusMessage = "열심히 일하는 중"
        
        // When: 모든 필드를 지정하여 User 객체 생성
        val user = User(
            userId = userId,
            name = name,
            email = email,
            profileImageUrl = profileImageUrl,
            status = status,
            statusMessage = statusMessage
        )
        
        // Then: 모든 필드가 주어진 값으로 초기화되어야 함
        assertEquals(userId, user.userId)
        assertEquals(name, user.name)
        assertEquals(email, user.email)
        assertEquals(profileImageUrl, user.profileImageUrl)
        assertEquals(status, user.status)
        assertEquals(statusMessage, user.statusMessage)
    }
    
    /**
     * equals 및 hashCode 테스트
     */
    @Test
    fun `equals and hashCode should work correctly`() {
        // Given: 동일한 데이터를 가진 두 User 객체
        val user1 = User(
            userId = "test123",
            name = "홍길동",
            email = "test@example.com"
        )
        
        val user2 = User(
            userId = "test123",
            name = "홍길동",
            email = "test@example.com"
        )
        
        val differentUser = User(
            userId = "test456",
            name = "김철수",
            email = "other@example.com"
        )
        
        // Then: 동일한 데이터를 가진 객체는 equals와 hashCode가 동일해야 함
        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
        
        // Then: 다른 데이터를 가진 객체는 equals가 false를 반환해야 함
        assertNotEquals(user1, differentUser)
    }
    
    /**
     * copy 메서드 테스트
     */
    @Test
    fun `copy should create new instance with specified changes`() {
        // Given: 원본 User 객체
        val original = User(
            userId = "test123",
            name = "홍길동",
            email = "test@example.com",
            profileImageUrl = "https://example.com/profile.jpg",
            status = "online",
            statusMessage = "열심히 일하는 중"
        )
        
        // When: 일부 필드만 변경하여 복사
        val copied = original.copy(
            name = "새이름",
            status = "offline"
        )
        
        // Then: 지정한 필드만 변경되고 나머지는 유지되어야 함
        assertEquals(original.userId, copied.userId)
        assertEquals("새이름", copied.name)
        assertEquals(original.email, copied.email)
        assertEquals(original.profileImageUrl, copied.profileImageUrl)
        assertEquals("offline", copied.status)
        assertEquals(original.statusMessage, copied.statusMessage)
        
        // Then: 완전히 새로운 객체여야 함
        assertNotSame(original, copied)
    }
    
    /**
     * toString 메서드 테스트
     */
    @Test
    fun `toString should contain all fields`() {
        // Given: 모든 필드가 채워진 User 객체
        val user = User(
            userId = "test123",
            name = "홍길동",
            email = "test@example.com",
            profileImageUrl = "https://example.com/profile.jpg",
            status = "online",
            statusMessage = "열심히 일하는 중"
        )
        
        // When: toString 호출
        val result = user.toString()
        
        // Then: 모든 필드의 이름과 값이 포함되어야 함
        assertTrue(result.contains("userId=test123"))
        assertTrue(result.contains("name=홍길동"))
        assertTrue(result.contains("email=test@example.com"))
        assertTrue(result.contains("profileImageUrl=https://example.com/profile.jpg"))
        assertTrue(result.contains("status=online"))
        assertTrue(result.contains("statusMessage=열심히 일하는 중"))
    }
} 