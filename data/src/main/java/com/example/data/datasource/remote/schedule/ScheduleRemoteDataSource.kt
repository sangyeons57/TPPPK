package com.example.data.datasource.remote.schedule

import com.example.data.model.remote.schedule.ScheduleDto // DTO 위치 가정
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth

/**
 * Firestore 'schedules' 컬렉션과 직접 상호작용하는 데이터 소스 인터페이스입니다.
 * 이 인터페이스의 구현체는 Firestore SDK를 사용하여 실제 작업을 수행합니다.
 */
interface ScheduleRemoteDataSource {

    /**
     * 특정 날짜 범위에 해당하는 Firestore 일정 문서 목록을 비동기적으로 가져옵니다.
     * Firestore 쿼리 예외 발생 시 호출자에게 전파됩니다.
     *
     * @param startOfDay 해당 날짜의 시작 시각 Timestamp.
     * @param endOfDay 해당 날짜의 종료 시각 Timestamp.
     * @return 성공 시 Firestore 문서로부터 매핑된 ScheduleDto 리스트.
     * @throws FirebaseFirestoreException Firestore 쿼리 중 오류 발생 시.
     */
    suspend fun getSchedulesForDate(startOfDay: Timestamp, endOfDay: Timestamp): List<ScheduleDto>

    /**
     * 특정 월 범위에 해당하는 Firestore 일정 문서 목록을 비동기적으로 가져옵니다.
     * Firestore 쿼리 예외 발생 시 호출자에게 전파됩니다.
     *
     * @param startOfMonth 해당 월의 시작 시각 Timestamp.
     * @param endOfMonth 해당 월의 종료 시각 Timestamp.
     * @return 성공 시 Firestore 문서로부터 매핑된 ScheduleDto 리스트.
     * @throws FirebaseFirestoreException Firestore 쿼리 중 오류 발생 시.
     */
    suspend fun getSchedulesForMonth(startOfMonth: Timestamp, endOfMonth: Timestamp): List<ScheduleDto>

    /**
     * 특정 ID를 가진 Firestore 일정 문서를 비동기적으로 가져옵니다.
     * Firestore 쿼리 예외 발생 시 호출자에게 전파됩니다.
     *
     * @param scheduleId 가져올 문서의 ID.
     * @return 성공 시 Firestore 문서로부터 매핑된 ScheduleDto.
     * @throws FirebaseFirestoreException Firestore 쿼리 중 오류 발생 시 또는 문서를 찾을 수 없을 때 (구현에 따라 다름).
     */
    suspend fun getScheduleDetail(scheduleId: String): ScheduleDto

    /**
     * 새로운 일정 데이터를 Firestore에 비동기적으로 추가합니다.
     * Firestore 작업 예외 발생 시 호출자에게 전파됩니다.
     *
     * @param scheduleDto Firestore에 추가할 데이터를 담은 ScheduleDto 객체 (ID는 null 또는 무시됨).
     * @return 성공 시 Firestore가 생성한 새로운 문서의 ID.
     * @throws FirebaseFirestoreException Firestore 쓰기 작업 중 오류 발생 시.
     */
    suspend fun addSchedule(scheduleDto: ScheduleDto): String // 성공 시 생성된 ID 반환

    /**
     * 특정 ID를 가진 Firestore 일정 문서를 비동기적으로 삭제합니다.
     * Firestore 작업 예외 발생 시 호출자에게 전파됩니다.
     *
     * @param scheduleId 삭제할 문서의 ID.
     * @throws FirebaseFirestoreException Firestore 삭제 작업 중 오류 발생 시.
     */
    suspend fun deleteSchedule(scheduleId: String) // 성공 시 반환값 없음, 실패 시 예외

    /**
     * 특정 ID를 가진 Firestore 일정 문서를 비동기적으로 업데이트합니다.
     * Firestore 작업 예외 발생 시 호출자에게 전파됩니다.
     *
     * @param scheduleId 업데이트할 문서의 ID.
     * @param scheduleDto 업데이트할 데이터가 담긴 ScheduleDto 객체.
     * @throws FirebaseFirestoreException Firestore 업데이트 작업 중 오류 발생 시.
     */
    suspend fun updateSchedule(scheduleId: String, scheduleDto: ScheduleDto) // 성공 시 반환값 없음, 실패 시 예외
} 