package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.schedule.ScheduleDto
import com.example.domain.model.Schedule
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import javax.inject.Inject
import android.util.Log
import com.example.domain.model.ScheduleStatus

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
        Log.d("ScheduleMapper", "mapToDomain(dto) 호출됨. DTO: $dto")
        val domainModel = dto.toDomainModelWithTime(dateTimeUtil)
        Log.d("ScheduleMapper", "mapToDomain(dto) - 변환된 도메인 모델: $domainModel")
        return domainModel
    }

    /**
     * Schedule 도메인 모델을 ScheduleDto로 변환합니다.
     */
    fun mapToDto(domain: Schedule): ScheduleDto {
        Log.d("ScheduleMapper", "mapToDto(domain) 호출됨. 도메인 모델: $domain")
        val dto = domain.toDtoWithTime(dateTimeUtil)
        Log.d("ScheduleMapper", "mapToDto(domain) - 변환된 DTO: $dto")
        return dto
    }

    /**
     * ScheduleDto 리스트를 Schedule 도메인 모델 리스트로 변환합니다.
     * 변환 중 오류가 발생하는 요소는 결과 리스트에서 제외됩니다 (null 반환 후 필터링).
     */
    fun mapToDomainList(dtoList: List<ScheduleDto>): List<Schedule> {
        Log.d("ScheduleMapper", "mapToDomainList 호출됨. DTO 리스트 크기: ${dtoList.size}")
        val domainList = dtoList.mapNotNull { dto ->
            try {
                // 개별 DTO 로깅은 양이 많을 수 있으므로 mapToDomain(dto) 내부 로그에 의존
                mapToDomain(dto)
            } catch (e: Exception) {
                Log.e("ScheduleMapper", "mapToDomainList - DTO (id: ${dto.scheduleId}) 변환 실패: ${e.message}", e)
                null
            }
        }
        Log.d("ScheduleMapper", "mapToDomainList - 변환된 도메인 리스트 크기: ${domainList.size}")
        return domainList
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
    Log.d("ScheduleMapperExt", "ScheduleDto.toDomainModelWithTime 호출됨. 입력 DTO: $this")
    val basicDomain = this.toBasicDomainModel() // Handles basic field mapping
    Log.d("ScheduleMapperExt", "ScheduleDto.toDomainModelWithTime - toBasicDomainModel 결과: $basicDomain")
    val finalDomain = basicDomain.copy(
        startTime = this.startTime?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        endTime = this.endTime?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        status = this.status ?: ScheduleStatus.SCHEDULED,
        createdAt = this.createdAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH, // Default if somehow null from DTO
        updatedAt = this.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) }
    )
    Log.d("ScheduleMapperExt", "ScheduleDto.toDomainModelWithTime - 변환된 도메인 모델: $finalDomain")
    return finalDomain
}

/**
 * Schedule 도메인 모델을 ScheduleDto로 변환합니다.
 * DateTimeUtil과 Enum.name을 사용합니다.
 */
fun Schedule.toDtoWithTime(dateTimeUtil: DateTimeUtil): ScheduleDto {
    Log.d("ScheduleMapperExt", "Schedule.toDtoWithTime 호출됨. 입력 도메인: $this")
    val basicDto = ScheduleDto.fromBasicDomainModel(this) // Handles basic field mapping
    Log.d("ScheduleMapperExt", "Schedule.toDtoWithTime - fromBasicDomainModel 결과: $basicDto")
    val finalDto = basicDto.copy(
        startTime = dateTimeUtil.instantToFirebaseTimestamp(this.startTime),
        endTime = dateTimeUtil.instantToFirebaseTimestamp(this.endTime),
        status = this.status,     // Enum to String
        createdAt = dateTimeUtil.instantToFirebaseTimestamp(this.createdAt), // Domain model's createdAt is non-null
        updatedAt = this.updatedAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }
    )
    Log.d("ScheduleMapperExt", "Schedule.toDtoWithTime - 변환된 DTO: $finalDto")
    return finalDto
} 