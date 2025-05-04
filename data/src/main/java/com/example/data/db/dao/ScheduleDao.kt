package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.data.model.local.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime // LocalDateTime 사용을 위해 임포트 (TypeConverter 필요)

/**
 * Room 데이터베이스의 'schedules' 테이블에 접근하기 위한 DAO(Data Access Object) 인터페이스입니다.
 */
@Dao
interface ScheduleDao {

    /**
     * 특정 날짜 범위에 해당하는 일정 목록을 Flow 형태로 가져옵니다.
     * 시작 시간이 해당 날짜 범위 내에 있는 일정을 반환합니다.
     * @param startOfDay 해당 날짜의 시작 시각 (TypeConverter 필요)
     * @param endOfDay 해당 날짜의 종료 시각 (TypeConverter 필요)
     * @return 일정 엔티티 리스트의 Flow.
     */
    @Query("SELECT * FROM schedules WHERE startTime >= :startOfDay AND startTime <= :endOfDay ORDER BY startTime ASC")
    fun getSchedulesForDateStream(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<ScheduleEntity>>

    /**
     * 특정 월 범위 내에서 일정이 있는 날짜 목록을 가져옵니다.
     * @param startOfMonth 해당 월의 시작 시각 (TypeConverter 필요)
     * @param endOfMonth 해당 월의 종료 시각 (TypeConverter 필요)
     * @return 일정이 있는 날짜(YYYY-MM-DD 형식의 문자열) 리스트.
     */
    @Query("SELECT DISTINCT date(startTime) FROM schedules WHERE startTime >= :startOfMonth AND startTime <= :endOfMonth")
    fun getDatesWithSchedulesBetween(startOfMonth: LocalDateTime, endOfMonth: LocalDateTime): List<String> // 반환 타입을 String 리스트로 변경

    /**
     * 특정 ID를 가진 일정 상세 정보를 가져옵니다.
     * @param scheduleId 가져올 일정의 ID.
     * @return 해당 ID의 일정 엔티티. 없으면 null.
     */
    @Query("SELECT * FROM schedules WHERE id = :scheduleId LIMIT 1")
    suspend fun getScheduleById(scheduleId: String): ScheduleEntity?

    /**
     * 새로운 일정 목록을 삽입합니다. 기존 데이터는 삭제됩니다.
     * @param schedules 저장할 일정 엔티티 리스트.
     */
    @Transaction
    suspend fun clearAndInsertSchedules(schedules: List<ScheduleEntity>) {
        clearAllSchedules()
        insertSchedules(schedules)
    }

    /**
     * 여러 일정을 한 번에 삽입합니다.
     * @param schedules 삽입할 일정 엔티티 리스트.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE) // 또는 REPLACE, 상황에 맞게 선택
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    /**
     * 단일 일정을 삽입하거나 이미 존재하면 업데이트합니다 (Upsert).
     * @param schedule 추가 또는 업데이트할 일정 엔티티.
     */
    @Upsert
    suspend fun upsertSchedule(schedule: ScheduleEntity)

    /**
     * 특정 ID의 일정을 삭제합니다.
     * @param scheduleId 삭제할 일정의 ID.
     * @return 삭제된 행의 수.
     */
    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteScheduleById(scheduleId: String): Int

    /**
     * 모든 일정을 삭제합니다.
     */
    @Query("DELETE FROM schedules")
    suspend fun clearAllSchedules()

} 