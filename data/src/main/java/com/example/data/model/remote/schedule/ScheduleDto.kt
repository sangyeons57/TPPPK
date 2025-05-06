package com.example.data.model.remote.schedule

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * 일정 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'schedules' 컬렉션과 매핑됩니다.
 */
data class ScheduleDto(
    @DocumentId
    val scheduleId: String = "",
    
    @PropertyName("title")
    val title: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("startTime")
    val startTime: Timestamp = Timestamp.now(),
    
    @PropertyName("endTime")
    val endTime: Timestamp = Timestamp.now(),
    
    @PropertyName("location")
    val location: String? = null,
    
    @PropertyName("projectId")
    val projectId: String? = null,
    
    @PropertyName("creatorId")
    val creatorId: String = "",
    
    @PropertyName("participantIds")
    val participantIds: List<String> = emptyList(),
    
    @PropertyName("isAllDay")
    val isAllDay: Boolean = false,
    
    @PropertyName("priority")
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    
    @PropertyName("status")
    val status: String = "SCHEDULED", // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    
    @PropertyName("reminderTime")
    val reminderTime: Timestamp? = null,
    
    @PropertyName("color")
    val color: String? = null,
    
    @PropertyName("tags")
    val tags: List<String> = emptyList(),
    
    @PropertyName("recurrenceRule")
    val recurrenceRule: String? = null, // iCalendar RRULE 형식의 반복 규칙
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now()
) 