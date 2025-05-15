// 경로: domain/model/Schedule.kt (CalendarViewModel, ScheduleDetailViewModel 등 기반)
package com.example.domain.model

import java.time.Instant

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
 * @property creatorId 일정 생성자의 ID. Firestore 스키마에는 현재 정의되지 않았습니다.
 * @property status 일정 상태. Firestore 스키마에는 현재 정의되지 않았습니다.
 * @property color 일정 표시 색상. Firestore 스키마에는 현재 정의되지 않았습니다.
 * @property createdAt 일정 생성 시간. Firestore 스키마에는 현재 정의되지 않았습니다.
 * @property updatedAt 일정 업데이트 시간. Firestore 스키마에는 현재 정의되지 않았습니다.
 */
data class Schedule(
    val id: String,
    val title: String,
    val content: String?,
    val startTime: Instant,
    val endTime: Instant,
    val projectId: String? = null,
    val creatorId: String,
    val status: ScheduleStatus = ScheduleStatus.SCHEDULED,
    val color: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant? = null
) {
}

/**
 * 24시간 캘린더 뷰에 표시할 일정 아이템 데이터 클래스입니다.
 * 
 * @property id 일정의 고유 ID
 * @property title 일정 제목
 * @property startTime 일정 시작 시간 (Instant)
 * @property endTime 일정 종료 시간 (Instant)
 * @property color 일정 표시 색상 (ULong ARGB)
 * @property startColorAlpha 시작 시간 색상 알파값
 * @property endColorAlpha 종료 시간 색상 알파값
 */
data class ScheduleItem24Hour(
    val id: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val color: ULong,
    val startColorAlpha: Float = 1.0f,
    val endColorAlpha: Float = 1.0f
)