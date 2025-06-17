
package com.example.data.datasource.remote

import android.util.Log
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.ScheduleDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ScheduleRemoteDataSource {

    companion object {
        private const val SCHEDULES_COLLECTION = "schedules"
    }

    private val schedulesCollection = firestore.collection(SCHEDULES_COLLECTION)

    override suspend fun findById(scheduleId: String): CustomResult<ScheduleDTO, Exception> {
        if (scheduleId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Schedule ID cannot be empty."))
        }
        val document = schedulesCollection.document(scheduleId).get().await()
        return CustomResult.Success(document.toObject(ScheduleDTO::class.java) as ScheduleDTO)
    }

    private suspend fun createSchedule(schedule: ScheduleDTO): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            // 생성자 ID와 생성 시간을 주입 (DTO에 ServerTimestamp가 있으므로 Firestore에서 자동 설정됨)
            val newScheduleWithCreator = schedule.copy(creatorId = uid)
            val docRef = schedulesCollection.add(newScheduleWithCreator).await()
            docRef.id
        }
    }

    private suspend fun updateSchedule(schedule: ScheduleDTO): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            if (schedule.id.isBlank()) {
                throw IllegalArgumentException("Schedule ID cannot be empty for an update.")
            }
            // 업데이트할 필드만 Map으로 만들어 전달하여 createdAt 같은 불변 필드를 보호하고
            // updatedAt 타임스탬프를 확실하게 찍습니다.
            val updateData = mutableMapOf<String, Any?>()
            updateData["title"] = schedule.title
            updateData["content"] = schedule.content
            updateData["startTime"] = schedule.startTime
            updateData["endTime"] = schedule.endTime
            updateData["projectId"] = schedule.projectId // projectId도 업데이트 가능하도록 유지 (선택사항)
            updateData["status"] = schedule.status
            updateData["color"] = schedule.color
            updateData["updatedAt"] = FieldValue.serverTimestamp()
            
            schedulesCollection.document(schedule.id).update(updateData).await()
            schedule.id
        }
    }

    override suspend fun saveSchedule(schedule: ScheduleDTO): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        if (schedule.id.isNotBlank()) {
            return@withContext updateSchedule(schedule)
        }
        return@withContext createSchedule(schedule)
    }

    override suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            schedulesCollection.document(scheduleId).delete().await()
            Unit
        }
    }

    override suspend fun findByMonth(userId : String, yearMonth: YearMonth): Flow<CustomResult<List<ScheduleDTO>, Exception>> = callbackFlow {

        val startOfMonthTimestamp = DateTimeUtil.yearMonthToStartOfMonthTimestamp(yearMonth)
        val endOfMonthTimestamp = DateTimeUtil.yearMonthToEndOfMonthExclusiveTimestamp(yearMonth)

        val query = firestore.collection(FirestoreConstants.Collections.SCHEDULES) // agés FirestoreConstants.Collections.SCHEDULES 사용
            .whereEqualTo(FirestoreConstants.Schedule.CREATOR_ID, userId) // agés FirestoreConstants.Schedule.CREATOR_ID 사용
            .whereGreaterThanOrEqualTo(FirestoreConstants.Schedule.START_TIME, startOfMonthTimestamp) // agés FirestoreConstants.Schedule.START_TIME 사용
            .whereLessThan(FirestoreConstants.Schedule.START_TIME, endOfMonthTimestamp)
            .orderBy(FirestoreConstants.Schedule.START_TIME, Query.Direction.ASCENDING) // agés FirestoreConstants.Schedule.START_TIME 사용

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

        val query = firestore.collection(FirestoreConstants.Collections.SCHEDULES)
            .whereEqualTo(FirestoreConstants.Schedule.CREATOR_ID, userId)
            .whereGreaterThanOrEqualTo(FirestoreConstants.Schedule.START_TIME, startOfDayTimestamp)
            .whereLessThan(FirestoreConstants.Schedule.START_TIME, endOfDayExclusiveTimestamp)
            .orderBy(FirestoreConstants.Schedule.START_TIME, Query.Direction.ASCENDING)
        
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

            val querySnapshot = firestore.collection(FirestoreConstants.Collections.SCHEDULES)
                .whereEqualTo(FirestoreConstants.Schedule.CREATOR_ID, userId)
                .whereGreaterThanOrEqualTo(FirestoreConstants.Schedule.START_TIME, startOfMonthTimestamp)
                .whereLessThan(FirestoreConstants.Schedule.START_TIME, endOfMonthExclusiveTimestamp)
                .get()
                .await()

            val datesWithSchedules = querySnapshot.documents
                .mapNotNull { document -> document.toObject(ScheduleDTO::class.java)?.startTime }
                .mapNotNull { timestamp -> DateTimeUtil.firebaseTimestampToLocalDateTime(timestamp).toLocalDate() }
                .toSet()

            datesWithSchedules
        }
    }

    override fun observeSchedule(scheduleId: String): Flow<CustomResult<ScheduleDTO, Exception>> = callbackFlow{
        val listenerRegistration = schedulesCollection.document(scheduleId)
            .addSnapshotListener { snapshot, error ->
                if(error != null){
                    trySend(CustomResult.Failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val schedule = snapshot.toObject(ScheduleDTO::class.java)
                    if(schedule != null){
                        trySend(CustomResult.Success(schedule))
                    } else {
                        trySend(CustomResult.Failure(Exception("Failed to parse schedule data.")))
                    }
                } else {
                    trySend(CustomResult.Failure(Exception("Schedule document does not exist.")))
                }
            }
        awaitClose{ listenerRegistration.remove() }
    }
}

