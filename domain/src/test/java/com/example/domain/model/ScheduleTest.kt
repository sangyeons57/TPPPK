package com.example.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/**
 * Schedule 클래스 단위 테스트
 *
 * 이 테스트 클래스는 Schedule 도메인 모델의 생성, 속성, 그리고 데이터 클래스로서의 
 * 기능(equals, hashCode, copy, toString)을 검증합니다.
 */
class ScheduleTest {

    /**
     * 기본 생성자 테스트
     */
    @Test
    fun `constructor should initialize schedule with required fields`() {
        // Given: 필수 필드 데이터
        val id = "schedule123"
        val projectId = "project456"
        val title = "디자인 회의"
        val content = "UI 디자인 논의"
        val startTime = LocalDateTime.of(2023, 10, 15, 14, 0)
        val endTime = LocalDateTime.of(2023, 10, 15, 15, 30)
        
        // When: 필수 필드로만 Schedule 객체 생성
        val schedule = Schedule(
            id = id,
            projectId = projectId,
            title = title,
            content = content,
            startTime = startTime,
            endTime = endTime
        )
        
        // Then: 필수 필드가 주어진 값으로 초기화되어야 함
        assertEquals(id, schedule.id)
        assertEquals(projectId, schedule.projectId)
        assertEquals(title, schedule.title)
        assertEquals(content, schedule.content)
        assertEquals(startTime, schedule.startTime)
        assertEquals(endTime, schedule.endTime)
        
        // 선택적 필드는 기본값으로 초기화되어야 함
        assertEquals(emptyList<String>(), schedule.participants)
        assertFalse(schedule.isAllDay)
    }
    
    /**
     * 모든 필드 생성자 테스트
     */
    @Test
    fun `constructor with all fields should initialize schedule properly`() {
        // Given: 모든 필드 데이터
        val id = "schedule123"
        val projectId = "project456"
        val title = "전체 공휴일"
        val content = "추석 연휴"
        val startTime = LocalDateTime.of(2023, 9, 28, 0, 0)
        val endTime = LocalDateTime.of(2023, 10, 1, 23, 59)
        val participants = listOf("user1", "user2", "user3")
        val isAllDay = true
        
        // When: 모든 필드를 지정하여 Schedule 객체 생성
        val schedule = Schedule(
            id = id,
            projectId = projectId,
            title = title,
            content = content,
            startTime = startTime,
            endTime = endTime,
            participants = participants,
            isAllDay = isAllDay
        )
        
        // Then: 모든 필드가 주어진 값으로 초기화되어야 함
        assertEquals(id, schedule.id)
        assertEquals(projectId, schedule.projectId)
        assertEquals(title, schedule.title)
        assertEquals(content, schedule.content)
        assertEquals(startTime, schedule.startTime)
        assertEquals(endTime, schedule.endTime)
        assertEquals(participants, schedule.participants)
        assertTrue(schedule.isAllDay)
    }
    
    /**
     * 개인 일정 테스트 (projectId가 null)
     */
    @Test
    fun `personal schedule should have null projectId`() {
        // Given: projectId가 null인 개인 일정 데이터
        val id = "personal123"
        val title = "병원 예약"
        val content = "건강검진"
        val startTime = LocalDateTime.of(2023, 10, 20, 10, 0)
        val endTime = LocalDateTime.of(2023, 10, 20, 11, 0)
        
        // When: projectId를 null로 지정하여 Schedule 객체 생성
        val schedule = Schedule(
            id = id,
            projectId = null,
            title = title,
            content = content,
            startTime = startTime,
            endTime = endTime
        )
        
        // Then: projectId가 null이어야 함
        assertNull(schedule.projectId)
    }
    
