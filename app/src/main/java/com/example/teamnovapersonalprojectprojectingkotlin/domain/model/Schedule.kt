// 경로: domain/model/Schedule.kt (CalendarViewModel, ScheduleDetailViewModel 등 기반)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

import java.time.LocalDateTime

data class Schedule(
    val id: String,
    val projectId: String?, // 어떤 프로젝트의 일정인지 (null이면 개인 일정)
    val title: String,
    val content: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val attendees: List<String> = emptyList(), // 참석자 userId 목록 (선택적)
    val isAllDay: Boolean = false
)