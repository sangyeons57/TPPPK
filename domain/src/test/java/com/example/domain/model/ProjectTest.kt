package com.example.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Project 클래스 단위 테스트
 *
 * 이 테스트 클래스는 Project 도메인 모델의 생성, 속성, 그리고 데이터 클래스로서의 
 * 기능(equals, hashCode, copy, toString)을 검증합니다.
 */
class ProjectTest {
    
    /**
     * 필수 필드 생성자 테스트
     */
    @Test
    fun `constructor with required fields should initialize project properly`() {
        // Given: 필수 필드 데이터
        val id = "project123"
        val name = "팀 프로젝트"
        
        // When: 필수 필드로만 Project 객체 생성
        val project = Project(
            id = id,
            name = name,
            description = null,
            imageUrl = null
        )
        
        // Then: 필수 필드가 주어진 값으로 초기화되어야 함
        assertEquals(id, project.id)
        assertEquals(name, project.name)
        assertNull(project.description)
        assertNull(project.imageUrl)
        
        // 선택적 필드는 기본값으로 초기화되어야 함
        assertNull(project.memberCount)
        assertFalse(project.isPublic)
    }
    
    /**
     * 모든 필드 생성자 테스트
     */
    @Test
    fun `constructor with all fields should initialize project properly`() {
        // Given: 모든 필드 데이터
        val id = "project123"
        val name = "팀 프로젝트"
        val description = "협업을 위한 프로젝트입니다"
        val imageUrl = "https://example.com/project.jpg"
        val memberCount = 5
        val isPublic = true
        
        // When: 모든 필드를 지정하여 Project 객체 생성
        val project = Project(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            memberCount = memberCount,
            isPublic = isPublic
        )
        
        // Then: 모든 필드가 주어진 값으로 초기화되어야 함
        assertEquals(id, project.id)
        assertEquals(name, project.name)
        assertEquals(description, project.description)
        assertEquals(imageUrl, project.imageUrl)
        assertEquals(memberCount, project.memberCount)
        assertEquals(isPublic, project.isPublic)
    }
    
    /**
     * equals 및 hashCode 테스트
     */
    @Test
    fun `equals and hashCode should work correctly`() {
        // Given: 동일한 데이터를 가진 두 Project 객체
        val project1 = Project(
            id = "project123",
            name = "팀 프로젝트",
            description = "협업을 위한 프로젝트입니다",
            imageUrl = "https://example.com/project.jpg"
        )
        
        val project2 = Project(
            id = "project123",
            name = "팀 프로젝트",
            description = "협업을 위한 프로젝트입니다",
            imageUrl = "https://example.com/project.jpg"
        )
        
        val differentProject = Project(
            id = "project456",
            name = "다른 프로젝트",
            description = "다른 설명",
            imageUrl = null
        )
        
        // Then: 동일한 데이터를 가진 객체는 equals와 hashCode가 동일해야 함
        assertEquals(project1, project2)
        assertEquals(project1.hashCode(), project2.hashCode())
        
        // Then: 다른 데이터를 가진 객체는 equals가 false를 반환해야 함
        assertNotEquals(project1, differentProject)
    }
    
    /**
     * copy 메서드 테스트
     */
    @Test
    fun `copy should create new instance with specified changes`() {
        // Given: 원본 Project 객체
        val original = Project(
            id = "project123",
            name = "팀 프로젝트",
            description = "협업을 위한 프로젝트입니다",
            imageUrl = "https://example.com/project.jpg",
            memberCount = 5,
            isPublic = false
        )
        
        // When: 일부 필드만 변경하여 복사
        val copied = original.copy(
            name = "업데이트된 프로젝트 이름",
            memberCount = 8,
            isPublic = true
        )
        
        // Then: 지정한 필드만 변경되고 나머지는 유지되어야 함
        assertEquals(original.id, copied.id)
        assertEquals("업데이트된 프로젝트 이름", copied.name)
        assertEquals(original.description, copied.description)
        assertEquals(original.imageUrl, copied.imageUrl)
        assertEquals(8, copied.memberCount)
        assertTrue(copied.isPublic)
        
        // Then: 완전히 새로운 객체여야 함
        assertNotSame(original, copied)
    }
    
    /**
     * toString 메서드 테스트
     */
    @Test
    fun `toString should contain all fields`() {
        // Given: 모든 필드가 채워진 Project 객체
        val project = Project(
            id = "project123",
            name = "팀 프로젝트",
            description = "협업을 위한 프로젝트입니다",
            imageUrl = "https://example.com/project.jpg",
            memberCount = 5,
            isPublic = true
        )
        
        // When: toString 호출
        val result = project.toString()
        
        // Then: 모든 필드의 이름과 값이 포함되어야 함
        assertTrue(result.contains("id=project123"))
        assertTrue(result.contains("name=팀 프로젝트"))
        assertTrue(result.contains("description=협업을 위한 프로젝트입니다"))
        assertTrue(result.contains("imageUrl=https://example.com/project.jpg"))
        assertTrue(result.contains("memberCount=5"))
        assertTrue(result.contains("isPublic=true"))
    }
} 