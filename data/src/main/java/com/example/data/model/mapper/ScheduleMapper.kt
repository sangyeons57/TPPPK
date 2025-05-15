package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.schedule.ScheduleDto
import com.example.domain.model.Schedule
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import javax.inject.Inject

/**
 * ScheduleDto와 Schedule 도메인 모델 간의 변환을 담당하는 매퍼 클래스.
 * DateTimeUtil을 주입받아 시간 관련 필드를 처리하고,
 * SchedulePriority 및 ScheduleStatus Enum 변환을 담당합니다.
 */
class ScheduleMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {

    /**
     * Firestore DocumentSnapshot을 Schedule 도메인 모델로 변환합니다.
     */
    fun mapToDomain(document: DocumentSnapshot): Schedule? {
        return try {
            val dto = document.toObject(ScheduleDto::class.java)
            dto?.toDomainModelWithTime(dateTimeUtil)
        } catch (e: Exception) {
            // Log error: e.g., Log.e("ScheduleMapper", "Error mapping snapshot to Schedule: ${e.message}")
            null
        }
    }

    /**
     * ScheduleDto를 Schedule 도메인 모델로 변환합니다.
     */
    fun mapToDomain(dto: ScheduleDto): Schedule {
        return dto.toDomainModelWithTime(dateTimeUtil)
    }

    /**
     * Schedule 도메인 모델을 ScheduleDto로 변환합니다.
     */
    fun mapToDto(domain: Schedule): ScheduleDto {
        return domain.toDtoWithTime(dateTimeUtil)
    }

    /**
     * ScheduleDto 리스트를 Schedule 도메인 모델 리스트로 변환합니다.
     * 변환 중 오류가 발생하는 요소는 결과 리스트에서 제외됩니다 (null 반환 후 필터링).
     */
    fun mapToDomainList(dtoList: List<ScheduleDto>): List<Schedule> {
        return dtoList.mapNotNull { dto ->
            try {
                mapToDomain(dto)
            } catch (e: Exception) {
                // Log error: e.g., Log.e("ScheduleMapper", "Failed to convert ScheduleDto (id: ${dto.scheduleId}) to domain: ${e.message}")
                null
            }
        }
    }

    /**
     * Schedule 도메인 모델 리스트를 ScheduleDto 리스트로 변환합니다.
     */
    fun mapToDtoList(domainList: List<Schedule>): List<ScheduleDto> {
        return domainList.map { mapToDto(it) }
    }
}

/**
 * ScheduleDto를 Schedule 도메인 모델로 변환합니다.
 * DateTimeUtil과 Enum.fromString을 사용합니다.
 */
fun ScheduleDto.toDomainModelWithTime(dateTimeUtil: DateTimeUtil): Schedule {
    val basicDomain = this.toBasicDomainModel() // Handles basic field mapping
    return basicDomain.copy(
        startTime = this.startTime?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        endTime = this.endTime?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        status = this.status,
        createdAt = this.createdAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH, // Default if somehow null from DTO
        updatedAt = this.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) }
    )
}

/**
 * Schedule 도메인 모델을 ScheduleDto로 변환합니다.
 * DateTimeUtil과 Enum.name을 사용합니다.
 */
fun Schedule.toDtoWithTime(dateTimeUtil: DateTimeUtil): ScheduleDto {
    val basicDto = ScheduleDto.fromBasicDomainModel(this) // Handles basic field mapping
    return basicDto.copy(
        startTime = dateTimeUtil.instantToFirebaseTimestamp(this.startTime),
        endTime = dateTimeUtil.instantToFirebaseTimestamp(this.endTime),
        status = this.status,     // Enum to String
        createdAt = dateTimeUtil.instantToFirebaseTimestamp(this.createdAt), // Domain model's createdAt is non-null
        updatedAt = this.updatedAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }
    )
} 