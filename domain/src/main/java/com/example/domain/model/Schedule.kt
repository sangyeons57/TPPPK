// 경로: domain/model/Schedule.kt (CalendarViewModel, ScheduleDetailViewModel 등 기반)
package com.example.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일정 정보를 나타내는 데이터 클래스입니다.
 * Firestore의 'schedules' 컬렉션 문서와 매핑됩니다.
 *
 * @property id 일정의 고유 Firestore 문서 ID.
 * @property projectId 일정이 속한 프로젝트의 ID. 개인 일정일 경우 null입니다. Firestore 'projectId' 필드에 해당합니다.
 * @property title 일정 제목. Firestore 'title' 필드에 해당합니다.
 * @property content 일정에 대한 추가 내용 또는 설명 (선택 사항). Firestore 스키마에는 현재 정의되지 않았습니다.
 * @property startTime 일정 시작 시간. Firestore 'startTime' Timestamp 필드에 해당합니다.
 * @property endTime 일정 종료 시간. Firestore 'endTime' Timestamp 필드에 해당합니다.
 * @property participants 일정 참여자의 사용자 ID 목록. Firestore 'participants' 필드(배열)에 해당합니다.
 * @property isAllDay 하루 종일 지속되는 일정인지 여부를 나타냅니다. Firestore 스키마에는 현재 정의되지 않았습니다.
 */
data class Schedule(
    val id: String,
    val projectId: String?,
    val title: String,
    val content: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val participants: List<String> = emptyList(), // attendees -> participants
    val isAllDay: Boolean = false
)