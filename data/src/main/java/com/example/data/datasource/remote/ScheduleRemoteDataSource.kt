
package com.example.data.datasource.remote

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.ScheduleDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ScheduleRemoteDataSource: DefaultDatasource {

    /**
     * Firestore에서 지정된 년도와 월에 해당하는 모든 스케줄 DTO를 가져옵니다.
     *
     * @param yearMonth 가져올 스케줄의 년월 정보
     * @return 해당 월의 스케줄 DTO 목록을 담은 Flow
     */
    suspend fun findByMonth(userId: String, yearMonth: YearMonth): Flow<CustomResult<List<ScheduleDTO>, Exception>>

    /**
     * Firestore에서 지정된 날짜에 시작하는 모든 스케줄 DTO를 가져옵니다.
     *
     * @param date 스케줄을 가져올 특정 날짜
     * @return 해당 날짜의 스케줄 DTO 목록을 담은 Flow
     */
    suspend fun findByDate(userId: String, date: LocalDate): Flow<CustomResult<List<ScheduleDTO>, Exception>>

    /**
     * Firestore에서 지정된 사용자의 특정 연월에 일정이 있는 날짜들의 요약 정보를 가져옵니다.
     *
     * @param userId 사용자 ID
     * @param yearMonth 요약 정보를 가져올 연월
     * @return 해당 월에 일정이 있는 날짜들의 Set. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    suspend fun findDateSummaryForMonth(userId: String, yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception>

}

@Singleton
class ScheduleRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<ScheduleDTO>(firestore, ScheduleDTO::class.java),
    ScheduleRemoteDataSource {

    override suspend fun findByMonth(userId : String, yearMonth: YearMonth): Flow<CustomResult<List<ScheduleDTO>, Exception>> = callbackFlow {

        val startOfMonthTimestamp = DateTimeUtil.yearMonthToStartOfMonthTimestamp(yearMonth)
        val endOfMonthTimestamp = DateTimeUtil.yearMonthToEndOfMonthExclusiveTimestamp(yearMonth)

        val query = firestore.collection(ScheduleDTO.COLLECTION_NAME) // agés FirestoreConstants.Collections.SCHEDULES 사용
            .whereEqualTo(ScheduleDTO.CREATOR_ID, userId) // agés FirestoreConstants.Schedule.CREATOR_ID 사용
            .whereGreaterThanOrEqualTo(ScheduleDTO.START_TIME, startOfMonthTimestamp) // agés FirestoreConstants.Schedule.START_TIME 사용
            .whereLessThan(ScheduleDTO.START_TIME, endOfMonthTimestamp)
            .orderBy(ScheduleDTO.START_TIME, Query.Direction.ASCENDING) // agés FirestoreConstants.Schedule.START_TIME 사용

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(Exception("$error")))
                close() // 오류 발생 시 Flow 종료
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val schedules = snapshots.toObjects(ScheduleDTO::class.java)
                trySend(CustomResult.Success(schedules))
            } else {
                // 스냅샷이 null인 경우는 Firestore 문서가 없을 때도 발생 가능 (Listener 초기 호출 시)
                trySend(CustomResult.Success(emptyList()))
            }
        }
        // Flow가 취소될 때 Firestore 리스너를 제거합니다.
        awaitClose { listenerRegistration.remove() }
    }


    override suspend fun findByDate(userId: String, date: LocalDate): Flow<CustomResult<List<ScheduleDTO>, Exception>> = callbackFlow {

        // DateTimeUtil을 사용하여 해당 날짜의 시작과 끝 Timestamp를 가져옵니다. (UTC 기준)
        val startOfDayTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.localDateToStartOfDayInstant(date))
        val endOfDayExclusiveTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.localDateToEndOfDayInstant(date))

        val query = firestore.collection(ScheduleDTO.COLLECTION_NAME)
            .whereEqualTo(ScheduleDTO.CREATOR_ID, userId)
            .whereGreaterThanOrEqualTo(ScheduleDTO.START_TIME, startOfDayTimestamp)
            .whereLessThan(ScheduleDTO.START_TIME, endOfDayExclusiveTimestamp)
            .orderBy(ScheduleDTO.START_TIME, Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (isClosedForSend) { // 채널이 닫혔는지 확인
                return@addSnapshotListener
            }
            if (error != null) {
                trySend(CustomResult.Failure(error))
                // 필요하다면 여기서 close(error) 호출하여 Flow를 종료
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val schedules = snapshots.toObjects(ScheduleDTO::class.java) // 여기에서 오류가 발생했었음
                Log.d("ScheduleRemoteDataSourceImpl", "schedules: $schedules")
                trySend(CustomResult.Success(schedules))
            } else {
                trySend(CustomResult.Success(emptyList()))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun findDateSummaryForMonth(
        userId: String,
        yearMonth: YearMonth
    ): CustomResult<Set<LocalDate>, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val startOfMonthTimestamp = DateTimeUtil.yearMonthToStartOfMonthTimestamp(yearMonth)
            val endOfMonthExclusiveTimestamp = DateTimeUtil.yearMonthToEndOfMonthExclusiveTimestamp(yearMonth)

            val querySnapshot = firestore.collection(ScheduleDTO.COLLECTION_NAME)
                .whereEqualTo(ScheduleDTO.CREATOR_ID, userId)
                .whereGreaterThanOrEqualTo(ScheduleDTO.START_TIME, startOfMonthTimestamp)
                .whereLessThan(ScheduleDTO.START_TIME, endOfMonthExclusiveTimestamp)
                .get()
                .await()

            val datesWithSchedules = querySnapshot.documents
                .mapNotNull { document -> document.toObject(ScheduleDTO::class.java)?.startTime }
                .mapNotNull { timestamp -> DateTimeUtil.toLocalDateTime(timestamp).toLocalDate() }
                .toSet()

            datesWithSchedules
        }
    }

}