    /**
     * equals 및 hashCode 테스트
     */
    @Test
    fun `equals and hashCode should work correctly`() {
        // Given: 동일한 데이터를 가진 두 Schedule 객체
        val startTime = LocalDateTime.of(2023, 10, 15, 14, 0)
        val endTime = LocalDateTime.of(2023, 10, 15, 15, 30)
        
        val schedule1 = Schedule(
            id = "schedule123",
            projectId = "project456",
            title = "디자인 회의",
            content = "UI 디자인 논의",
            startTime = startTime,
            endTime = endTime
        )
        
        val schedule2 = Schedule(
            id = "schedule123",
            projectId = "project456",
            title = "디자인 회의",
            content = "UI 디자인 논의",
            startTime = startTime,
            endTime = endTime
        )
        
        val differentSchedule = Schedule(
            id = "schedule456",
            projectId = "project456",
            title = "백엔드 회의",
            content = "API 설계 논의",
            startTime = startTime,
            endTime = endTime
        )
        
        // Then: 동일한 데이터를 가진 객체는 equals와 hashCode가 동일해야 함
        assertEquals(schedule1, schedule2)
        assertEquals(schedule1.hashCode(), schedule2.hashCode())
        
        // Then: 다른 데이터를 가진 객체는 equals가 false를 반환해야 함
        assertNotEquals(schedule1, differentSchedule)
    }
    
    /**
     * copy 메서드 테스트
     */
    @Test
    fun `copy should create new instance with specified changes`() {
        // Given: 원본 Schedule 객체
        val startTime = LocalDateTime.of(2023, 10, 15, 14, 0)
        val endTime = LocalDateTime.of(2023, 10, 15, 15, 30)
        
        val original = Schedule(
            id = "schedule123",
            projectId = "project456",
            title = "디자인 회의",
            content = "UI 디자인 논의",
            startTime = startTime,
            endTime = endTime,
            participants = listOf("user1", "user2")
        )
        
        // When: 일부 필드만 변경하여 복사
        val newEndTime = LocalDateTime.of(2023, 10, 15, 16, 0)
        val newParticipants = listOf("user1", "user2", "user3")
        
        val copied = original.copy(
            title = "확장된 디자인 회의",
            endTime = newEndTime,
            participants = newParticipants
        )
        
        // Then: 지정한 필드만 변경되고 나머지는 유지되어야 함
        assertEquals(original.id, copied.id)
        assertEquals(original.projectId, copied.projectId)
        assertEquals("확장된 디자인 회의", copied.title)
        assertEquals(original.content, copied.content)
        assertEquals(original.startTime, copied.startTime)
        assertEquals(newEndTime, copied.endTime)
        assertEquals(newParticipants, copied.participants)
        assertEquals(original.isAllDay, copied.isAllDay)
        
        // Then: 완전히 새로운 객체여야 함
        assertNotSame(original, copied)
    }
    
    /**
     * toString 메서드 테스트
     */
    @Test
    fun `toString should contain all fields`() {
        // Given: 모든 필드가 채워진 Schedule 객체
        val startTime = LocalDateTime.of(2023, 10, 15, 14, 0)
        val endTime = LocalDateTime.of(2023, 10, 15, 15, 30)
        val participants = listOf("user1", "user2")
        
        val schedule = Schedule(
            id = "schedule123",
            projectId = "project456",
            title = "디자인 회의",
            content = "UI 디자인 논의",
            startTime = startTime,
            endTime = endTime,
            participants = participants,
            isAllDay = false
        )
        
        // When: toString 호출
        val result = schedule.toString()
        
        // Then: 모든 필드의 이름과 값이 포함되어야 함
        assertTrue(result.contains("id=schedule123"))
        assertTrue(result.contains("projectId=project456"))
        assertTrue(result.contains("title=디자인 회의"))
        assertTrue(result.contains("content=UI 디자인 논의"))
        assertTrue(result.contains("startTime=$startTime"))
        assertTrue(result.contains("endTime=$endTime"))
        assertTrue(result.contains("participants=$participants"))
        assertTrue(result.contains("isAllDay=false"))
    }
} 