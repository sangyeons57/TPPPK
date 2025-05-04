package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Room 데이터베이스의 'schedules' 테이블과 매핑되는 엔티티 클래스입니다.
 *
 * @property id 일정의 고유 ID (기본 키).
 * @property projectId 일정이 속한 프로젝트의 ID. 개인 일정일 경우 null.
 * @property title 일정 제목.
 * @property content 일정 내용 (선택 사항).
 * @property startTime 일정 시작 시간 (UTC 기준). TypeConverter 필요.
 * @property endTime 일정 종료 시간 (UTC 기준). TypeConverter 필요.
 * @property participants 참여자 ID 리스트. TypeConverter 필요.
 * @property isAllDay 하루 종일 일정 여부.
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val title: String,
    val content: String?,
    val startTime: LocalDateTime, // Room은 LocalDateTime 직접 지원 안 함 -> TypeConverter 필요
    val endTime: LocalDateTime,   // Room은 LocalDateTime 직접 지원 안 함 -> TypeConverter 필요
    val participants: List<String>, // Room은 List<String> 직접 지원 안 함 -> TypeConverter 필요
    val isAllDay: Boolean
) 