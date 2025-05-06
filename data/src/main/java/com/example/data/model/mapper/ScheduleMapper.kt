package com.example.data.model.mapper

import com.example.data.model.remote.schedule.ScheduleDto
import com.example.domain.model.Schedule
import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date // Timestamp.toDate() 사용 위해 필요

/**
 * ScheduleDto(데이터 레이어 Firestore 모델)와 Schedule(도메인 모델) 간의 변환을 담당하는 확장 함수들을 정의합니다.
 */

// 시간 변환 시 UTC 시간대 사용
private val zoneId = ZoneId.of("UTC")

/**
 * ScheduleDto 객체를 Schedule 도메인 모델 객체로 변환합니다.
 * Firestore에서 읽어온 데이터(DTO)를 앱 내부 로직에서 사용하는 모델로 변환할 때 사용됩니다.
 * startTime, endTime, id는 필수 값으로 간주하고 null일 경우 예외를 발생시킵니다.
 *
 * @receiver ScheduleDto 변환할 DTO 객체.
 * @return Schedule 변환된 도메인 모델 객체.
 * @throws IllegalArgumentException 필수 필드(id, startTime, endTime)가 DTO에서 null일 경우 발생.
 */
fun ScheduleDto.toDomain(): Schedule {
    val documentId = this.scheduleId
        ?: throw IllegalArgumentException("Schedule ID cannot be null in DTO")
    val startTimeStamp = this.startTime
        ?: throw IllegalArgumentException("startTime cannot be null in DTO for Schedule ID: $documentId")
    val endTimeStamp = this.endTime
        ?: throw IllegalArgumentException("endTime cannot be null in DTO for Schedule ID: $documentId")

    // Timestamp -> Instant -> LocalDateTime 변환
    val startDateTime = startTimeStamp.toDate().toInstant().atZone(zoneId).toLocalDateTime()
    val endDateTime = endTimeStamp.toDate().toInstant().atZone(zoneId).toLocalDateTime()

    return Schedule(
        id = documentId,
        projectId = this.projectId,
        title = this.title, // DTO의 기본값("") 사용
        content = this.description,
        startTime = startDateTime,
        endTime = endDateTime,
        participants = this.participantIds, // DTO의 participantIds를 domain의 participants로 매핑
        isAllDay = this.isAllDay ?: false // null이면 false로 (스키마에 없는 필드)
    )
}

/**
 * Schedule 도메인 모델 객체를 ScheduleDto 객체로 변환합니다.
 * 앱 내부 데이터를 Firestore에 쓰기 용이한 형태(DTO)로 변환할 때 사용됩니다.
 *
 * @receiver Schedule 변환할 도메인 모델 객체.
 * @return ScheduleDto 변환된 DTO 객체. 도메인 모델의 id는 DTO의 id 필드에 포함되지 않습니다 (Firestore 자동 생성 또는 별도 지정).
 */
fun Schedule.toDto(): ScheduleDto {
    // LocalDateTime -> Instant -> Timestamp 변환
    val startInstant = this.startTime.atZone(zoneId).toInstant()
    val endInstant = this.endTime.atZone(zoneId).toInstant()
    // Timestamp 생성자에 Instant 초와 나노초 전달
    val startTimestamp = Timestamp(startInstant.epochSecond, startInstant.nano)
    val endTimestamp = Timestamp(endInstant.epochSecond, endInstant.nano)

    return ScheduleDto(
        // id는 Firestore에서 자동 생성되거나 외부에서 설정하므로 여기서는 null로 둠
        projectId = this.projectId,
        title = this.title,
        description = this.content ?: "",
        startTime = startTimestamp,
        endTime = endTimestamp,
        participantIds = this.participants,
        isAllDay = this.isAllDay
        // createdAt은 DTO에서 @ServerTimestamp로 처리 (필요 시)
    )
}

/**
 * ScheduleDto 리스트를 Schedule 도메인 모델 리스트로 변환합니다.
 * 변환 중 오류가 발생하는 요소는 결과 리스트에서 제외됩니다.
 *
 * @receiver List<ScheduleDto> 변환할 DTO 리스트.
 * @return List<Schedule> 변환된 도메인 모델 리스트.
 */
fun List<ScheduleDto>.toDomain(): List<Schedule> {
    // 개별 DTO 변환 실패 시 null을 반환하고 최종적으로 null이 아닌 결과만 필터링
    return this.mapNotNull { dto ->
        try {
            dto.toDomain()
        } catch (e: Exception) {
            // TODO: 로깅 추가 고려 (어떤 DTO 변환에 실패했는지 기록)
            // Log.e("ScheduleMapper", "Failed to convert ScheduleDto (id: ${dto.id}) to domain: ${e.message}")
            null // 변환 실패 시 null 반환하여 mapNotNull에서 걸러지도록 함
        }
    }
}

/**
 * Schedule 도메인 모델 리스트를 ScheduleDto 리스트로 변환합니다.
 *
 * @receiver List<Schedule> 변환할 도메인 모델 리스트.
 * @return List<ScheduleDto> 변환된 DTO 리스트.
 */
fun List<Schedule>.toDto(): List<ScheduleDto> {
    return this.map { it.toDto() }
} 