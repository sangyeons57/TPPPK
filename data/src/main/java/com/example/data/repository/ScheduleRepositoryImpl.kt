package com.example.data.repository

// import removed: FirestoreConstants not used anymore
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.domain.model.base.Schedule
import com.example.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeParseException


/**
 * 일정 관리를 위한 저장소 구현체
 * Firebase의 자체 캐싱 시스템을 활용하여 데이터를 관리합니다.
 */
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
) : ScheduleRepository {

    /**
     * 새로운 일정을 생성합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param schedule 생성할 일정 정보
     * @return 생성된 일정 정보 (서버에서 부여된 ID 포함)
     */
    override suspend fun save(schedule: Schedule): CustomResult<String, Exception> {
        return try {
            val scheduleDto = schedule.toDto() // ID는 비어있을 수 있음
            val result = scheduleRemoteDataSource.saveSchedule(scheduleDto)
            when (result) {
                is CustomResult.Success -> {
                    CustomResult.Success(result.data)
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown error"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 특정 일정의 상세 정보를 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param scheduleId 일정 ID
     * @return 일정 상세 정보
     */
    override suspend fun findById(scheduleId: String): CustomResult<Schedule, Exception> {
        return try {
            val result = scheduleRemoteDataSource.findById(scheduleId)
            when (result) {
                is CustomResult.Success -> {
                    try {
                        val domainSchedule = result.data.toDomain()
                        CustomResult.Success(domainSchedule)
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error ?: Exception("Failed to get schedule details"))
                else -> CustomResult.Failure(Exception("Unknown error"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 일정을 삭제합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param scheduleId 삭제할 일정 ID
     * @param currentUserId 현재 사용자 ID (권한 확인용)
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun delete(scheduleId: String): CustomResult<Unit, Exception> {
        return try {
            val result = scheduleRemoteDataSource.deleteSchedule(scheduleId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown error"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun findByDateSummaryForMonth(
        userId: String,
        yearMonth: YearMonth
    ): CustomResult<Set<LocalDate>, Exception> {
        return scheduleRemoteDataSource.findDateSummaryForMonth(userId, yearMonth)
    }

    override suspend fun findByMonth(userId: String, yearMonth: YearMonth): Flow<CustomResult<List<Schedule>, Exception>> {
        return scheduleRemoteDataSource.findByMonth(userId, yearMonth).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    try {
                        // ScheduleDTO 리스트를 Schedule 도메인 모델 리스트로 변환
                        val domainSchedules = result.data.map { dto -> dto.toDomain() }
                        CustomResult.Success(domainSchedules)
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> CustomResult.Failure(Exception("Unknown error"))
            }
        }
    }

    override fun observe(scheduleId: String): Flow<CustomResult<Schedule, Exception>> {
        return scheduleRemoteDataSource.observeSchedule(scheduleId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    try {
                        CustomResult.Success(result.data.toDomain())
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown error"))
            }
        }
    }

    override fun findByDate(date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>> {
        val currentUser = firebaseAuth.currentUser
            ?: return flowOf(CustomResult.Failure(Exception("User not authenticated")))
        val userId = currentUser.uid

        return flow {
            val sourceFlow = scheduleRemoteDataSource.findByDate(userId, date)
            emitAll(sourceFlow.map {
                when (it) {
                    is CustomResult.Success -> {
                        try {
                            val domainSchedules = it.data.map { dto -> dto.toDomain() }
                            CustomResult.Success(domainSchedules)
                        } catch (e: Exception) {
                            CustomResult.Failure(e)
                        }
                    }
                    is CustomResult.Failure -> CustomResult.Failure(it.error)
                    else -> CustomResult.Failure(Exception("Unknown error"))
                    // If CustomResult has other states like Loading, they would be implicitly passed
                    // or could be explicitly handled if needed.
                }
            }.catch { e -> // Catch exceptions from sourceFlow or map
                emit(CustomResult.Failure(e as Exception))
            })
        }
    }

    override fun findByDate(date: String): Flow<CustomResult<List<Schedule>, Exception>> {
        return try {
            val localDate = LocalDate.parse(date) // Assumes ISO_LOCAL_DATE format e.g. "2023-10-26"
            findByDate(localDate) // Delegate to the LocalDate version
        } catch (e: DateTimeParseException) {
            flowOf(CustomResult.Failure(IllegalArgumentException("Invalid date format: '$date'. Expected yyyy-MM-dd.", e)))
        } catch (e: Exception) {
            flowOf(CustomResult.Failure(e))
        }
    }
}
